@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Ejecuta el experimento controlado completo: la linea base y
rem  los cuatro escenarios con derivas inducidas, bajo los dos
rem  modos de clasificacion. Escribe la evidencia (txt, json, csv
rem  y el grafo en svg, tikz y dot) en resultados\.
rem
rem  Uso habitual:  arquitecta experimento
rem ============================================================

cd /d "%~dp0.."

set "CP=out\arquitecta-core;lib\*"
set "APP=co.javeriana.msw.arquitecta.ArquitectaApp"
set "REGLAS=caso-estudio\rules.properties"

if not exist "out\arquitecta-core" (
    echo [ERROR] Compile antes del experimento: arquitecta compilar
    exit /b 1
)

if exist resultados rmdir /s /q resultados
mkdir resultados

echo.
echo === Catalogo de Fitness Functions =============================
java -cp "%CP%" %APP% --catalogo caso-estudio\c0-base %REGLAS% > resultados\catalogo-reglas.txt
type resultados\catalogo-reglas.txt

call :modo cr              resultados\modo-cr
call :modo cr+severidad    resultados\modo-cr-severidad

echo.
echo === Resumen comparativo (modo cr) =============================
type resultados\modo-cr\resumen.csv
echo.
echo === Resumen comparativo (modo cr+severidad) ===================
type resultados\modo-cr-severidad\resumen.csv
echo.
echo Evidencia completa en: resultados\
exit /b 0

:modo
call :escenario %1 %2 c0-base            C0-base
call :escenario %1 %2 c1-capas           C1-capas
call :escenario %1 %2 c2-ciclo           C2-ciclo
call :escenario %1 %2 c3-concentracion   C3-concentracion
call :escenario %1 %2 c4-combinado       C4-combinado
exit /b 0

:escenario
rem Se indica la carpeta del escenario, no su directorio de clases: el
rem mecanismo localiza target\classes igual que al elegirla en la interfaz.
echo.
echo --- [modo %1] %4 --------------------------------------------
java -cp "%CP%" %APP% caso-estudio\%3 %REGLAS% --etiqueta %4 --modo %1 --salida %2
exit /b 0
