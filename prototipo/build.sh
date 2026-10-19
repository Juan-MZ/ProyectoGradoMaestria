#!/usr/bin/env bash
# ============================================================
#  Compila el prototipo Arquitecta, su bateria de pruebas y los
#  cinco escenarios del experimento controlado. Requiere JDK 17+.
#  Equivalente POSIX de build.bat.
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

CORE_OUT=out/arquitecta-core
TEST_OUT=out/arquitecta-test
ESCENARIOS=(caso-estudio caso-estudio-c1-capas caso-estudio-c2-ciclo
            caso-estudio-c3-concentracion caso-estudio-c4-combinado)

echo "[1/3] Compilando nucleo de Arquitecta..."
mkdir -p "$CORE_OUT"
find src/main/java -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 --release 17 -cp "lib/*" -d "$CORE_OUT"

echo "[2/3] Compilando bateria de pruebas..."
mkdir -p "$TEST_OUT"
find src/test/java -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 --release 17 -cp "$CORE_OUT:lib/*" -d "$TEST_OUT"

echo "[3/3] Compilando escenarios del experimento..."
for escenario in "${ESCENARIOS[@]}"; do
    echo "      - $escenario"
    mkdir -p "out/$escenario"
    find "$escenario/src/main/java" -name '*.java' -print0 \
        | xargs -0 javac -encoding UTF-8 --release 17 -d "out/$escenario"
done

echo
echo "Compilacion completada correctamente."
