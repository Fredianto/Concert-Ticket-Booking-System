$ErrorActionPreference = 'Stop'

# Default values (override via env var if needed)
$dbHost = $env:DB_HOST; if (-not $dbHost) { $dbHost = 'localhost' }
$dbPort = $env:DB_PORT; if (-not $dbPort) { $dbPort = '5432' }
$dbName = $env:DB_NAME; if (-not $dbName) { $dbName = 'postgres' }
$dbUser = $env:DB_USERNAME; if (-not $dbUser) { $dbUser = 'cinema' }
$dbPass = $env:DB_PASSWORD; if (-not $dbPass) { $dbPass = 'admin' }
$schema = $env:DB_SCHEMA; if (-not $schema) { $schema = 'ticketing' }

$schemaOut = 'database/schema/ticketing_schema.sql'
$seedOut = 'database/seed/ticketing_seed.sql'

New-Item -ItemType Directory -Force -Path 'database/schema' | Out-Null
New-Item -ItemType Directory -Force -Path 'database/seed' | Out-Null

$pgDump = Get-Command pg_dump -ErrorAction SilentlyContinue
if (-not $pgDump) {
    Write-Host 'pg_dump tidak ditemukan di PATH. Gunakan DBeaver Backup manual ke folder database/schema dan database/seed.' -ForegroundColor Yellow
    exit 1
}

$env:PGPASSWORD = $dbPass

# Schema only
& pg_dump -h $dbHost -p $dbPort -U $dbUser -d $dbName -n $schema --schema-only --no-owner --no-privileges --file $schemaOut
if ($LASTEXITCODE -ne 0) { throw 'Gagal export schema' }

# Data only (seed)
& pg_dump -h $dbHost -p $dbPort -U $dbUser -d $dbName -n $schema --data-only --inserts --column-inserts --file $seedOut
if ($LASTEXITCODE -ne 0) { throw 'Gagal export seed data' }

Write-Host "Export selesai:" -ForegroundColor Green
Write-Host "- $schemaOut"
Write-Host "- $seedOut"
