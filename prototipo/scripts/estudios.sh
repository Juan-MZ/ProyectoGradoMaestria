#!/usr/bin/env bash
# ============================================================
#  Estudios complementarios de la evaluacion (seccion 4.7):
#    umbral     calibracion del umbral de concentracion
#    evolucion  deriva real a lo largo de versiones publicadas
#    oraculo    contraste del grafo con JDepend
#    archunit   las reglas del caso de estudio escritas con ArchUnit
#  Sin argumento ejecuta los tres. Descarga de Maven Central los
#  .jar que falten y verifica su SHA-1. La evidencia se escribe en
#  ejemplos/estudios/. Equivalente POSIX de estudios.bat.
#
#  Uso habitual:  ./arquitecta.sh estudios [umbral|evolucion|oraculo|archunit]
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

[ -d out/arquitecta-test ] || { echo "[ERROR] Compile antes: ./arquitecta.sh compilar"; exit 1; }
java -cp "out/arquitecta-core:out/arquitecta-test:lib/*" \
    co.javeriana.msw.arquitecta.sistemas.EstudiosComplementarios "${1:-todos}"
