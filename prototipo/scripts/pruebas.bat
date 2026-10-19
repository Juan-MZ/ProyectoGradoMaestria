@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Ejecuta la bateria de pruebas del mecanismo. Devuelve codigo
rem  de salida 1 si alguna comprobacion falla.
rem
rem  Uso habitual:  arquitecta pruebas
rem ============================================================

cd /d "%~dp0.."

if not exist "out\arquitecta-test" (
    echo [ERROR] Compile antes de ejecutar las pruebas: arquitecta compilar
    exit /b 1
)

java -cp "out\arquitecta-core;out\arquitecta-test;lib\*" ^
     co.javeriana.msw.arquitecta.PruebasArquitecta
exit /b %errorlevel%
