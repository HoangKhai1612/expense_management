# Runs the backend on the host JVM against the Dockerised PostgreSQL.
# Useful for fast iteration; the deployment path is `docker compose up -d --build`.
#
#   .\scripts\run-backend-local.ps1

param(
    [string]$Profile = "local"
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

# Load .env into the process environment.
$envFile = Join-Path $repoRoot '.env'
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $name, $value = $line -split '=', 2
            [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), 'Process')
        }
    }
} else {
    throw ".env not found. Copy .env.example to .env and fill it in first."
}

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = Join-Path $env:USERPROFILE 'tools\jdk-21.0.12.1+1'
}
$maven = Join-Path $env:USERPROFILE 'tools\apache-maven-3.9.9\bin\mvn.cmd'
if (-not (Test-Path $maven)) {
    throw "Maven not found. Set FINAI_MAVEN_HOME or install Maven 3.9."
}

$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_HOST = 'localhost'
$env:DB_PORT = if ($env:DB_HOST_PORT) { $env:DB_HOST_PORT } else { '5433' }
$env:SERVER_PORT = if ($env:BACKEND_HOST_PORT) { $env:BACKEND_HOST_PORT } else { '8081' }

Set-Location (Join-Path $repoRoot 'backend')

# Tee output to a log file so stack traces survive the background-process buffer.
$logDir = Join-Path $repoRoot 'logs'
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}
$logFile = Join-Path $logDir 'backend.log'
Write-Host "Logging backend output to $logFile"

& $maven -B spring-boot:run "-Dspring-boot.run.profiles=$Profile" 2>&1 |
    Tee-Object -FilePath $logFile
