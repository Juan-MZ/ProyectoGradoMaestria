@echo off
setlocal
chcp 65001 > nul
rem ============================================================
rem  Prepara las tres aplicaciones en capas de la validacion en
rem  sistemas reales: las clona en la version fijada y las compila
rem  en out\aplicaciones\<id>\target\classes. Usa Docker (imagen
rem  oficial de Maven con JDK 17) para no depender de la version de
rem  Java instalada; sin Docker, recurre al Maven Wrapper de cada
rem  proyecto.
rem
rem  Uso habitual:  arquitecta aplicaciones
rem ============================================================

cd /d "%~dp0.."
if not exist "out\aplicaciones" mkdir "out\aplicaciones"

call :preparar spring-framework-petclinic spring-petclinic/spring-framework-petclinic cf392920cdbe9cd07689f1f4b603271940bac957
if errorlevel 1 exit /b 1
call :preparar jpetstore-6 mybatis/jpetstore-6 c9a37f22e0afa06c11dfad26b1a8bf0e1a111d3e
if errorlevel 1 exit /b 1
call :preparar dddsample-core citerus/dddsample-core d8b896254ed4e18ba8117dcd5bf9a54708bdeaaf
exit /b %errorlevel%

:preparar
set "DIR=out\aplicaciones\%1"
if exist "%DIR%\target\classes" (
    echo %1: ya compilada.
    exit /b 0
)
if not exist "%DIR%\.git" (
    echo %1: clonando %2...
    git clone -q "https://github.com/%2.git" "%DIR%" || exit /b 1
)
git -C "%DIR%" checkout -q %3 || exit /b 1
echo %1: compilando...
where docker > nul 2> nul
if errorlevel 1 goto :sin_docker
docker run --rm -v "%CD%\%DIR%":/app -v arquitecta-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -q -B -DskipTests compile
exit /b %errorlevel%

:sin_docker
pushd "%DIR%"
call mvnw.cmd -q -B -DskipTests compile
set "CODIGO=%errorlevel%"
popd
exit /b %CODIGO%
