#Requires -Version 5.1
Set-StrictMode -Version Latest

function Build-PlayAab([string] $Gradlew) {
    # Full game for Google Play. Do not pass -Pdemo.
    Invoke-ReleaseGradle -Gradlew $Gradlew -GradleArgs @('prepareRelease')
}

function Require-PlayAab([string] $DistDir, [string] $VersionName) {
    return Get-RequiredDistFile $DistDir "iatdb-$VersionName-android.aab"
}
