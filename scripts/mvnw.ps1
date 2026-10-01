# Local development wrapper.
#
# This machine has no system JDK on PATH, so the backend is always built through
# this script, which pins JAVA_HOME to a JDK 21 toolchain. Adjust $JdkHome if the
# JDK is installed elsewhere. Docker-based builds (docker compose build backend)
# do not need this script at all.

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = 'Stop'

$JdkHome = if ($env:FINAI_JAVA_HOME) {
    $env:FINAI_JAVA_HOME
} else {
    Join-Path $env:USERPROFILE 'tools\jdk-21.0.12.1+1'
}

$MavenHome = if ($env:FINAI_MAVEN_HOME) {
    $env:FINAI_MAVEN_HOME
} else {
    Join-Path $env:USERPROFILE 'tools\apache-maven-3.9.9'
}

if (-not (Test-Path (Join-Path $JdkHome 'bin\java.exe'))) {
    throw "JDK 21 not found at $JdkHome. Set FINAI_JAVA_HOME to a JDK 21 installation."
}
if (-not (Test-Path (Join-Path $MavenHome 'bin\mvn.cmd'))) {
    throw "Maven not found at $MavenHome. Set FINAI_MAVEN_HOME to a Maven 3.9 installation."
}

$env:JAVA_HOME = $JdkHome
$env:Path = "$MavenHome\bin;$JdkHome\bin;$env:Path"

Push-Location (Join-Path $PSScriptRoot '..\backend')
try {
    & (Join-Path $MavenHome 'bin\mvn.cmd') @MavenArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
