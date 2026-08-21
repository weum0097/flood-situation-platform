[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:FLOOD_DB_ADMIN_PASSWORD)) {
    throw 'FLOOD_DB_ADMIN_PASSWORD is required.'
}

$mysqlPath = $env:FLOOD_DB_MYSQL_PATH
if ([string]::IsNullOrWhiteSpace($mysqlPath)) {
    $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($null -ne $mysqlCommand) {
        $mysqlPath = $mysqlCommand.Source
    }
}

if ([string]::IsNullOrWhiteSpace($mysqlPath)) {
    $defaultMysqlPath = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
    if (Test-Path -LiteralPath $defaultMysqlPath) {
        $mysqlPath = $defaultMysqlPath
    }
}

if ([string]::IsNullOrWhiteSpace($mysqlPath) -or
    -not (Test-Path -LiteralPath $mysqlPath -PathType Leaf)) {
    throw 'MySQL client was not found. Set FLOOD_DB_MYSQL_PATH to mysql.exe.'
}

$databaseHost = if ([string]::IsNullOrWhiteSpace($env:FLOOD_DB_HOST)) {
    '127.0.0.1'
} else {
    $env:FLOOD_DB_HOST
}
$databasePort = if ([string]::IsNullOrWhiteSpace($env:FLOOD_DB_PORT)) {
    '3306'
} else {
    $env:FLOOD_DB_PORT
}
$databaseUser = if ([string]::IsNullOrWhiteSpace($env:FLOOD_DB_ADMIN_USER)) {
    'root'
} else {
    $env:FLOOD_DB_ADMIN_USER
}

if ($databasePort -notmatch '^\d{1,5}$' -or [int]$databasePort -gt 65535) {
    throw 'FLOOD_DB_PORT must be an integer between 1 and 65535.'
}

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$assertionPath = Resolve-Path (
    Join-Path $projectRoot 'sql\mysql\tests\assert_schema.sql'
)
$mysqlSourcePath = $assertionPath.Path.Replace('\', '/')
$previousMysqlPassword = $env:MYSQL_PWD

try {
    $env:MYSQL_PWD = $env:FLOOD_DB_ADMIN_PASSWORD
    $verificationOutput = & $mysqlPath `
        --protocol=TCP `
        --host=$databaseHost `
        --port=$databasePort `
        --user=$databaseUser `
        --connect-timeout=5 `
        --batch `
        --skip-column-names `
        --execute="source $mysqlSourcePath" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL schema verification failed: $($verificationOutput -join ' ')"
    }
} finally {
    if ($null -eq $previousMysqlPassword) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    } else {
        $env:MYSQL_PWD = $previousMysqlPassword
    }
}

$summaryLine = @($verificationOutput | Where-Object {
    $_ -match '^\S+\t\S+\tflood_scenario_deduction\t\d+$'
}) | Select-Object -Last 1

if ($null -eq $summaryLine) {
    throw 'MySQL schema verification returned an unexpected summary.'
}

$summaryParts = $summaryLine -split "`t"
Write-Output 'MySQL schema verification passed'
Write-Output "Server: $($summaryParts[0])"
Write-Output "User: $($summaryParts[1])"
Write-Output "Database: $($summaryParts[2])"
Write-Output "Tables: $($summaryParts[3])"
Write-Output 'Engine: InnoDB'
Write-Output 'Charset: utf8mb4'
