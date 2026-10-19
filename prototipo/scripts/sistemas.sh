#!/usr/bin/env bash
# ============================================================
#  Validacion de aplicabilidad en entorno controlado: inyecta
#  derivas conocidas en cuatro sistemas reales de codigo abierto
#  y mide si el mecanismo las detecta. Descarga los .jar desde
#  Maven Central (verificando su SHA-1) la primera vez.
#  Equivalente POSIX de sistemas.bat.
#
#  Uso habitual:  ./arquitecta.sh sistemas
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

[ -d out/arquitecta-test ] || { echo "[ERROR] Compile antes del experimento: ./arquitecta.sh compilar"; exit 1; }
mkdir -p out/sistemas

descargar() {
    local ruta="$1" archivo="$2" sha1="$3" destino="out/sistemas/$2"
    if [ ! -f "$destino" ]; then
        echo "Descargando $archivo desde Maven Central..."
        curl -s -f -o "$destino" "https://repo1.maven.org/maven2/$ruta/$archivo"
    fi
    # El SHA-1 publicado por Maven Central garantiza que se analiza el artefacto oficial.
    local real
    real="$( (sha1sum "$destino" 2>/dev/null || shasum -a 1 "$destino") | cut -c1-40)"
    if [ "$real" != "$sha1" ]; then
        echo "[ERROR] El SHA-1 de $archivo no coincide con el publicado."
        rm -f "$destino"
        exit 1
    fi
}

descargar org/apache/commons/commons-lang3/3.14.0 commons-lang3-3.14.0.jar 1ed471194b02f2c6cb734a0cd6f6f107c673afae
descargar com/fasterxml/jackson/core/jackson-databind/2.17.2 jackson-databind-2.17.2.jar e6deb029e5901e027c129341fac39e515066b68c
descargar com/google/guava/guava/33.2.1-jre guava-33.2.1-jre.jar 818e780da2c66c63bbb6480fef1f3855eeafa3e4

java -cp "out/arquitecta-core:out/arquitecta-test:lib/*" co.javeriana.msw.arquitecta.sistemas.ExperimentoSistemas ejecutar
