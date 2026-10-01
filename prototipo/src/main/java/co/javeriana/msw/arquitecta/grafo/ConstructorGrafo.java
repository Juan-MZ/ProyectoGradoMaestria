package co.javeriana.msw.arquitecta.grafo;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import java.nio.file.Path;
import java.util.List;

/**
 * Construye el grafo de dependencias a partir del código compilado mediante
 * análisis estático (ArchUnit).
 *
 * <p>Cada clase aporta su paquete como nodo y sus dependencias salientes como
 * aristas, restringidas al paquete raíz del sistema para que las bibliotecas de
 * terceros no contaminen el diagnóstico. De cada dependencia se conserva la
 * descripción que produce el importador ---clase origen, clase destino y posición
 * en el archivo fuente---, que es la evidencia que después permite justificar cada
 * hallazgo.</p>
 *
 * <p>La restricción a la raíz no siempre basta. Algunas bibliotecas empaquetan sus
 * propias dependencias bajo su mismo prefijo, y entonces el código vendorizado
 * entra en el grafo aunque no forme parte del sistema evaluado. Peor aún: basta
 * con que una clase lo referencie para que el paquete aparezca como nodo, aunque
 * sus clases nunca se hayan importado. De ahí que la exclusión deba aplicarse
 * sobre las referencias y no solo sobre las clases analizadas.</p>
 */
public class ConstructorGrafo {

    private final String raiz;
    private final List<String> exclusiones;

    public ConstructorGrafo(String raiz) {
        this(raiz, List.of());
    }

    /**
     * @param exclusiones prefijos de paquete que quedan fuera del grafo, tanto
     *                    como origen como en calidad de destino de una dependencia.
     */
    public ConstructorGrafo(String raiz, List<String> exclusiones) {
        if (raiz == null || raiz.isBlank()) {
            throw new IllegalArgumentException("La raíz de análisis es obligatoria.");
        }
        this.raiz = raiz;
        this.exclusiones = List.copyOf(exclusiones);
    }

    public GrafoDependencias desdeDirectorio(Path directorioClases) {
        JavaClasses clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPath(directorioClases);
        return desdeClases(clases);
    }

    public GrafoDependencias desdeClases(JavaClasses clases) {
        GrafoDependencias grafo = new GrafoDependencias();
        for (JavaClass clase : clases) {
            String paquete = clase.getPackageName();
            if (!perteneceAlAnalisis(paquete)) {
                continue;
            }
            grafo.agregarNodo(paquete);
            for (Dependency dependencia : clase.getDirectDependenciesFromSelf()) {
                String destino = dependencia.getTargetClass().getPackageName();
                if (perteneceAlAnalisis(destino) && !destino.equals(paquete)) {
                    grafo.agregarArista(paquete, destino, dependencia.getDescription());
                }
            }
        }
        return grafo;
    }

    private boolean perteneceAlAnalisis(String paquete) {
        if (paquete == null || !(paquete.equals(raiz) || paquete.startsWith(raiz + "."))) {
            return false;
        }
        for (String excluido : exclusiones) {
            if (paquete.equals(excluido) || paquete.startsWith(excluido + ".")) {
                return false;
            }
        }
        return true;
    }
}
