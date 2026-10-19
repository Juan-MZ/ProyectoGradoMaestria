@echo off
setlocal
rem ============================================================
rem  Abre la interfaz grafica de Arquitecta.
rem
rem    arquitecta.bat                 abre la ventana vacia
rem    arquitecta.bat <carpeta|jar>   la abre con ese proyecto
rem
rem  Si el prototipo aun no esta compilado, lo compila primero.
rem  Requiere JDK 17+.
rem ============================================================

cd /d "%~dp0"

if not exist "out\arquitecta-core\co\javeriana\msw\arquitecta\ArquitectaApp.class" (
    echo Compilando Arquitecta por primera vez...
    call "%~dp0build.bat"
    if errorlevel 1 (
        pause
        exit /b 1
    )
)

set "CP=%~dp0out\arquitecta-core;%~dp0lib\*"

rem javaw no deja una consola abierta detras de la ventana; si no esta
rem disponible (algunas distribuciones del JDK no lo incluyen) se usa java.
where javaw > nul 2> nul
if errorlevel 1 (set "JAVA=java") else (set "JAVA=javaw")

if "%~1"=="" (
    start "Arquitecta" %JAVA% -cp "%CP%" co.javeriana.msw.arquitecta.ArquitectaApp --gui
) else (
    start "Arquitecta" %JAVA% -cp "%CP%" co.javeriana.msw.arquitecta.ArquitectaApp --gui --proyecto "%~f1"
)
exit /b 0
