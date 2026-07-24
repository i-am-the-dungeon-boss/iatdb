#Requires -Version 5.1
Set-StrictMode -Version Latest

function Update-Sha256Sums([string] $DistDir, [string] $VersionName, [int] $VersionCode) {
    $lines = @(
        "appVersionName=$VersionName",
        "appVersionCode=$VersionCode",
        "builtAt=$(Get-Date -Format o)",
        ''
    )
    Get-ChildItem -LiteralPath $DistDir -File |
        Where-Object { $_.Name -notin @('SHA256SUMS.txt', 'RELEASE_NOTES.md') } |
        Sort-Object Name |
        ForEach-Object {
            $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            $lines += "$hash  $($_.Name)"
        }
    Set-Content -LiteralPath (Join-Path $DistDir 'SHA256SUMS.txt') -Value ($lines -join "`n") -Encoding utf8
}
