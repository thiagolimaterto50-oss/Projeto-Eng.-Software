param([int]$Porta = 8081)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
# Usa o JDK local preparado nesta máquina, se JAVA_HOME não estiver configurado.
if (-not $env:JAVA_HOME) {
    $jdkLocal = Get-ChildItem (Join-Path $env:LOCALAPPDATA 'sgma-tools') -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdkLocal) { $env:JAVA_HOME = $jdkLocal.FullName }
}
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
    throw 'Configure JAVA_HOME com o caminho de um JDK 17 antes de continuar. Consulte o README.'
}
$env:DEBUG = ''
Write-Host "SGMA: abra http://localhost:$Porta após a inicialização. Use Ctrl+C para encerrar."
& .\gradlew.bat bootRun "--args=--spring.profiles.active=demo --server.port=$Porta"
exit $LASTEXITCODE
