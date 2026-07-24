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
