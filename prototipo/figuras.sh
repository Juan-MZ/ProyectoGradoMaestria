#!/usr/bin/env bash
# ============================================================
#  Regenera, a partir del codigo, las figuras de grafos:
#   - TikZ para la monografia  -> ../Maestria_PlantillaProyectoGrado/figuras/
#   - SVG para la documentacion -> ejemplos/graficos/
#  Equivalente POSIX de figuras.bat.
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

CP="out/arquitecta-core:lib/*"
APP=co.javeriana.msw.arquitecta.ArquitectaApp
TIKZ=../Maestria_PlantillaProyectoGrado/figuras
SVG=ejemplos/graficos

[ -d out/arquitecta-core ] || { echo "[ERROR] Ejecuta ./build.sh antes de generar las figuras."; exit 1; }
mkdir -p "$TIKZ" "$SVG"

figura() {
    echo "  - $4"
    java -cp "$CP" "$APP" "$1" "$2" --etiqueta "$3" --tikz "$TIKZ/$4.tikz" --svg "$SVG/$4.svg" > /dev/null 2>&1
}

figura out/caso-estudio              rules.properties                            C0-base        c0-base
figura out/caso-estudio-c2-ciclo     rules.properties                            C2-ciclo       c2-ciclo
figura out/caso-estudio-c4-combinado rules.properties                            C4-combinado   c4-combinado
figura lib/archunit-1.3.0.jar        ejemplos/archunit/rules-archunit.properties ArchUnit-1.3.0 archunit

echo
echo "Figuras TikZ en: $TIKZ"
echo "Figuras SVG  en: $SVG   (se abren en cualquier navegador)"
