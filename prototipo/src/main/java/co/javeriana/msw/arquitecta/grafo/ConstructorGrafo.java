package co.javeriana.msw.arquitecta.grafo;

import java.nio.file.Path;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Construye el grafo de dependencias a partir del código compilado mediante
 * análisis estático (ArchUnit).
 *
 * <p>Cada clase aporta su paquete como nodo y sus dependencias salientes como
 * aristas, restringidas al paquete raíz del sistema para que las bibliotecas de
 * terceros no contaminen el diagnóstico. De cada dependencia se conserva la
 * descripción que produce el importador —clase origen, clase destino y posición
 * en el archivo fuente—, que es la evidencia que después permite justificar cada
 * hallazgo.</p>
 */
public class ConstructorGrafo {

    private final String raiz;

    public ConstructorGrafo(String raiz) {
        if (raiz == null || raiz.isBlank()) {
            throw new IllegalArgumentException("La raíz de análisis es obligatoria.");
        }
        this.raiz = raiz;
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
            if (!perteneceAlSistema(paquete)) {
                continue;
            }
            grafo.agregarNodo(paquete);
            for (Dependency dependencia : clase.getDirectDependenciesFromSelf()) {
                String destino = dependencia.getTargetClass().getPackageName();
                if (perteneceAlSistema(destino) && !destino.equals(paquete)) {
                    grafo.agregarArista(paquete, destino, dependencia.getDescription());
                }
            }
        }
        return grafo;
    }

    private boolean perteneceAlSistema(String paquete) {
        return paquete != null && paquete.startsWith(raiz);
    }
}
