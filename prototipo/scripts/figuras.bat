@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Regenera, a partir del codigo, las figuras de grafos:
rem   - TikZ para la monografia  -> ..\Maestria_PlantillaProyectoGrado\figuras\
rem   - SVG para la documentacion -> ejemplos\graficos\
rem   - capturas de la interfaz grafica -> ejemplos\interfaz\ (y
rem     la del grafo, tambien en la carpeta de la monografia)
rem  Las figuras del documento no se dibujan a mano: las produce
rem  el propio mecanismo, de modo que siempre coinciden con el
rem  grafo que realmente extrae.
rem
rem  Uso habitual:  arquitecta figuras
rem ============================================================

cd /d "%~dp0.."

set "CP=out\arquitecta-core;lib\*"
set "APP=co.javeriana.msw.arquitecta.ArquitectaApp"
set "TIKZ=..\Maestria_PlantillaProyectoGrado\figuras"
set "SVG=ejemplos\graficos"
set "GUI=ejemplos\interfaz"
set "REGLAS=caso-estudio\rules.properties"

if not exist "out\arquitecta-core" (
    echo [ERROR] Compile antes de generar las figuras: arquitecta compilar
    exit /b 1
)
if not exist "%TIKZ%" mkdir "%TIKZ%"
if not exist "%SVG%" mkdir "%SVG%"
if not exist "%GUI%" mkdir "%GUI%"

call :figura caso-estudio\c0-base         %REGLAS%                                     C0-base        c0-base
call :figura caso-estudio\c2-ciclo        %REGLAS%                                     C2-ciclo       c2-ciclo
call :figura caso-estudio\c4-combinado    %REGLAS%                                     C4-combinado   c4-combinado
call :figura lib\archunit-1.3.0.jar       ejemplos\archunit\rules-archunit.properties  ArchUnit-1.3.0 archunit

rem Las capturas dependen de las fuentes del sistema y muestran el tiempo de
rem analisis, por lo que no son identicas byte a byte entre equipos; un fallo
rem (por ejemplo, sin entorno grafico) no invalida las demas figuras.
echo   - capturas de la interfaz grafica
set "C4=--proyecto caso-estudio\c4-combinado --reglas %REGLAS%"
java -cp "%CP%" %APP% --gui %C4% --seleccion negocio --captura "%GUI%\interfaz-grafo.png" > nul 2>&1
if errorlevel 1 (
    echo     [aviso] No se pudieron generar las capturas: requieren entorno grafico.
) else (
    java -cp "%CP%" %APP% --gui %C4% --pestana hallazgos --captura "%GUI%\interfaz-hallazgos.png" > nul 2>&1
    java -cp "%CP%" %APP% --gui --pestana inicio --captura "%GUI%\interfaz-inicio.png" > nul 2>&1
    copy /y "%GUI%\interfaz-grafo.png" "%TIKZ%\interfaz-grafo.png" > nul
)

echo.
echo Figuras TikZ en: %TIKZ%
echo Figuras SVG  en: %SVG%   (se abren en cualquier navegador)
echo Capturas     en: %GUI%
exit /b 0

:figura
echo   - %4
java -cp "%CP%" %APP% %1 %2 --etiqueta %3 --tikz "%TIKZ%\%4.tikz" --svg "%SVG%\%4.svg" > nul 2>&1
if errorlevel 1 (
    echo [ERROR] No se pudo generar la figura %4.
    exit /b 1
)
exit /b 0
