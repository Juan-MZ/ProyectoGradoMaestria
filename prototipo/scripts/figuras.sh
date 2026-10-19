#!/usr/bin/env bash
# ============================================================
#  Regenera, a partir del codigo, las figuras de grafos:
#   - TikZ para la monografia  -> ../Maestria_PlantillaProyectoGrado/figuras/
#   - SVG para la documentacion -> ejemplos/graficos/
#   - capturas de la interfaz grafica -> ejemplos/interfaz/ (y la
#     del grafo, tambien en la carpeta de la monografia)
#  Equivalente POSIX de figuras.bat.
#
#  Uso habitual:  ./arquitecta.sh figuras
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

CP="out/arquitecta-core:lib/*"
APP=co.javeriana.msw.arquitecta.ArquitectaApp
TIKZ=../Maestria_PlantillaProyectoGrado/figuras
SVG=ejemplos/graficos
GUI=ejemplos/interfaz
REGLAS=caso-estudio/rules.properties

[ -d out/arquitecta-core ] || { echo "[ERROR] Compile antes de generar las figuras: ./arquitecta.sh compilar"; exit 1; }
mkdir -p "$TIKZ" "$SVG" "$GUI"

figura() {
    echo "  - $4"
    java -cp "$CP" "$APP" "$1" "$2" --etiqueta "$3" --tikz "$TIKZ/$4.tikz" --svg "$SVG/$4.svg" > /dev/null 2>&1
}

figura caso-estudio/c0-base        "$REGLAS"                                   C0-base        c0-base
figura caso-estudio/c2-ciclo       "$REGLAS"                                   C2-ciclo       c2-ciclo
figura caso-estudio/c4-combinado   "$REGLAS"                                   C4-combinado   c4-combinado
figura lib/archunit-1.3.0.jar      ejemplos/archunit/rules-archunit.properties ArchUnit-1.3.0 archunit

# Las capturas dependen de las fuentes del sistema y muestran el tiempo de
# analisis, por lo que no son identicas byte a byte entre equipos; un fallo
# (por ejemplo, sin entorno grafico) no invalida las demas figuras.
echo "  - capturas de la interfaz grafica"
C4=(--proyecto caso-estudio/c4-combinado --reglas "$REGLAS")
if java -cp "$CP" "$APP" --gui "${C4[@]}" --seleccion negocio --captura "$GUI/interfaz-grafo.png" > /dev/null 2>&1; then
    java -cp "$CP" "$APP" --gui "${C4[@]}" --pestana hallazgos --captura "$GUI/interfaz-hallazgos.png" > /dev/null 2>&1 || true
    java -cp "$CP" "$APP" --gui --pestana inicio --captura "$GUI/interfaz-inicio.png" > /dev/null 2>&1 || true
    cp "$GUI/interfaz-grafo.png" "$TIKZ/interfaz-grafo.png"
else
    echo "    [aviso] No se pudieron generar las capturas: requieren entorno grafico."
fi

echo
echo "Figuras TikZ en: $TIKZ"
echo "Figuras SVG  en: $SVG   (se abren en cualquier navegador)"
echo "Capturas     en: $GUI"
