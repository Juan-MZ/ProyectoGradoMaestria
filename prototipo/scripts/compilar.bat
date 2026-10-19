@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Compila Arquitecta: nucleo, bateria de pruebas, el .jar
rem  ejecutable y los cinco escenarios del caso de estudio.
rem  Requiere JDK 17+. No requiere Maven: las dependencias viven
rem  en lib\ y se referencian con un comodin de classpath.
rem
rem  Uso habitual:  arquitecta compilar
rem ============================================================

cd /d "%~dp0.."

set "CORE_OUT=out\arquitecta-core"
set "TEST_OUT=out\arquitecta-test"
set "JAR_TMP=out\jar"

rem Se parte de cero para que una clase eliminada del codigo fuente no
rem sobreviva en el directorio de salida.
if exist "%CORE_OUT%" rmdir /s /q "%CORE_OUT%"
if exist "%TEST_OUT%" rmdir /s /q "%TEST_OUT%"
if exist "%JAR_TMP%" rmdir /s /q "%JAR_TMP%"
mkdir "%CORE_OUT%" "%TEST_OUT%" "%JAR_TMP%"

echo [1/4] Compilando nucleo de Arquitecta...
dir /s /b src\main\java\*.java > out\_fuentes.txt
javac -encoding UTF-8 --release 17 -cp "lib\*" -d "%CORE_OUT%" @out\_fuentes.txt
if errorlevel 1 goto :error

echo [2/4] Compilando bateria de pruebas...
dir /s /b src\test\java\*.java > out\_fuentes.txt
javac -encoding UTF-8 --release 17 -cp "%CORE_OUT%;lib\*" -d "%TEST_OUT%" @out\_fuentes.txt
if errorlevel 1 goto :error

echo [3/4] Empaquetando out\arquitecta.jar...
rem Un unico .jar con las dependencias dentro: basta con copiarlo a otro
rem equipo para usar la herramienta. La herramienta jar se invoca a
rem traves de java porque no todas las instalaciones la ponen en el PATH.
xcopy /e /q /y "%CORE_OUT%\*" "%JAR_TMP%\" > nul
pushd "%JAR_TMP%"
for %%j in ("%~dp0..\lib\*.jar") do java -m jdk.jartool xf "%%~fj"
del /q META-INF\MANIFEST.MF META-INF\*.SF META-INF\*.RSA META-INF\*.DSA 2> nul
if exist module-info.class del /q module-info.class
if exist META-INF\versions rmdir /s /q META-INF\versions
popd
if exist out\arquitecta.jar del /q out\arquitecta.jar
java -m jdk.jartool --create --file out\arquitecta.jar ^
     --main-class co.javeriana.msw.arquitecta.ArquitectaApp -C "%JAR_TMP%" .
if errorlevel 1 goto :error
rmdir /s /q "%JAR_TMP%"

echo [4/4] Compilando escenarios del caso de estudio...
for %%c in (c0-base c1-capas c2-ciclo c3-concentracion c4-combinado) do (
    call :caso %%c
    if errorlevel 1 goto :error
)

del /q out\_fuentes.txt 2> nul
rem Marca de compilacion: el lanzador la compara con las fuentes para
rem recompilar solo cuando algo cambio.
echo compilado> out\.compilado
echo.
echo Compilacion completada correctamente.
exit /b 0

:caso
rem Cada escenario se compila como lo haria Maven, en target\classes, de
rem modo que su carpeta puede elegirse tal cual desde la interfaz grafica.
echo       - %1
set "DESTINO=caso-estudio\%1\target\classes"
if exist "%DESTINO%" rmdir /s /q "%DESTINO%"
mkdir "%DESTINO%"
dir /s /b caso-estudio\%1\src\main\java\*.java > out\_fuentes.txt
javac -encoding UTF-8 --release 17 -d "%DESTINO%" @out\_fuentes.txt
exit /b %errorlevel%

:error
echo.
echo [ERROR] La compilacion fallo. Revise los mensajes anteriores.
del /q out\_fuentes.txt 2> nul
exit /b 1
