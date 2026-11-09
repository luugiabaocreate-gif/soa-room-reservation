[CmdletBinding()]
param(
    [string]$GatewayUrl = "http://localhost:8088",

    [string]$Username = "student01",

    [string]$Password = "DemoPassword123!",

    [ValidateRange(20, 1000)]
    [int]$RequestCount = 20,

    [ValidateRange(1, 100)]
    [int]$MinimumDistinctInstances = 2,

    [ValidateRange(0, 10000)]
    [int]$DelayMilliseconds = 100,

    [string]$OutputDirectory = (Join-Path $PSScriptRoot "evidence")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$GatewayUrl = $GatewayUrl.TrimEnd("/")
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

Write-Host "Logging in through API Gateway: $GatewayUrl" -ForegroundColor Cyan
$loginPayload = @{
    username = $Username
    password = $Password
} | ConvertTo-Json -Compress

$loginResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$GatewayUrl/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body $loginPayload

if ([string]::IsNullOrWhiteSpace([string]$loginResponse.accessToken)) {
    throw "Gateway login did not return accessToken. Check the gateway, AUTH-SERVICE, credentials, and service registration."
}

$headers = @{ Authorization = "Bearer $($loginResponse.accessToken)" }
$results = [System.Collections.Generic.List[object]]::new()

Write-Host "Sending $RequestCount consumer-driven discovery probes through Gateway -> Reservation -> ROOM-SERVICE..." -ForegroundColor Cyan
for ($attempt = 1; $attempt -le $RequestCount; $attempt++) {
    $timestamp = (Get-Date).ToString("o")
    try {
        $response = Invoke-RestMethod `
            -Method Get `
            -Uri "$GatewayUrl/api/v1/discovery/room-provider" `
            -Headers $headers

        $results.Add([pscustomobject]@{
            timestamp  = $timestamp
            attempt    = $attempt
            outcome    = "SUCCESS"
            serviceId  = [string]$response.serviceId
            instanceId = [string]$response.instanceId
            port       = [string]$response.port
            error      = ""
        })
        Write-Host ("[{0,2}/{1}] {2} ({3})" -f $attempt, $RequestCount, $response.instanceId, $response.port) -ForegroundColor Green
    }
    catch {
        $results.Add([pscustomobject]@{
            timestamp  = $timestamp
            attempt    = $attempt
            outcome    = "FAILED"
            serviceId  = ""
            instanceId = ""
            port       = ""
            error      = $_.Exception.Message
        })
        Write-Warning ("[{0,2}/{1}] FAILED: {2}" -f $attempt, $RequestCount, $_.Exception.Message)
    }

    if ($DelayMilliseconds -gt 0 -and $attempt -lt $RequestCount) {
        Start-Sleep -Milliseconds $DelayMilliseconds
    }
}

$successful = @($results | Where-Object { $_.outcome -eq "SUCCESS" })
$distribution = @($successful |
        Group-Object -Property instanceId, port |
        Sort-Object -Property Count -Descending |
        ForEach-Object {
            [pscustomobject]@{
                instanceId = $_.Group[0].instanceId
                port       = $_.Group[0].port
                hits       = $_.Count
            }
        })

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$csvPath = Join-Path $OutputDirectory "th7-discovery-$runId.csv"
$jsonPath = Join-Path $OutputDirectory "th7-discovery-summary-$runId.json"
$results | Export-Csv -NoTypeInformation -Encoding utf8 -Path $csvPath

$summary = [pscustomobject]@{
    runAt                    = (Get-Date).ToString("o")
    gatewayUrl                = $GatewayUrl
    requestedCalls            = $RequestCount
    successfulCalls           = $successful.Count
    failedCalls               = $results.Count - $successful.Count
    minimumDistinctInstances  = $MinimumDistinctInstances
    distinctInstancesObserved = $distribution.Count
    distribution              = $distribution
    rawEvidenceCsv            = $csvPath
}
$summary | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 -Path $jsonPath

Write-Host "`nDistribution:" -ForegroundColor Cyan
$distribution | Format-Table -AutoSize | Out-Host
Write-Host "Raw request evidence: $csvPath" -ForegroundColor Cyan
Write-Host "Summary: $jsonPath" -ForegroundColor Cyan

if ($successful.Count -ne $RequestCount) {
    throw "TH7 distribution baseline FAILED: $($results.Count - $successful.Count) of $RequestCount discovery calls failed. Evidence files were still written."
}

if ($distribution.Count -lt $MinimumDistinctInstances) {
    throw "TH7 distribution baseline FAILED: observed $($distribution.Count) provider instance(s), expected at least $MinimumDistinctInstances. Confirm room-service-1 and room-service-2 are both UP in Eureka. Evidence files were still written."
}

Write-Host "TH7 distribution baseline PASSED: all $RequestCount calls succeeded across $($distribution.Count) ROOM-SERVICE instance(s)." -ForegroundColor Green
