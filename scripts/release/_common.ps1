#Requires -Version 5.1
Set-StrictMode -Version Latest

function Invoke-Checked {
    param(
        [Parameter(Mandatory)] [scriptblock] $Command,
        [Parameter(Mandatory)] [string] $FailMessage
    )
    # Native tools (gh/git) write to the success stream; keep it off the pipeline
    # so callers are not polluted with empty lines / progress text.
    & $Command | Out-Host
    if ($LASTEXITCODE -ne 0) { throw $FailMessage }
}

function Assert-Command([string] $Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found on PATH: $Name"
    }
}

function Read-KeyValueFile([string] $Path) {
    $map = @{}
    if (-not (Test-Path -LiteralPath $Path)) { return $map }
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        $eq = $line.IndexOf('=')
        if ($eq -le 0) { return }
        $key = $line.Substring(0, $eq).Trim()
        $value = $line.Substring($eq + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $map[$key] = $value
    }
    return $map
}

function Import-DotEnv([string] $Path) {
    foreach ($entry in (Read-KeyValueFile $Path).GetEnumerator()) {
        $existing = [Environment]::GetEnvironmentVariable($entry.Key, 'Process')
        if ([string]::IsNullOrWhiteSpace($existing)) {
            Set-Item -Path "Env:$($entry.Key)" -Value $entry.Value
        }
    }
}

function Get-AppVersion([string] $BuildGradlePath) {
    $text = Get-Content -LiteralPath $BuildGradlePath -Raw
    if ($text -notmatch "appVersionName\s*=\s*'([^']+)'") {
        throw "Could not parse appVersionName from $BuildGradlePath"
    }
    $name = $Matches[1]
    if ($text -notmatch 'appVersionCode\s*=\s*(\d+)') {
        throw "Could not parse appVersionCode from $BuildGradlePath"
    }
    return @{ Name = $name; Code = [int]$Matches[1] }
}

function Get-ProjectLinks([string] $Root) {
    $path = Join-Path $Root 'services\src\main\resources\project-links.properties'
    $map = Read-KeyValueFile $path
    if ($map.Count -eq 0) { throw "Missing project-links.properties at $path" }
    foreach ($required in @('github.owner.repo', 'developer.email')) {
        if ([string]::IsNullOrWhiteSpace($map[$required])) {
            throw "Missing required property '$required' in $path"
        }
    }
    $ownerRepo = $map['github.owner.repo']
    return @{
        GithubOwnerRepo = $ownerRepo
        GithubRepoUrl   = "https://github.com/$ownerRepo"
        DeveloperEmail  = $map['developer.email']
    }
}

function Test-JdkHasJlink([string] $JavaHome) {
    if ([string]::IsNullOrWhiteSpace($JavaHome)) { return $false }
    if (-not (Test-Path -LiteralPath $JavaHome)) { return $false }
    return (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\jlink.exe')) -or
        (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/jlink'))
}

function Resolve-ReleaseJavaHome {
    # Android JdkImageTransform needs a real JDK with jlink. The Cursor/VS Code
    # Red Hat Java extension embeds a stripped Temurin JRE (no jlink) that Gradle
    # may otherwise pick as the Daemon JVM.
    $candidates = [System.Collections.Generic.List[string]]::new()
    foreach ($key in @('JAVA_HOME', 'JDK_HOME')) {
        $value = [Environment]::GetEnvironmentVariable($key, 'Process')
        if (-not [string]::IsNullOrWhiteSpace($value)) { $candidates.Add($value.Trim()) }
    }
    $userGradleHome = Join-Path $env:USERPROFILE '.gradle\gradle.properties'
    if (Test-Path -LiteralPath $userGradleHome) {
        $text = Get-Content -LiteralPath $userGradleHome -Raw
        if ($text -match 'org\.gradle\.java\.home\s*=\s*(\S+)') {
            $candidates.Add(($Matches[1] -replace '/', '\').Trim())
        }
    }
    $candidates.Add((Join-Path $env:USERPROFILE '.gradle\jdks\eclipse_adoptium-21-amd64-windows.2'))
    $candidates.Add('C:\Program Files\Android\Android Studio\jbr')
    $candidates.Add('C:\Program Files\Java\jdk-17')
    $candidates.Add('C:\Program Files\Eclipse Adoptium\jdk-21.0.7+6-hotspot')

    $seen = @{}
    foreach ($raw in $candidates) {
        if ([string]::IsNullOrWhiteSpace($raw)) { continue }
        $jdkHome = $raw.Trim().Trim('"').Trim("'")
        $key = $jdkHome.ToLowerInvariant()
        if ($seen.ContainsKey($key)) { continue }
        $seen[$key] = $true
        # Never accept the Red Hat / JustJ embedded JRE from the Java extension.
        if ($jdkHome -match '[\\/]\.cursor[\\/]extensions[\\/]redhat\.java') { continue }
        if ($jdkHome -match '[\\/]org\.eclipse\.justj') { continue }
        if (Test-JdkHasJlink $jdkHome) {
            return (Resolve-Path -LiteralPath $jdkHome).Path
        }
    }

    throw @'
No JDK with jlink.exe found for release builds.

Android compileRelease needs jlink (JdkImageTransform). The Cursor Red Hat Java
extension JRE is not a full JDK — do not use it for Gradle.

Install Temurin 21 (or Android Studio JBR), set JAVA_HOME to that JDK, then retry.
'@
}

function Assert-ReleaseJavaHome {
    $javaHome = Resolve-ReleaseJavaHome
    $env:JAVA_HOME = $javaHome
    # Daemon JVM criteria (gradle/gradle-daemon-jvm.properties) overrides
    # org.gradle.java.home and can still select the IDE's stripped JRE.
    $env:ORG_GRADLE_JAVA_HOME = $javaHome
    Write-Host ">> JAVA_HOME=$javaHome (jlink OK)"
    return $javaHome
}

function Suspend-DaemonJvmCriteria([string] $Root) {
    $path = Join-Path $Root 'gradle\gradle-daemon-jvm.properties'
    if (-not (Test-Path -LiteralPath $path)) {
        return $null
    }
    $backup = "$path.release-bak"
    Move-Item -LiteralPath $path -Destination $backup -Force
    Write-Host '>> Suspended gradle-daemon-jvm.properties (use JAVA_HOME / org.gradle.java.home)'
    return $backup
}

function Restore-DaemonJvmCriteria([string] $BackupPath) {
    if ([string]::IsNullOrWhiteSpace($BackupPath)) { return }
    if (-not (Test-Path -LiteralPath $BackupPath)) { return }
    $original = $BackupPath -replace '\.release-bak$', ''
    Move-Item -LiteralPath $BackupPath -Destination $original -Force
    Write-Host '>> Restored gradle-daemon-jvm.properties'
}

function Invoke-ReleaseGradle([string] $Gradlew, [string[]] $GradleArgs) {
    Write-Host ">> $Gradlew $($GradleArgs -join ' ')"
    if ($DryRun) { return }
    & $Gradlew @GradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw ("Gradle {0} failed with exit code {1}" -f ($GradleArgs -join ' '), $LASTEXITCODE)
    }
}

function Get-RequiredDistFile([string] $DistDir, [string] $Filter) {
    $item = Get-ChildItem -LiteralPath $DistDir -Filter $Filter -File -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $item) { throw "Missing $Filter under $DistDir" }
    return $item
}
