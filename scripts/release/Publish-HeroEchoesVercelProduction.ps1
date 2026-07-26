#Requires -Version 5.1
Set-StrictMode -Version Latest

<#
.SYNOPSIS
  Invoke hero-echoes scripts/promote-vercel.ps1 after a successful iatdb release.

.DESCRIPTION
  Vercel promote ownership lives in the hero-echoes repo. This wrapper only
  resolves the sibling checkout and runs that script.
#>
function Resolve-HeroEchoesRoot {
    param(
        [string] $IatdbRoot,
        [string] $Override = ''
    )
    if (-not [string]::IsNullOrWhiteSpace($Override)) {
        if (-not (Test-Path -LiteralPath $Override)) {
            throw "Hero Echoes root not found: $Override"
        }
        return (Resolve-Path -LiteralPath $Override).Path
    }
    $fromEnv = [Environment]::GetEnvironmentVariable('HERO_ECHOES_ROOT', 'Process')
    if (-not [string]::IsNullOrWhiteSpace($fromEnv)) {
        if (-not (Test-Path -LiteralPath $fromEnv)) {
            throw "HERO_ECHOES_ROOT not found: $fromEnv"
        }
        return (Resolve-Path -LiteralPath $fromEnv).Path
    }
    $sibling = Join-Path $IatdbRoot '..\hero-echoes'
    if (-not (Test-Path -LiteralPath $sibling)) {
        throw @"
Hero Echoes repo not found at $sibling.
Pass -HeroEchoesRoot, set HERO_ECHOES_ROOT, or clone it next to iatdb.
"@
    }
    return (Resolve-Path -LiteralPath $sibling).Path
}

function Publish-HeroEchoesVercelProduction {
    param(
        [Parameter(Mandatory)] [string] $IatdbRoot,
        [string] $HeroEchoesRoot = '',
        [string] $Remote = 'origin',
        [switch] $DryRun
    )

    $root = Resolve-HeroEchoesRoot -IatdbRoot $IatdbRoot -Override $HeroEchoesRoot
    $scriptPath = Join-Path $root 'scripts\promote-vercel.ps1'
    if (-not (Test-Path -LiteralPath $scriptPath)) {
        throw "Missing hero-echoes promote script: $scriptPath"
    }

    # Hashtable splat (named params). Array splat would bind positionally and
    # turn `-Remote origin` into ProjectName=origin.
    $promoteArgs = @{
        Remote = $Remote
    }
    if ($DryRun) { $promoteArgs['DryRun'] = $true }

    Write-Host ">> Invoking hero-echoes promote-vercel.ps1"
    & $scriptPath @promoteArgs
    if ($LASTEXITCODE -ne 0) {
        throw "hero-echoes promote-vercel.ps1 failed with exit code $LASTEXITCODE"
    }
}
