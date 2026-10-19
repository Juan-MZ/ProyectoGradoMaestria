#!/usr/bin/env bash
# ============================================================
#  Experimento controlado completo: linea base y cuatro
#  escenarios con derivas inducidas, bajo los dos modos de
#  clasificacion. La evidencia se escribe en resultados/.
#  Equivalente POSIX de experimento.bat.
#
#  Uso habitual:  ./arquitecta.sh experimento
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

CP="out/arquitecta-core:lib/*"
APP=co.javeriana.msw.arquitecta.ArquitectaApp
REGLAS=caso-estudio/rules.properties
ESCENARIOS=("c0-base:C0-base"
            "c1-capas:C1-capas"
            "c2-ciclo:C2-ciclo"
            "c3-concentracion:C3-concentracion"
            "c4-combinado:C4-combinado")

[ -d out/arquitecta-core ] || { echo "[ERROR] Compile antes del experimento: ./arquitecta.sh compilar"; exit 1; }

rm -rf resultados && mkdir -p resultados

echo "=== Catalogo de Fitness Functions ============================="
java -cp "$CP" "$APP" --catalogo caso-estudio/c0-base "$REGLAS" \
    | tee resultados/catalogo-reglas.txt

for modo in cr cr+severidad; do
    destino="resultados/modo-${modo/+/-}"
    for entrada in "${ESCENARIOS[@]}"; do
        caso="${entrada%%:*}"
        etiqueta="${entrada##*:}"
        echo
        echo "--- [modo $modo] $etiqueta ------------------------------------"
        # Se indica la carpeta del escenario, no su directorio de clases: el
        # mecanismo localiza target/classes igual que al elegirla en la interfaz.
        java -cp "$CP" "$APP" "caso-estudio/$caso" "$REGLAS" \
            --etiqueta "$etiqueta" --modo "$modo" --salida "$destino"
    done
done

echo
echo "=== Resumen comparativo ======================================="
cat resultados/modo-cr/resumen.csv
echo
cat resultados/modo-cr-severidad/resumen.csv
