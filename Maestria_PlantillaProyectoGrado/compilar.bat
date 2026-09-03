@echo off
setlocal
rem ============================================================
rem  Compila el documento con TeX Live dentro de Docker, sin
rem  necesidad de instalar una distribucion LaTeX en Windows.
rem
rem  Secuencia: pdflatex -> bibtex -> pdflatex -> pdflatex
rem  (bibtex, no biber: el documento usa natbib + apalike)
rem ============================================================

set "IMAGEN=xucheng/texlive-full"
set "DOCKER=docker run --rm -v "%CD%":/workdir -w /workdir %IMAGEN%"

docker image inspect %IMAGEN% >nul 2>&1
if errorlevel 1 (
    echo [1/5] Descargando la imagen %IMAGEN% ...
    docker pull %IMAGEN%
    if errorlevel 1 goto :error
)

echo [2/5] pdflatex ^(primera pasada^)...
%DOCKER% pdflatex -synctex=1 -interaction=nonstopmode -file-line-error main.tex
if errorlevel 1 goto :error

echo [3/5] bibtex...
%DOCKER% bibtex main
if errorlevel 1 goto :error

echo [4/5] pdflatex ^(segunda pasada^)...
%DOCKER% pdflatex -synctex=1 -interaction=nonstopmode -file-line-error main.tex
if errorlevel 1 goto :error

echo [5/5] pdflatex ^(tercera pasada: referencias cruzadas^)...
%DOCKER% pdflatex -synctex=1 -interaction=nonstopmode -file-line-error main.tex
if errorlevel 1 goto :error

echo.
echo Compilacion terminada: main.pdf
echo Revisa main.log si aparecieron advertencias.
exit /b 0

:error
echo.
echo [ERROR] La compilacion fallo. Revisa main.log.
exit /b 1
