<#
.SYNOPSIS
Stops the Web UI process created by Start-Project and the complete Docker stack.

.USAGE
From the repository root:
  .\Stop-Project.cmd
#>
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$WebUiPidFile = Join-Path $RepositoryRoot '.runtime\web-ui.pid'

try {
    if (Test-Path $WebUiPidFile) {
        $processId = [int](Get-Content -Raw $WebUiPidFile)
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($null -ne $process) {
            Write-Host "Stopping Web UI process tree (PID $processId)" -ForegroundColor Cyan
            & taskkill.exe /PID $processId /T /F *> $null
        }
        Remove-Item -Force $WebUiPidFile
    }
    else {
        Write-Host 'No Web UI PID file found; leaving any manually started UI process untouched.' -ForegroundColor DarkGray
    }

    Set-Location $RepositoryRoot
    Write-Host 'Stopping Docker Compose stack (data volumes are preserved)' -ForegroundColor Cyan
    & docker compose -f docker-compose.yml -f docker-compose.th7.yml down --remove-orphans
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose failed with exit code $LASTEXITCODE."
    }

    Write-Host 'PROJECT STOPPED. PostgreSQL/RabbitMQ volumes were not deleted.' -ForegroundColor Green
}
catch {
    Write-Error $_.Exception.Message
    exit 1
}
