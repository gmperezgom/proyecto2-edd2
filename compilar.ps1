param([string]$Jdk = "")
$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    $compilador = if ($Jdk) { Join-Path $Jdk 'bin\javac.exe' } else { 'javac' }
    New-Item -ItemType Directory -Force -Path 'bin' | Out-Null
    $fuentesJava = @(Get-ChildItem -LiteralPath 'src\librosdigitales' -Filter '*.java' | ForEach-Object FullName)
    & $compilador -encoding UTF-8 --release 17 -Xlint:all -d bin @fuentesJava
    if ($LASTEXITCODE -ne 0) { throw 'La compilación falló.' }
    Write-Host 'Compilación correcta. Código fuente en src; clases compiladas en bin.'
} finally { Pop-Location }
