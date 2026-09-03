@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Ejecuta la bateria de pruebas del nucleo del mecanismo.
rem  Devuelve codigo de salida 1 si alguna comprobacion falla.
rem ============================================================

if not exist "out\arquitecta-test" (
    echo [ERROR] Ejecuta build.bat antes de las pruebas.
    exit /b 1
)

java -cp "out\arquitecta-core;out\arquitecta-test;lib\*" ^
     co.javeriana.msw.arquitecta.PruebasArquitecta
exit /b %errorlevel%
