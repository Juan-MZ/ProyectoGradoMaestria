@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Estudios complementarios de la evaluacion (seccion 4.7):
rem    umbral     calibracion del umbral de concentracion
rem    evolucion  deriva real a lo largo de versiones publicadas
rem    oraculo    contraste del grafo con JDepend
rem    archunit   las reglas del caso de estudio escritas con ArchUnit
rem  Sin argumento ejecuta los tres. Descarga de Maven Central los
rem  .jar que falten y verifica su SHA-1. La evidencia se escribe en
rem  ejemplos\estudios\.
rem
rem  Uso habitual:  arquitecta estudios [umbral^|evolucion^|oraculo^|archunit]
rem ============================================================

cd /d "%~dp0.."
if not exist "out\arquitecta-test" (
    echo [ERROR] Compile antes: arquitecta compilar
    exit /b 1
)
set "MODO=%~1"
if "%MODO%"=="" set "MODO=todos"
java -cp "out\arquitecta-core;out\arquitecta-test;lib\*" co.javeriana.msw.arquitecta.sistemas.EstudiosComplementarios %MODO%
exit /b %errorlevel%
