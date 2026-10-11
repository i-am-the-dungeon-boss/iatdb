#Requires -Version 5.1
# Paused release paths. Not dot-sourced. Uncomment a block in release.ps1 to turn it back on.

# . (Join-Path $releaseLib 'Fetch-UnsignedIosIpa.ps1')

# Paused: overlap cold macOS CI with local tests + prepareRelease when the IPA won't be built here.
# $ipaExpectedPath = Join-Path $distDir "iatdb-$versionName-ios-unsigned.ipa"
# $willBuildIpaLocally = $onMacOs -and -not $SkipBuild
# $iosRunId = $null
# if (-not $DryRun -and -not $willBuildIpaLocally -and -not (Test-Path -LiteralPath $ipaExpectedPath)) {
#     $iosRunId = Start-UnsignedIosIpaViaActions -CommitSha $commitSha
# }

# $gradleArgs = @('prepareRelease', '-PwithJpackage')
# Invoke-ReleaseGradle -Gradlew $gradlew -GradleArgs $gradleArgs

# Paused: require APK, JAR, and unsigned IPA, then attach them to the GitHub Release.
# $apk = Get-RequiredDistFile $distDir "iatdb-$versionName-android.apk"
# $jar = Get-RequiredDistFile $distDir "iatdb-$versionName-desktop.jar"
# $iosIpa = Get-ChildItem -LiteralPath $distDir -Filter "iatdb-$versionName-ios-unsigned.ipa" -File -ErrorAction SilentlyContinue | Select-Object -First 1
# if (-not $iosIpa) {
#     if ($iosRunId) {
#         Complete-UnsignedIosIpaViaActions -DistDir $distDir -VersionName $versionName -RunId $iosRunId | Out-Null
#     } else {
#         Get-UnsignedIosIpaViaActions -DistDir $distDir -VersionName $versionName -CommitSha $commitSha | Out-Null
#     }
#     $iosIpa = Get-Item -LiteralPath (Join-Path $distDir "iatdb-$versionName-ios-unsigned.ipa")
# }
# if (-not $iosIpa) {
#     throw "Missing unsigned iOS IPA under $distDir (expected iatdb-$versionName-ios-unsigned.ipa)"
# }

# Paused: notes listed sideload artifacts.
# if ($NotesFile) {
#     if (-not (Test-Path -LiteralPath $NotesFile)) { throw "Notes file not found: $NotesFile" }
#     $notesPath = (Resolve-Path -LiteralPath $NotesFile).Path
# } else {
#     $notesPath = Join-Path $distDir 'RELEASE_NOTES.md'
#     $body = Get-ReleaseNoteBody `
#         -VersionName $versionName `
#         -VersionCode $versionCode `
#         -TagName $tagName `
#         -CommitSha $commitSha `
#         -AssetNames @($assets | ForEach-Object { Split-Path $_ -Leaf }) `
#         -IncludesJpackage $includesJpackage `
#         -ProjectLinks $projectLinks
#     Set-Content -LiteralPath $notesPath -Value $body -Encoding utf8
# }
# Write-Host 'Artifacts:'
# foreach ($a in $assets) { Write-Host "  - $(Split-Path $a -Leaf)" }

# Paused: upload binaries on the GitHub Release.
# Write-Host ">> gh release upload $tagName --clobber"
# & gh release upload $tagName @assets --clobber
# $ghArgs += @($assets)
