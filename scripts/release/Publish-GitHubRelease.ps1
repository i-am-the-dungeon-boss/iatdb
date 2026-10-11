#Requires -Version 5.1
Set-StrictMode -Version Latest

function Remove-GitHubReleaseAssets([string] $TagName) {
    $names = @(gh release view $TagName --json assets --jq '.assets[].name')
    if ($LASTEXITCODE -ne 0) { throw 'gh release view assets failed' }
    foreach ($name in $names) {
        $trimmed = "$name".Trim()
        if (-not $trimmed) { continue }
        Write-Host ">> gh release delete-asset $TagName $trimmed"
        Invoke-Checked { gh release delete-asset $TagName $trimmed --yes } "failed to delete asset $trimmed"
    }
}

function Publish-GitHubRelease(
    [string] $TagName,
    [string] $VersionName,
    [int] $VersionCode,
    [string] $CommitSha,
    [string] $DistDir,
    [string] $NotesFile,
    [bool] $Draft,
    [bool] $DryRun,
    [bool] $Force,
    [hashtable] $ProjectLinks
) {
    if ($NotesFile) {
        if (-not (Test-Path -LiteralPath $NotesFile)) { throw "Notes file not found: $NotesFile" }
        $notesPath = (Resolve-Path -LiteralPath $NotesFile).Path
    } else {
        $notesPath = Join-Path $DistDir 'RELEASE_NOTES.md'
        $body = Get-ReleaseNoteBody `
            -VersionName $VersionName `
            -VersionCode $VersionCode `
            -TagName $TagName `
            -CommitSha $CommitSha `
            -ProjectLinks $ProjectLinks
        Set-Content -LiteralPath $notesPath -Value $body -Encoding utf8
    }

    # Paused: upload APK, JAR, IPA, and desktop zips as GitHub Release assets.
    # $assets = [System.Collections.Generic.List[string]]::new()
    # $assets.Add($apk.FullName) | Out-Null
    # $assets.Add($jar.FullName) | Out-Null
    # $assets.Add($iosIpa.FullName) | Out-Null
    # $assets.Add($sums.FullName) | Out-Null
    # $jpackageZips = @(Get-ChildItem -LiteralPath $DistDir -Filter "iatdb-$VersionName-desktop-*.zip" -File -ErrorAction SilentlyContinue)
    # foreach ($z in $jpackageZips) { $assets.Add($z.FullName) | Out-Null }
    # $body = Get-ReleaseNoteBody ... -AssetNames @($assets | ForEach-Object { Split-Path $_ -Leaf }) -IncludesJpackage $includesJpackage
    # Write-Host ">> gh release upload $TagName --clobber"
    # & gh release upload $TagName @assets --clobber
    # $ghArgs += @($assets)

    Write-Host "Notes: $notesPath"
    Write-Host 'Store builds stay in dist/. GitHub Release gets Play and Steam links only.'

    $releaseTitle = "$VersionName - I am the Dungeon Boss"
    & gh release view $TagName --json tagName *> $null
    $releaseExists = ($LASTEXITCODE -eq 0)

    if ($releaseExists -and -not $Force) {
        throw "GitHub Release $TagName already exists. Bump the version, or pass -Force to overwrite it."
    }

    if ($releaseExists) {
        Write-Host ">> gh release edit $TagName (notes only)"
        if (-not $DryRun) {
            & gh release edit $TagName --title $releaseTitle --notes-file $notesPath
            if ($LASTEXITCODE -ne 0) {
                throw "gh release edit failed with exit code $LASTEXITCODE"
            }
            Remove-GitHubReleaseAssets $TagName
        }
    } else {
        $ghArgs = @(
            'release', 'create', $TagName,
            '--title', $releaseTitle,
            '--notes-file', $notesPath
        )
        if ($Draft) { $ghArgs += '--draft' }

        Write-Host ">> gh $($ghArgs -join ' ')"
        if (-not $DryRun) {
            & gh @ghArgs
            if ($LASTEXITCODE -ne 0) {
                throw "gh release create failed with exit code $LASTEXITCODE"
            }
        }
    }
}
