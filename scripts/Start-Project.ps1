<#
.SYNOPSIS
Starts the complete local SOA Room Reservation stack with one command.

.DESCRIPTION
Starts Docker Compose (databases, RabbitMQ, Eureka, Gateway and all backend
services), waits until every required service is healthy and registered in
Eureka, then starts the Vite Web UI in the background.

.USAGE
From the repository root:
  .\Start-Project.cmd

Use -SkipBuild when Docker images are known to be current:
  .\Start-Project.cmd -SkipBuild
#>
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$NoWebUi,
    [ValidateRange(60, 900)]
    [int]$TimeoutSeconds = 300
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$RuntimeDirectory = Join-Path $RepositoryRoot '.runtime'
$LogDirectory = Join-Path $RuntimeDirectory 'logs'
$WebUiPidFile = Join-Path $RuntimeDirectory 'web-ui.pid'

function Invoke-Compose {
    param([string[]]$ComposeArguments)

    & docker @ComposeArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose failed with exit code $LASTEXITCODE."
    }
}

function Test-HttpOk {
    param([string]$Uri)

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 5
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 300
    }
    catch {
        return $false
    }
}

function Wait-ForHttp {
    param(
        [string]$Name,
        [string]$Uri,
        [datetime]$Deadline
    )

    Write-Host "Waiting for $Name at $Uri" -ForegroundColor DarkCyan
    while ((Get-Date) -lt $Deadline) {
        if (Test-HttpOk -Uri $Uri) {
            Write-Host "READY  $Name" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "$Name did not become ready before the timeout. Run: docker compose -f docker-compose.yml -f docker-compose.th7.yml logs $($Name.ToLower().Replace(' ', '-')) --tail 120"
}

function Get-EurekaApplications {
    $response = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri 'http://localhost:8761/eureka/apps' `
        -Headers @{ Accept = 'application/json' } `
        -TimeoutSec 5

    $payload = $response.Content | ConvertFrom-Json
    return @($payload.applications.application)
}

function Wait-ForEurekaRegistration {
    param([datetime]$Deadline)

    $requiredServiceIds = @(
        'API-GATEWAY',
        'AUTH-SERVICE',
        'AUDIT-SERVICE',
        'RESERVATION-SERVICE',
        'ROOM-SERVICE'
    )

    Write-Host 'Waiting for all required Service IDs in Eureka' -ForegroundColor DarkCyan
    while ((Get-Date) -lt $Deadline) {
        try {
            $applications = @(Get-EurekaApplications)
            $registeredIds = @($applications | ForEach-Object { [string]$_.name })
            $missingIds = @($requiredServiceIds | Where-Object { $_ -notin $registeredIds })
            $roomApplication = @($applications | Where-Object { $_.name -eq 'ROOM-SERVICE' } | Select-Object -First 1)
            $roomInstanceCount = if ($roomApplication.Count -eq 1) { @($roomApplication[0].instance).Count } else { 0 }

            if ($missingIds.Count -eq 0 -and $roomInstanceCount -ge 2) {
                Write-Host 'READY  Eureka registration: 5 Service IDs and 2 ROOM-SERVICE instances' -ForegroundColor Green
                return
            }

            $missingText = if ($missingIds.Count -gt 0) { $missingIds -join ', ' } else { 'none' }
            Write-Host "  Eureka pending. Missing: $missingText; ROOM-SERVICE instances: $roomInstanceCount" -ForegroundColor DarkGray
        }
        catch {
            Write-Host '  Eureka registry is not queryable yet.' -ForegroundColor DarkGray
        }

        Start-Sleep -Seconds 3
    }

    throw 'Eureka did not reach the required registration state before the timeout. Open http://localhost:8761 and inspect Docker logs.'
}

function Start-WebUi {
    param([datetime]$Deadline)

    $webUiDirectory = Join-Path $RepositoryRoot 'web-ui'
    $webUiUrl = 'http://localhost:5173'

    if (Test-HttpOk -Uri $webUiUrl) {
        Write-Host 'READY  Web UI is already responding on http://localhost:5173' -ForegroundColor Green
        return
    }

    $npm = Get-Command 'npm.cmd' -ErrorAction SilentlyContinue
    if ($null -eq $npm) {
        $npm = Get-Command 'npm' -ErrorAction SilentlyContinue
    }
    if ($null -eq $npm) {
        throw 'Cannot find npm. Install Node.js 18+ and run this command again.'
    }

    if (-not (Test-Path (Join-Path $webUiDirectory 'node_modules'))) {
        Write-Host 'Installing Web UI packages with npm ci (first run only)' -ForegroundColor DarkCyan
        Push-Location $webUiDirectory
        try {
            & $npm.Source ci
            if ($LASTEXITCODE -ne 0) {
                throw "npm ci failed with exit code $LASTEXITCODE."
            }
        }
        finally {
            Pop-Location
        }
    }

    New-Item -ItemType Directory -Force -Path $LogDirectory | Out-Null
    Remove-Item -Force -ErrorAction SilentlyContinue $WebUiPidFile

    $standardOutputLog = Join-Path $LogDirectory 'web-ui.out.log'
    $standardErrorLog = Join-Path $LogDirectory 'web-ui.err.log'
    $process = Start-Process `
        -FilePath $npm.Source `
        -ArgumentList @('run', 'dev', '--', '--port', '5173', '--strictPort') `
        -WorkingDirectory $webUiDirectory `
        -RedirectStandardOutput $standardOutputLog `
        -RedirectStandardError $standardErrorLog `
        -PassThru `
        -WindowStyle Hidden

    Set-Content -Path $WebUiPidFile -Value $process.Id -NoNewline
    Wait-ForHttp -Name 'Web UI' -Uri $webUiUrl -Deadline $Deadline
}

try {
    Set-Location $RepositoryRoot

    if ($null -eq (Get-Command 'docker' -ErrorAction SilentlyContinue)) {
        throw 'Cannot find Docker CLI. Start Docker Desktop, wait for Engine running, then run this command again.'
    }

    & docker version --format '{{.Server.Version}}' *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Engine is not available. Start Docker Desktop and wait for Engine running.'
    }

    # Validates .env, secrets, and loads the same values for Docker Compose.
    . (Join-Path $PSScriptRoot 'Import-LocalEnv.ps1')

    $composeArguments = @('compose', '-f', 'docker-compose.yml', '-f', 'docker-compose.th7.yml', 'up', '-d')
    if (-not $SkipBuild) {
        $composeArguments += '--build'
    }

    Write-Host "Starting SOA Room Reservation from $RepositoryRoot" -ForegroundColor Cyan
    Invoke-Compose -ComposeArguments $composeArguments

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    Wait-ForHttp -Name 'Service Registry' -Uri 'http://localhost:8761/actuator/health' -Deadline $deadline
    Wait-ForHttp -Name 'API Gateway' -Uri 'http://localhost:8088/actuator/health' -Deadline $deadline
    Wait-ForHttp -Name 'Auth Service' -Uri 'http://localhost:8083/actuator/health' -Deadline $deadline
    Wait-ForHttp -Name 'Room Service 1' -Uri 'http://localhost:8080/actuator/health' -Deadline $deadline
    Wait-ForHttp -Name 'Room Service 2' -Uri 'http://localhost:8084/actuator/health' -Deadline $deadline
    Wait-ForHttp -Name 'Reservation Service' -Uri 'http://localhost:8081/actuator/health' -Deadline $deadline
    Wait-ForHttp -Name 'Audit Service' -Uri 'http://localhost:8082/actuator/health' -Deadline $deadline
    Wait-ForEurekaRegistration -Deadline $deadline

    if (-not $NoWebUi) {
        Start-WebUi -Deadline $deadline
    }

    Write-Host ''
    Write-Host 'PROJECT READY' -ForegroundColor Green
    Write-Host '  Web UI:           http://localhost:5173'
    Write-Host '  API Gateway:      http://localhost:8088/actuator/health'
    Write-Host '  Eureka Dashboard: http://localhost:8761'
    Write-Host '  Stop everything:  .\Stop-Project.cmd'
}
catch {
    Write-Host ''
    Write-Error $_.Exception.Message
    exit 1
}
