@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Regenera, a partir del codigo, las figuras de grafos:
rem   - TikZ para la monografia  -> ..\Maestria_PlantillaProyectoGrado\figuras\
rem   - SVG para la documentacion -> ejemplos\graficos\
rem  Las figuras del documento no se dibujan a mano: las produce
rem  el propio mecanismo, de modo que siempre coinciden con el
rem  grafo que realmente extrae.
rem ============================================================

set "CP=out\arquitecta-core;lib\*"
set "APP=co.javeriana.msw.arquitecta.ArquitectaApp"
set "TIKZ=..\Maestria_PlantillaProyectoGrado\figuras"
set "SVG=ejemplos\graficos"

if not exist "out\arquitecta-core" (
    echo [ERROR] Ejecuta build.bat antes de generar las figuras.
    exit /b 1
)
if not exist "%TIKZ%" mkdir "%TIKZ%"
if not exist "%SVG%" mkdir "%SVG%"

call :figura out\caso-estudio                rules.properties                             C0-base       c0-base
call :figura out\caso-estudio-c2-ciclo       rules.properties                             C2-ciclo      c2-ciclo
call :figura out\caso-estudio-c4-combinado   rules.properties                             C4-combinado  c4-combinado
call :figura lib\archunit-1.3.0.jar          ejemplos\archunit\rules-archunit.properties  ArchUnit-1.3.0 archunit

echo.
echo Figuras TikZ en: %TIKZ%
echo Figuras SVG  en: %SVG%   (se abren en cualquier navegador)
exit /b 0

:figura
echo   - %4
java -cp "%CP%" %APP% %1 %2 --etiqueta %3 --tikz "%TIKZ%\%4.tikz" --svg "%SVG%\%4.svg" > nul 2>&1
if errorlevel 1 (
    echo [ERROR] No se pudo generar la figura %4.
    exit /b 1
)
exit /b 0
