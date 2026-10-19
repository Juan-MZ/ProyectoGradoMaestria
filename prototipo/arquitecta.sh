#!/usr/bin/env bash
# ============================================================
#  Arquitecta: punto de entrada unico. Equivalente POSIX de
#  arquitecta.bat.
#
#    ./arquitecta.sh                      abre la aplicacion
#    ./arquitecta.sh <carpeta|jar>        la abre con ese proyecto
#    ./arquitecta.sh analizar <proyecto> [reglas] [opciones]
#                                         analisis por consola
#    ./arquitecta.sh compilar | pruebas | experimento | figuras | sistemas
#    ./arquitecta.sh ayuda
#
#  Compila por su cuenta la primera vez y cada vez que cambia el
#  codigo. Requiere JDK 17+.
# ============================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")" && pwd)"
JAR="$RAIZ/out/arquitecta.jar"

ayuda() {
    cat <<'TEXTO'

 Arquitecta - deteccion de deriva arquitectonica

   ./arquitecta.sh                      abre la aplicacion
   ./arquitecta.sh <carpeta|jar>        la abre con ese proyecto
   ./arquitecta.sh analizar <proyecto> [reglas.properties] [opciones]
                                        analisis por consola; opciones:
                                        --salida <dir>  --svg <archivo>  --modo cr|cr+severidad
                                        --etiqueta <nombre>  --fallar-si <estado>
   ./arquitecta.sh compilar             compila el prototipo y el caso de estudio
   ./arquitecta.sh preparar             compila solo si el codigo cambio
   ./arquitecta.sh pruebas              ejecuta la bateria de pruebas
   ./arquitecta.sh experimento          reproduce el experimento (resultados/)
   ./arquitecta.sh figuras              regenera figuras y capturas
   ./arquitecta.sh sistemas             inyecta derivas en 4 sistemas reales y mide su deteccion

 El proyecto puede indicarse por su carpeta raiz (Maven, Gradle, IntelliJ,
 Eclipse), por su directorio de clases o por su archivo .jar.
TEXTO
}

# Recompila si falta el .jar o si alguna fuente es posterior a la ultima
# compilacion.
actualizar() {
    local marca="$RAIZ/out/.compilado"
    if [ -f "$JAR" ] && [ -f "$marca" ] \
        && [ -z "$(find "$RAIZ/src" "$RAIZ/caso-estudio" -name '*.java' -newer "$marca" -print -quit)" ]; then
        return 0
    fi
    echo "Compilando Arquitecta (solo la primera vez o tras cambiar el codigo)..."
    bash "$RAIZ/scripts/compilar.sh" || {
        echo
        echo "[ERROR] No se pudo compilar Arquitecta. Verifique que haya un JDK 17 o superior instalado."
        exit 1
    }
}

case "${1:-}" in
    ayuda|-h|--help|--ayuda)
        ayuda ;;
    compilar)
        bash "$RAIZ/scripts/compilar.sh" ;;
    preparar)
        actualizar ;;
    pruebas|experimento|figuras|sistemas)
        actualizar
        bash "$RAIZ/scripts/$1.sh" ;;
    "")
        actualizar
        exec java -jar "$JAR" ;;
    analizar)
        shift
        if [ $# -eq 0 ]; then
            echo "Uso: ./arquitecta.sh analizar <carpeta-del-proyecto | clases | archivo.jar> [reglas.properties] [opciones]"
            exit 1
        fi
        actualizar
        exec java -jar "$JAR" "$@" ;;
    *)
        actualizar
        if [ -e "$1" ]; then
            exec java -jar "$JAR" --gui --proyecto "$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
        fi
        # Cualquier otra entrada (por ejemplo, --catalogo) se entrega tal cual
        # a la linea de comandos del mecanismo.
        exec java -jar "$JAR" "$@" ;;
esac
