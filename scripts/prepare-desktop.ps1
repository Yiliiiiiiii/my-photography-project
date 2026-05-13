$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$RuntimeDir = Join-Path $Root "desktop-runtime"

Push-Location $Root
try {
    mvn clean package -DskipTests

    if (-not $env:JAVA_HOME) {
        throw "JAVA_HOME is not set. Set JAVA_HOME to a JDK 17+ path before building the desktop package."
    }

    $Jlink = Join-Path $env:JAVA_HOME "bin\jlink.exe"
    if (-not (Test-Path $Jlink)) {
        throw "jlink.exe was not found under JAVA_HOME: $env:JAVA_HOME"
    }

    if (Test-Path $RuntimeDir) {
        Remove-Item -LiteralPath $RuntimeDir -Recurse -Force
    }

    & $Jlink `
        --no-header-files `
        --no-man-pages `
        --strip-debug `
        --compress=2 `
        --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.security.jgss,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.unsupported `
        --output $RuntimeDir

    Write-Host "Desktop backend jar and Java runtime are ready."
}
finally {
    Pop-Location
}
