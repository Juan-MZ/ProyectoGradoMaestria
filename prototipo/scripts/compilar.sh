#!/usr/bin/env bash
# ============================================================
#  Compila Arquitecta: nucleo, bateria de pruebas, el .jar
#  ejecutable y los cinco escenarios del caso de estudio.
#  Requiere JDK 17+. Equivalente POSIX de compilar.bat.
#
#  Uso habitual:  ./arquitecta.sh compilar
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

CORE_OUT=out/arquitecta-core
TEST_OUT=out/arquitecta-test
JAR_TMP=out/jar
CASOS=(c0-base c1-capas c2-ciclo c3-concentracion c4-combinado)

# Se parte de cero para que una clase eliminada del codigo fuente no
# sobreviva en el directorio de salida.
rm -rf "$CORE_OUT" "$TEST_OUT" "$JAR_TMP"
mkdir -p "$CORE_OUT" "$TEST_OUT" "$JAR_TMP"

echo "[1/4] Compilando nucleo de Arquitecta..."
find src/main/java -name '*.java' > out/_fuentes.txt
javac -encoding UTF-8 --release 17 -cp "lib/*" -d "$CORE_OUT" @out/_fuentes.txt

echo "[2/4] Compilando bateria de pruebas..."
find src/test/java -name '*.java' > out/_fuentes.txt
javac -encoding UTF-8 --release 17 -cp "$CORE_OUT:lib/*" -d "$TEST_OUT" @out/_fuentes.txt

echo "[3/4] Empaquetando out/arquitecta.jar..."
# Un unico .jar con las dependencias dentro: basta con copiarlo a otro
# equipo para usar la herramienta. La herramienta jar se invoca a traves
# de java porque no todas las instalaciones la ponen en el PATH.
cp -R "$CORE_OUT"/. "$JAR_TMP"/
LIB="$(pwd)/lib"
(
    cd "$JAR_TMP"
    for j in "$LIB"/*.jar; do java -m jdk.jartool xf "$j"; done
    rm -f META-INF/MANIFEST.MF META-INF/*.SF META-INF/*.RSA META-INF/*.DSA module-info.class
    rm -rf META-INF/versions
)
rm -f out/arquitecta.jar
java -m jdk.jartool --create --file out/arquitecta.jar \
     --main-class co.javeriana.msw.arquitecta.ArquitectaApp -C "$JAR_TMP" .
rm -rf "$JAR_TMP"

echo "[4/4] Compilando escenarios del caso de estudio..."
# Cada escenario se compila como lo haria Maven, en target/classes, de
# modo que su carpeta puede elegirse tal cual desde la interfaz grafica.
for caso in "${CASOS[@]}"; do
    echo "      - $caso"
    destino="caso-estudio/$caso/target/classes"
    rm -rf "$destino" && mkdir -p "$destino"
    find "caso-estudio/$caso/src/main/java" -name '*.java' > out/_fuentes.txt
    javac -encoding UTF-8 --release 17 -d "$destino" @out/_fuentes.txt
done

rm -f out/_fuentes.txt
# Marca de compilacion: el lanzador la compara con las fuentes para
# recompilar solo cuando algo cambio.
echo compilado > out/.compilado
echo
echo "Compilacion completada correctamente."
