@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Compila el prototipo Arquitecta, su bateria de pruebas y los
rem  cinco escenarios del experimento controlado.
rem  Requiere JDK 17+. No requiere Maven: las dependencias viven
rem  en lib\ y se referencian con un wildcard de classpath.
rem ============================================================

set "CORE_OUT=out\arquitecta-core"
set "TEST_OUT=out\arquitecta-test"

echo [1/3] Compilando nucleo de Arquitecta...
if not exist "%CORE_OUT%" mkdir "%CORE_OUT%"
dir /s /b src\main\java\*.java > _fuentes.txt
javac -encoding UTF-8 --release 17 -cp "lib\*" -d "%CORE_OUT%" @_fuentes.txt
if errorlevel 1 goto :error

echo [2/3] Compilando bateria de pruebas...
if not exist "%TEST_OUT%" mkdir "%TEST_OUT%"
dir /s /b src\test\java\*.java > _fuentes.txt
javac -encoding UTF-8 --release 17 -cp "%CORE_OUT%;lib\*" -d "%TEST_OUT%" @_fuentes.txt
if errorlevel 1 goto :error

echo [3/3] Compilando escenarios del experimento...
call :caso caso-estudio
if errorlevel 1 goto :error
call :caso caso-estudio-c1-capas
if errorlevel 1 goto :error
call :caso caso-estudio-c2-ciclo
if errorlevel 1 goto :error
call :caso caso-estudio-c3-concentracion
if errorlevel 1 goto :error
call :caso caso-estudio-c4-combinado
if errorlevel 1 goto :error

del /q _fuentes.txt 2>nul
echo.
echo Compilacion completada correctamente.
exit /b 0

:caso
echo       - %1
if not exist "out\%1" mkdir "out\%1"
dir /s /b %1\src\main\java\*.java > _fuentes.txt
javac -encoding UTF-8 --release 17 -d "out\%1" @_fuentes.txt
exit /b %errorlevel%

:error
echo.
echo [ERROR] La compilacion fallo. Revisa los mensajes anteriores.
del /q _fuentes.txt 2>nul
exit /b 1
