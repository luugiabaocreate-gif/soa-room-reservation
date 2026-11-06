<#
.SYNOPSIS
Loads root .env values into the current PowerShell process for local Java services.

.USAGE
From the repository root run: . .\scripts\Import-LocalEnv.ps1
The leading dot is required so environment variables remain in the current terminal.
#>
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '..\.env')
)

$resolved = [System.IO.Path]::GetFullPath($EnvFile)
if (-not (Test-Path -LiteralPath $resolved)) {
    throw "Cannot find $resolved. Run: Copy-Item .env.example .env, then replace secret placeholders."
}

Get-Content -LiteralPath $resolved | ForEach-Object {
    $line = $_.Trim()

    if (-not $line -or $line.StartsWith('#')) {
        return
    }

    $pair = $line.Split('=', 2)
    if ($pair.Count -ne 2) {
        throw "Invalid .env line: $line"
    }

    [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
}

if ($env:JWT_SECRET -like 'REPLACE_*' -or $env:RESERVATION_SERVICE_CLIENT_SECRET -like 'REPLACE_*') {
    throw 'Replace JWT_SECRET and RESERVATION_SERVICE_CLIENT_SECRET in .env before starting services.'
}

Write-Host 'Loaded local .env into the current PowerShell.' -ForegroundColor Green
