[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$Sha,

    [Parameter(Mandatory = $true)]
    [string]$EnvFile,

    [string]$BackendRepository = 'ghcr.io/johnvo402/retail-hub-backend',

    [string]$FrontendRepository = 'ghcr.io/johnvo402/retail-hub-frontend',

    [int]$HealthTimeoutSeconds = 300
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repositoryRoot 'docker-compose.prod.yml'

function Resolve-RequiredFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "$Description was not found at '$Path'."
    }

    return (Resolve-Path -LiteralPath $Path).Path
}

$script:ComposeFilePath = Resolve-RequiredFile -Path $composeFile -Description 'Production Compose file'
$script:EnvFilePath = Resolve-RequiredFile -Path $EnvFile -Description 'Production environment file'

function Invoke-Compose {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    & docker compose --env-file $script:EnvFilePath -f $script:ComposeFilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $($Arguments -join ' ') failed with exit code $LASTEXITCODE."
    }
}

function Get-ServiceContainerId {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Service
    )

    $containerId = & docker compose --env-file $script:EnvFilePath -f $script:ComposeFilePath ps -q $Service 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
        return $null
    }

    return ($containerId | Select-Object -First 1).Trim()
}

function Get-RunningImage {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Service
    )

    $containerId = Get-ServiceContainerId -Service $Service
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        return $null
    }

    $image = & docker inspect --format '{{.Config.Image}}' $containerId 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($image)) {
        return $null
    }

    return $image.Trim()
}

function Wait-ServiceHealthy {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Service
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($HealthTimeoutSeconds)
    do {
        $containerId = Get-ServiceContainerId -Service $Service
        if (-not [string]::IsNullOrWhiteSpace($containerId)) {
            $status = & docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $containerId 2>$null
            if ($LASTEXITCODE -eq 0) {
                $status = $status.Trim()
                if ($status -eq 'healthy' -or $status -eq 'running') {
                    Write-Host "$Service is $status."
                    return
                }

                if ($status -eq 'exited' -or $status -eq 'dead') {
                    throw "$Service entered terminal state '$status'."
                }
            }
        }

        Start-Sleep -Seconds 5
    } while ([DateTime]::UtcNow -lt $deadline)

    throw "$Service did not become healthy within $HealthTimeoutSeconds seconds."
}

function Get-EnvFileValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    foreach ($line in Get-Content -LiteralPath $script:EnvFilePath) {
        if ($line -match "^\s*$([Regex]::Escape($Name))\s*=\s*(.*?)\s*$") {
            return $Matches[1].Trim().Trim('"').Trim("'")
        }
    }

    return $null
}

function Get-PublicHealthUrl {
    $publicOrigin = $env:PUBLIC_ORIGIN
    if ([string]::IsNullOrWhiteSpace($publicOrigin)) {
        $publicOrigin = Get-EnvFileValue -Name 'PUBLIC_ORIGIN'
    }

    if ([string]::IsNullOrWhiteSpace($publicOrigin)) {
        throw 'PUBLIC_ORIGIN must be configured for the post-deployment health check.'
    }

    return "$($publicOrigin.Trim().TrimEnd('/'))/api/health"
}

function Wait-PublicHealth {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($HealthTimeoutSeconds)
    $lastFailure = 'No request completed.'
    do {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 15
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                Write-Host "Public health check passed at $Url."
                return
            }

            $lastFailure = "HTTP $($response.StatusCode)"
        } catch {
            $lastFailure = $_.Exception.Message
        }

        Start-Sleep -Seconds 5
    } while ([DateTime]::UtcNow -lt $deadline)

    throw "Public health check failed at '$Url': $lastFailure"
}

function Wait-StackHealthy {
    foreach ($service in @('postgres', 'redis', 'elasticsearch', 'backend', 'frontend')) {
        Wait-ServiceHealthy -Service $service
    }

    Wait-PublicHealth -Url (Get-PublicHealthUrl)
}

function Show-Diagnostics {
    Write-Host 'Container status:'
    & docker compose --env-file $script:EnvFilePath -f $script:ComposeFilePath ps

    Write-Host 'Recent application logs (bounded to 100 lines per service):'
    & docker compose --env-file $script:EnvFilePath -f $script:ComposeFilePath logs --tail 100 backend frontend
}

$normalizedSha = $Sha.ToLowerInvariant()
$newBackendImage = "$($BackendRepository.TrimEnd('/')):$normalizedSha"
$newFrontendImage = "$($FrontendRepository.TrimEnd('/')):$normalizedSha"

$env:BACKEND_IMAGE = $newBackendImage
$env:FRONTEND_IMAGE = $newFrontendImage

Invoke-Compose -Arguments @('config', '--quiet')

$previousBackendImage = Get-RunningImage -Service 'backend'
$previousFrontendImage = Get-RunningImage -Service 'frontend'

try {
    Write-Host "Pulling immutable application images for commit $normalizedSha."
    Invoke-Compose -Arguments @('pull', 'backend', 'frontend')

    Write-Host 'Updating application services without stopping infrastructure or removing volumes.'
    Invoke-Compose -Arguments @('up', '-d', '--no-build', 'backend', 'frontend')
    Wait-StackHealthy

    Invoke-Compose -Arguments @('ps')
    Write-Host "Deployment succeeded:"
    Write-Host "  backend:  $newBackendImage"
    Write-Host "  frontend: $newFrontendImage"
} catch {
    $deploymentFailure = $_.Exception.Message
    Write-Host "Deployment failed: $deploymentFailure" -ForegroundColor Red
    Show-Diagnostics

    $canRollback = -not [string]::IsNullOrWhiteSpace($previousBackendImage) -and
        -not [string]::IsNullOrWhiteSpace($previousFrontendImage)

    if ($canRollback) {
        Write-Host 'Restoring the previously running application images.' -ForegroundColor Yellow
        $env:BACKEND_IMAGE = $previousBackendImage
        $env:FRONTEND_IMAGE = $previousFrontendImage

        try {
            Invoke-Compose -Arguments @('up', '-d', '--no-build', 'backend', 'frontend')
            Wait-StackHealthy
            Write-Host 'Automatic rollback succeeded.' -ForegroundColor Yellow
        } catch {
            Write-Host "Automatic rollback failed: $($_.Exception.Message)" -ForegroundColor Red
            Show-Diagnostics
        }
    } else {
        Write-Host 'No previous application containers were found; automatic rollback is unavailable for this first deployment.' -ForegroundColor Yellow
    }

    exit 1
}
