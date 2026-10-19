#!/usr/bin/env bash
# Ejecuta la bateria de pruebas del nucleo. Codigo de salida 1 si algo falla.
set -euo pipefail
cd "$(dirname "$0")"
exec java -cp "out/arquitecta-core:out/arquitecta-test:lib/*" \
    co.javeriana.msw.arquitecta.PruebasArquitecta
