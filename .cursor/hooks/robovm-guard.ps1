#!/usr/bin/env pwsh
# Stop hook: enforce .cursor/rules/cross-platform.mdc mechanically.
#
# Runs RoboVMUnsupportedApisTest when the agent touched Java under a guarded
# root, and feeds any failure back to the agent as a followup. Skips silently
# otherwise, so non-Java turns pay nothing.
#
# Contract: stdout must be empty or a single JSON object. All diagnostics go to
# stderr, and any internal error exits 0 so a broken hook never blocks work.

$ErrorActionPreference = 'Stop'

# The hook event payload is unused, but stdin must still be drained.
$null = [Console]::In.ReadToEnd()

function Emit-Followup([string] $message) {
	@{ followup_message = $message } | ConvertTo-Json -Compress -Depth 3 | Write-Output
	exit 0
}

try {
	$repo = (git rev-parse --show-toplevel 2>$null)
	if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repo)) { exit 0 }
	Set-Location $repo

	# Same roots the test walks; anything outside them cannot trip it.
	$roots = @(
		'core/src/main/java',
		'SPD-classes/src/main/java',
		'desktop/src/main/java',
		'android/src/main/java',
		'ios/src/main/java'
	)

	$changed = git status --porcelain --untracked-files=all -- $roots 2>$null |
		Where-Object { $_ -match '\.java$' }
	if (-not $changed) { exit 0 }

	$gradlew = if ($IsWindows -or $env:OS -eq 'Windows_NT') { '.\gradlew.bat' } else { './gradlew' }

	$startedAt = Get-Date
	$output = & $gradlew ':core:test' '--tests' 'com.watabou.utils.RoboVMUnsupportedApisTest' `
		'--console=plain' 2>&1 | Out-String
	if ($LASTEXITCODE -eq 0) { exit 0 }

	# The JUnit report names the offending pattern and file; Gradle's console does not.
	# Only trust a report this run produced — a compile failure leaves the previous
	# run's XML in place, which would report a stale, already-fixed violation.
	$detail = $null
	$report = 'core/build/test-results/test/TEST-com.watabou.utils.RoboVMUnsupportedApisTest.xml'
	if ((Test-Path $report) -and (Get-Item $report).LastWriteTime -ge $startedAt) {
		$failure = ([xml](Get-Content $report -Raw)).testsuite.testcase.failure
		if ($failure) { $detail = $failure.message }
	}
	if (-not $detail) {
		# No fresh verdict: the build broke before the test ran (usually a compile error).
		Emit-Followup @"
:core:test could not run the RoboVM guard after your Java edits — the build failed first.

$(($output -split "`n" | Select-Object -Last 40) -join "`n")

Fix the build, then re-run:

    ./gradlew :core:test --tests "com.watabou.utils.RoboVMUnsupportedApisTest"
"@
	}

	Emit-Followup @"
RoboVMUnsupportedApisTest failed after your Java edits, so this change would break the iOS (RoboVM) build.

$detail

Rewrite the flagged call sites using the portable equivalents in .cursor/rules/cross-platform.mdc (for example Strings.join instead of String.join, Collections.sort instead of list.sort, an iterator loop instead of removeIf). Do not add the pattern to the test's allowlist. Then re-run:

    ./gradlew :core:test --tests "com.watabou.utils.RoboVMUnsupportedApisTest"
"@
} catch {
	Write-Error "robovm-guard hook error: $_"
	exit 0
}
