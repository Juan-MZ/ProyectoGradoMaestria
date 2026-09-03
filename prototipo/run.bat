@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Ejecuta el analisis de conformidad sobre la linea base y
rem  sobre el escenario combinado, usando la ruta real de
rem  extraccion (ArchUnit sobre bytecode).
rem ============================================================

set "CP=out\arquitecta-core;lib\*"

echo.
echo ================================================================
echo   C0 - Linea base (sin derivas inducidas)
echo ================================================================
java -cp "%CP%" co.javeriana.msw.arquitecta.ArquitectaApp out\caso-estudio rules.properties --etiqueta C0-base
if errorlevel 1 exit /b 1

echo.
echo ================================================================
echo   C4 - Escenario combinado (tres derivas inducidas)
echo ================================================================
java -cp "%CP%" co.javeriana.msw.arquitecta.ArquitectaApp out\caso-estudio-c4-combinado rules.properties --etiqueta C4-combinado
if errorlevel 1 exit /b 1

exit /b 0
