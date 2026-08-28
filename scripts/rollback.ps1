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

$deployScript = Join-Path $PSScriptRoot 'deploy.ps1'

Write-Host "Rolling production back to images built from commit $($Sha.ToLowerInvariant())."
& $deployScript `
    -Sha $Sha `
    -EnvFile $EnvFile `
    -BackendRepository $BackendRepository `
    -FrontendRepository $FrontendRepository `
    -HealthTimeoutSeconds $HealthTimeoutSeconds

exit $LASTEXITCODE
