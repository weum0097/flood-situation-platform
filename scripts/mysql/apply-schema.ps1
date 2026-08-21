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
$migrationDirectory = Join-Path $projectRoot 'sql\mysql'
$migrationPaths = @(
    Get-ChildItem -LiteralPath $migrationDirectory -Filter 'V*.sql' -File |
        Sort-Object -Property Name
)
if ($migrationPaths.Count -eq 0) {
    throw "No versioned MySQL migrations were found in $migrationDirectory."
}
$previousMysqlPassword = $env:MYSQL_PWD

try {
    $env:MYSQL_PWD = $env:FLOOD_DB_ADMIN_PASSWORD
    foreach ($migrationPath in $migrationPaths) {
        $mysqlSourcePath = $migrationPath.FullName.Replace('\', '/')
        & $mysqlPath `
            --protocol=TCP `
            --host=$databaseHost `
            --port=$databasePort `
            --user=$databaseUser `
            --connect-timeout=5 `
            --execute="source $mysqlSourcePath"
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL migration $($migrationPath.Name) failed with exit code $LASTEXITCODE."
        }
    }
} finally {
    if ($null -eq $previousMysqlPassword) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    } else {
        $env:MYSQL_PWD = $previousMysqlPassword
    }
}

Write-Output "MySQL schema migrations completed ($($migrationPaths.Count) files)."
