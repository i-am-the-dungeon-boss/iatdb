#Requires -Version 5.1
Set-StrictMode -Version Latest

function Invoke-Checked {
    param(
        [Parameter(Mandatory)] [scriptblock] $Command,
        [Parameter(Mandatory)] [string] $FailMessage
    )
    & $Command
    if ($LASTEXITCODE -ne 0) { throw $FailMessage }
}
