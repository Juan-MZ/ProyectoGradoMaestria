#!/usr/bin/env bash
# ============================================================
#  Ejecuta la bateria de pruebas del mecanismo. Devuelve codigo
#  de salida 1 si alguna comprobacion falla.
#  Equivalente POSIX de pruebas.bat.
#
#  Uso habitual:  ./arquitecta.sh pruebas
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -d out/arquitecta-test ]; then
    echo "[ERROR] Compile antes de ejecutar las pruebas: ./arquitecta.sh compilar"
    exit 1
fi

java -cp "out/arquitecta-core:out/arquitecta-test:lib/*" co.javeriana.msw.arquitecta.PruebasArquitecta
