#Requires -Version 5.1
Set-StrictMode -Version Latest

function Get-UnsignedIosIpaViaActions([string] $DistDir, [string] $VersionName, [string] $CommitSha) {
    $expectedName = "iatdb-$VersionName-ios-unsigned.ipa"
    $expectedPath = Join-Path $DistDir $expectedName
    $artifactName = "iatdb-$VersionName-ios-unsigned"
    $workflow = 'ios-unsigned.yml'

    $ref = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($ref -eq 'HEAD') { $ref = $CommitSha }

    git fetch --quiet origin 2>$null
    $remoteContains = @(git branch -r --contains $CommitSha 2>$null)
    if (-not $remoteContains -or $remoteContains.Count -eq 0) {
        throw "Commit $CommitSha is not on the remote. Push your branch before release so $workflow can build the unsigned IPA."
    }

    Write-Host ">> gh workflow run $workflow --ref $ref"
    Invoke-Checked { gh workflow run $workflow --ref $ref } "Failed to dispatch $workflow (is the workflow on the remote ref?)"

    Write-Host ">> Waiting for $workflow run at $CommitSha ..."
    $runId = $null
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 5
        $json = gh run list --workflow $workflow --limit 10 --json databaseId,headSha,status,conclusion
        if ($LASTEXITCODE -ne 0) { throw 'gh run list failed' }
        $match = ($json | ConvertFrom-Json) | Where-Object { $_.headSha -eq $CommitSha } | Select-Object -First 1
        if ($match) {
            $runId = [string]$match.databaseId
            break
        }
    }
    if (-not $runId) { throw "No $workflow run found for commit $CommitSha" }

    Write-Host ">> gh run watch $runId"
    Invoke-Checked { gh run watch $runId --exit-status } "ios-unsigned workflow run $runId failed"

    $downloadDir = Join-Path $DistDir '_ios-unsigned-download'
    if (Test-Path -LiteralPath $downloadDir) {
        Remove-Item -LiteralPath $downloadDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $downloadDir | Out-Null

    Write-Host ">> gh run download $runId -n $artifactName"
    Invoke-Checked {
        gh run download $runId -n $artifactName -D $downloadDir
    } "Failed to download artifact $artifactName from run $runId"

    $downloaded = Get-ChildItem -LiteralPath $downloadDir -Recurse -Filter $expectedName -File |
        Select-Object -First 1
    if (-not $downloaded) {
        $downloaded = Get-ChildItem -LiteralPath $downloadDir -Recurse -Filter '*.ipa' -File |
            Select-Object -First 1
    }
    if (-not $downloaded) { throw "Downloaded artifact did not contain $expectedName" }

    Copy-Item -LiteralPath $downloaded.FullName -Destination $expectedPath -Force
    Remove-Item -LiteralPath $downloadDir -Recurse -Force
    return $expectedPath
}
