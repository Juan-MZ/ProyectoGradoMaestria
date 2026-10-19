package co.javeriana.msw.arquitecta.interfaz;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Escenarios del caso de estudio que acompañan al prototipo, ofrecidos en la
 * pantalla de inicio para probar la herramienta sin tener un proyecto a mano.
 *
 * <p>Se buscan junto a la instalación ---subiendo desde el {@code .jar} o el
 * directorio de clases--- y, en su defecto, en el directorio de trabajo. Si el
 * {@code .jar} se copió a otro equipo sin el caso de estudio, simplemente no se
 * ofrecen.</p>
 */
final class Ejemplos {

    /** Un escenario: su nombre corto, qué deriva introduce y su carpeta. */
    record Caso(String nombre, String descripcion, Path carpeta) {
    }

    private static final String[][] CASOS = {
        {"c0-base", "C0 · Línea base", "La arquitectura de referencia, sin derivas."},
        {"c1-capas", "C1 · Capas", "La presentación accede directamente a la persistencia."},
        {"c2-ciclo", "C2 · Ciclo", "Dos paquetes del negocio se referencian mutuamente."},
        {"c3-concentracion", "C3 · Concentración", "Un paquete utilitario depende de todos los demás."},
        {"c4-combinado", "C4 · Combinado", "Las tres derivas a la vez."},
    };

    private Ejemplos() {
    }

    static List<Caso> disponibles() {
        List<Caso> casos = new ArrayList<>();
        carpetaCasoEstudio().ifPresent(raiz -> {
            for (String[] c : CASOS) {
                Path carpeta = raiz.resolve(c[0]);
                if (Files.isDirectory(carpeta)) {
                    casos.add(new Caso(c[1], c[2], carpeta));
                }
            }
        });
        return casos;
    }

    private static Optional<Path> carpetaCasoEstudio() {
        List<Path> origenes = new ArrayList<>();
        try {
            origenes.add(Path.of(Ejemplos.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            // Sin ubicación conocida queda el directorio de trabajo.
        }
        origenes.add(Path.of("").toAbsolutePath());
        for (Path origen : origenes) {
            Path actual = origen.toAbsolutePath().normalize();
            for (int nivel = 0; nivel < 4 && actual != null; nivel++, actual = actual.getParent()) {
                Path candidata = actual.resolve("caso-estudio");
                if (Files.isRegularFile(candidata.resolve("rules.properties"))) {
                    return Optional.of(candidata);
                }
            }
        }
        return Optional.empty();
    }
}
