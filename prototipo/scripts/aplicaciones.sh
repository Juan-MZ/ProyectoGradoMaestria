#!/usr/bin/env bash
# ============================================================
#  Prepara las tres aplicaciones en capas de la validacion en
#  sistemas reales: las clona en la version fijada y las compila
#  en out/aplicaciones/<id>/target/classes. Usa Docker (imagen
#  oficial de Maven con JDK 17) para no depender de la version de
#  Java instalada; sin Docker, recurre al Maven Wrapper de cada
#  proyecto. Equivalente POSIX de aplicaciones.bat.
#
#  Uso habitual:  ./arquitecta.sh aplicaciones
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/aplicaciones

preparar() {
    local id="$1" repositorio="$2" commit="$3" dir="out/aplicaciones/$1"
    if [ -d "$dir/target/classes" ]; then
        echo "$id: ya compilada."
        return 0
    fi
    if [ ! -d "$dir/.git" ]; then
        echo "$id: clonando $repositorio..."
        git clone -q "https://github.com/$repositorio.git" "$dir"
    fi
    git -C "$dir" checkout -q "$commit"
    echo "$id: compilando (commit ${commit:0:7})..."
    if command -v docker > /dev/null 2>&1; then
        MSYS_NO_PATHCONV=1 docker run --rm -v "$(cd "$dir" && pwd)":/app -v arquitecta-m2:/root/.m2 -w /app \
            maven:3.9-eclipse-temurin-17 mvn -q -B -DskipTests compile
    else
        (cd "$dir" && ./mvnw -q -B -DskipTests compile)
    fi
}

preparar spring-framework-petclinic spring-petclinic/spring-framework-petclinic cf392920cdbe9cd07689f1f4b603271940bac957
preparar jpetstore-6 mybatis/jpetstore-6 c9a37f22e0afa06c11dfad26b1a8bf0e1a111d3e
preparar dddsample-core citerus/dddsample-core d8b896254ed4e18ba8117dcd5bf9a54708bdeaaf
