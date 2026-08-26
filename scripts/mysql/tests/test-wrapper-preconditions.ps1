$ErrorActionPreference = 'Stop'

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..\..')
$powerShellExecutable = (Get-Process -Id $PID).Path
$scriptsUnderTest = @(
    (Join-Path $projectRoot 'scripts\mysql\apply-schema.ps1'),
    (Join-Path $projectRoot 'scripts\mysql\verify-schema.ps1')
)

Remove-Item Env:FLOOD_DB_ADMIN_PASSWORD -ErrorAction SilentlyContinue

foreach ($scriptPath in $scriptsUnderTest) {
    $output = & $powerShellExecutable -NoProfile -NonInteractive -File $scriptPath 2>&1
    $exitCode = $LASTEXITCODE

    if ($exitCode -eq 0) {
        throw "Expected $scriptPath to fail when FLOOD_DB_ADMIN_PASSWORD is empty."
    }

    if (($output -join "`n") -notmatch 'FLOOD_DB_ADMIN_PASSWORD') {
        throw "Expected $scriptPath to explain the missing FLOOD_DB_ADMIN_PASSWORD variable."
    }
}

Write-Output 'Wrapper precondition tests passed (2 scripts).'
exit 0
