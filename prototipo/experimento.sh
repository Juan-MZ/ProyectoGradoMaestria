#!/usr/bin/env bash
# ============================================================
#  Experimento controlado completo: linea base y cuatro
#  escenarios con derivas inducidas, bajo los dos modos de
#  clasificacion. La evidencia se escribe en resultados/.
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

CP="out/arquitecta-core:lib/*"
APP=co.javeriana.msw.arquitecta.ArquitectaApp
ESCENARIOS=("caso-estudio:C0-base"
            "caso-estudio-c1-capas:C1-capas"
            "caso-estudio-c2-ciclo:C2-ciclo"
            "caso-estudio-c3-concentracion:C3-concentracion"
            "caso-estudio-c4-combinado:C4-combinado")

rm -rf resultados && mkdir -p resultados

echo "=== Catalogo de Fitness Functions ============================="
java -cp "$CP" "$APP" --catalogo out/caso-estudio rules.properties \
    | tee resultados/catalogo-reglas.txt

for modo in cr cr+severidad; do
    destino="resultados/modo-${modo/+/-}"
    for entrada in "${ESCENARIOS[@]}"; do
        directorio="${entrada%%:*}"
        etiqueta="${entrada##*:}"
        echo
        echo "--- [modo $modo] $etiqueta ------------------------------------"
        java -cp "$CP" "$APP" "out/$directorio" rules.properties \
            --etiqueta "$etiqueta" --modo "$modo" --salida "$destino"
    done
done

echo
echo "=== Resumen comparativo ======================================="
cat resultados/modo-cr/resumen.csv
echo
cat resultados/modo-cr-severidad/resumen.csv
