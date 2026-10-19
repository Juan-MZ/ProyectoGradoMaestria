@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Validacion de aplicabilidad en entorno controlado: inyecta
rem  derivas conocidas en cuatro sistemas reales de codigo abierto
rem  y mide si el mecanismo las detecta. Descarga los .jar desde
rem  Maven Central (verificando su SHA-1) la primera vez.
rem  La evidencia se escribe en ejemplos\sistemas\.
rem
rem  Uso habitual:  arquitecta sistemas
rem ============================================================

cd /d "%~dp0.."

if not exist "out\arquitecta-test" (
    echo [ERROR] Compile antes del experimento: arquitecta compilar
    exit /b 1
)
if not exist "out\sistemas" mkdir "out\sistemas"

call :descargar org/apache/commons/commons-lang3/3.14.0 commons-lang3-3.14.0.jar 1ed471194b02f2c6cb734a0cd6f6f107c673afae
if errorlevel 1 exit /b 1
call :descargar com/fasterxml/jackson/core/jackson-databind/2.17.2 jackson-databind-2.17.2.jar e6deb029e5901e027c129341fac39e515066b68c
if errorlevel 1 exit /b 1
call :descargar com/google/guava/guava/33.2.1-jre guava-33.2.1-jre.jar 818e780da2c66c63bbb6480fef1f3855eeafa3e4
if errorlevel 1 exit /b 1

java -cp "out\arquitecta-core;out\arquitecta-test;lib\*" co.javeriana.msw.arquitecta.sistemas.ExperimentoSistemas ejecutar
exit /b %errorlevel%

:descargar
set "DESTINO=out\sistemas\%2"
if not exist "%DESTINO%" (
    echo Descargando %2 desde Maven Central...
    curl -s -f -o "%DESTINO%" "https://repo1.maven.org/maven2/%1/%2"
    if errorlevel 1 (
        echo [ERROR] No se pudo descargar %2.
        exit /b 1
    )
)
rem El SHA-1 publicado por Maven Central garantiza que se analiza el artefacto oficial.
for /f "skip=1 tokens=*" %%h in ('certutil -hashfile "%DESTINO%" SHA1 ^| findstr /v "CertUtil"') do (
    if /i not "%%h"=="%3" (
        echo [ERROR] El SHA-1 de %2 no coincide con el publicado.
        del /q "%DESTINO%"
        exit /b 1
    )
    goto :verificado
)
:verificado
exit /b 0
