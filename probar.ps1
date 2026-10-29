param([string]$Jdk = "", [switch]$SoloLocal, [string]$OrigenV1 = "")
$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    & (Join-Path $PSScriptRoot 'compilar.ps1') -Jdk $Jdk
    $compilador = if ($Jdk) { Join-Path $Jdk 'bin\javac.exe' } else { 'javac' }
    $ejecutor = if ($Jdk) { Join-Path $Jdk 'bin\java.exe' } else { 'java' }
    & $compilador -encoding UTF-8 --release 17 -Xlint:all -cp bin -d bin pruebas\PruebasIntegracion.java
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron compilar las pruebas.' }
    $opcionesPrueba = @('work\pruebas')
    if ($SoloLocal) { $opcionesPrueba += '--solo-local' }
    elseif ($OrigenV1) { $opcionesPrueba += $OrigenV1 }
    & $ejecutor '-Dfile.encoding=UTF-8' -cp bin librosdigitales.PruebasIntegracion @opcionesPrueba
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba. Revisar el mensaje anterior.' }
} finally { Pop-Location }
