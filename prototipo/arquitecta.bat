@echo off
setlocal
rem ============================================================
rem  Arquitecta: punto de entrada unico.
rem
rem    arquitecta                      abre la aplicacion
rem    arquitecta <carpeta|jar>        la abre con ese proyecto
rem    arquitecta analizar <proyecto> [reglas] [opciones]
rem                                    analisis por consola
rem    arquitecta compilar | pruebas | experimento | figuras | sistemas
rem    arquitecta aplicaciones | estudios [umbral|evolucion|oraculo|archunit]
rem    arquitecta ayuda
rem
rem  Con doble clic abre la aplicacion. Compila por su cuenta la
rem  primera vez y cada vez que cambia el codigo. Requiere JDK 17+.
rem ============================================================

set "RAIZ=%~dp0"
set "JAR=%RAIZ%out\arquitecta.jar"

if /i "%~1"=="ayuda"       goto :ayuda
if /i "%~1"=="-h"          goto :ayuda
if /i "%~1"=="--help"      goto :ayuda
if /i "%~1"=="--ayuda"     goto :ayuda
if /i "%~1"=="compilar"    goto :compilar
if /i "%~1"=="pruebas"     goto :guion
if /i "%~1"=="experimento" goto :guion
if /i "%~1"=="figuras"     goto :guion
if /i "%~1"=="sistemas"    goto :guion
if /i "%~1"=="aplicaciones" goto :guion
if /i "%~1"=="estudios"    goto :guion
if /i "%~1"=="preparar"    goto :actualizar

call :actualizar
if errorlevel 1 (
    if "%~1"=="" pause
    exit /b 1
)

if "%~1"=="" goto :interfaz
if /i "%~1"=="analizar" goto :analizar
if exist "%~1" goto :interfaz_con_proyecto

rem Cualquier otra entrada (por ejemplo, --catalogo) se entrega tal cual
rem a la linea de comandos del mecanismo.
java -jar "%JAR%" %*
exit /b %errorlevel%

rem ------------------------------------------------------------ interfaz
:interfaz
call :javaw
start "Arquitecta" %JAVAW% -jar "%JAR%"
exit /b 0

:interfaz_con_proyecto
call :javaw
start "Arquitecta" %JAVAW% -jar "%JAR%" --gui --proyecto "%~f1"
exit /b 0

rem ------------------------------------------------------------ consola
:analizar
shift
if "%~1"=="" (
    echo Uso: arquitecta analizar ^<carpeta-del-proyecto ^| clases ^| archivo.jar^> [reglas.properties] [opciones]
    exit /b 1
)
set "ARGUMENTOS="
:argumento
if "%~1"=="" goto :ejecutar
set ARGUMENTOS=%ARGUMENTOS% "%~1"
shift
goto :argumento
:ejecutar
java -jar "%JAR%" %ARGUMENTOS%
exit /b %errorlevel%

rem ------------------------------------------------------------ guiones
:compilar
pushd "%CD%"
call "%RAIZ%scripts\compilar.bat"
set "CODIGO=%errorlevel%"
popd
exit /b %CODIGO%

:guion
call :actualizar
if errorlevel 1 exit /b 1
pushd "%CD%"
call "%RAIZ%scripts\%~1.bat" %2
set "CODIGO=%errorlevel%"
popd
exit /b %CODIGO%

rem ------------------------------------------------------------ apoyo
:actualizar
rem Recompila si falta el .jar o si alguna fuente es posterior a la ultima
rem compilacion. Si PowerShell no estuviera disponible, se recompila.
if not exist "%JAR%" goto :recompilar
if not exist "%RAIZ%out\.compilado" goto :recompilar
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$r='%RAIZ%'; $m=Get-Item -LiteralPath ($r+'out\.compilado'); $f=Get-ChildItem -LiteralPath ($r+'src'),($r+'caso-estudio') -Recurse -File -Filter *.java | Where-Object { $_.LastWriteTime -gt $m.LastWriteTime } | Select-Object -First 1; if ($f) { exit 1 } else { exit 0 }" > nul 2> nul
if errorlevel 1 goto :recompilar
exit /b 0
:recompilar
echo Compilando Arquitecta (solo la primera vez o tras cambiar el codigo)...
pushd "%CD%"
call "%RAIZ%scripts\compilar.bat"
set "CODIGO=%errorlevel%"
popd
if not "%CODIGO%"=="0" (
    echo.
    echo [ERROR] No se pudo compilar Arquitecta. Verifique que haya un JDK 17 o superior instalado.
    exit /b 1
)
exit /b 0

:javaw
rem javaw no deja una consola abierta detras de la ventana; si no esta
rem disponible (algunas distribuciones del JDK no lo incluyen) se usa java.
where javaw > nul 2> nul
if errorlevel 1 (set "JAVAW=java") else (set "JAVAW=javaw")
exit /b 0

:ayuda
echo.
echo  Arquitecta - deteccion de deriva arquitectonica
echo.
echo    arquitecta                       abre la aplicacion
echo    arquitecta ^<carpeta^|jar^>         la abre con ese proyecto
echo    arquitecta analizar ^<proyecto^> [reglas.properties] [opciones]
echo                                     analisis por consola; opciones:
echo                                     --salida ^<dir^>  --svg ^<archivo^>  --modo cr^|cr+severidad
echo                                     --etiqueta ^<nombre^>  --fallar-si ^<estado^>
echo    arquitecta compilar              compila el prototipo y el caso de estudio
echo    arquitecta preparar              compila solo si el codigo cambio
echo    arquitecta pruebas               ejecuta la bateria de pruebas
echo    arquitecta experimento           reproduce el experimento (resultados\)
echo    arquitecta figuras               regenera figuras y capturas
echo    arquitecta sistemas              inyecta derivas en sistemas reales y mide su deteccion
echo    arquitecta aplicaciones          clona y compila las 3 aplicaciones en capas de la validacion
echo    arquitecta estudios [modo]       umbral, evolucion, oraculo JDepend y equivalente ArchUnit (ejemplos\estudios\)
echo.
echo  El proyecto puede indicarse por su carpeta raiz (Maven, Gradle, IntelliJ,
echo  Eclipse), por su directorio de clases o por su archivo .jar.
exit /b 0
