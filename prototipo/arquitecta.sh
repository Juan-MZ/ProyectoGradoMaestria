#!/usr/bin/env bash
# ============================================================
#  Abre la interfaz grafica de Arquitecta. Equivalente POSIX de
#  arquitecta.bat.
#
#    ./arquitecta.sh                 abre la ventana vacia
#    ./arquitecta.sh <carpeta|jar>   la abre con ese proyecto
#
#  Si el prototipo aun no esta compilado, lo compila primero.
#  Requiere JDK 17+.
# ============================================================
set -euo pipefail

# La ruta del proyecto se resuelve antes de cambiar de directorio.
PROYECTO=""
if [ $# -gt 0 ]; then
    PROYECTO="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
fi
cd "$(dirname "$0")"

if [ ! -f out/arquitecta-core/co/javeriana/msw/arquitecta/ArquitectaApp.class ]; then
    echo "Compilando Arquitecta por primera vez..."
    bash build.sh
fi

CP="out/arquitecta-core:lib/*"
if [ -z "$PROYECTO" ]; then
    exec java -cp "$CP" co.javeriana.msw.arquitecta.ArquitectaApp --gui
else
    exec java -cp "$CP" co.javeriana.msw.arquitecta.ArquitectaApp --gui --proyecto "$PROYECTO"
fi
