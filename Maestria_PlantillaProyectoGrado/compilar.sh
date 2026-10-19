#!/usr/bin/env bash
# ============================================================
#  Compila el documento con TeX Live dentro de Docker.
#  Secuencia: pdflatex -> bibtex -> pdflatex -> pdflatex
#  (bibtex, no biber: el documento usa natbib + apalike)
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

IMAGEN=xucheng/texlive-full
run() { docker run --rm -v "$PWD":/workdir -w /workdir "$IMAGEN" "$@"; }

if ! docker image inspect "$IMAGEN" >/dev/null 2>&1; then
    echo "[1/5] Descargando la imagen $IMAGEN ..."
    docker pull "$IMAGEN"
fi

echo "[2/5] pdflatex (primera pasada)..."
run pdflatex -synctex=1 -interaction=nonstopmode -file-line-error main.tex
echo "[3/5] bibtex..."
run bibtex main
echo "[4/5] pdflatex (segunda pasada)..."
run pdflatex -synctex=1 -interaction=nonstopmode -file-line-error main.tex
echo "[5/5] pdflatex (tercera pasada: referencias cruzadas)..."
run pdflatex -synctex=1 -interaction=nonstopmode -file-line-error main.tex

echo
echo "Compilacion terminada: main.pdf"
