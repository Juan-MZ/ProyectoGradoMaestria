package co.javeriana.msw.arquitecta.interfaz;

import java.awt.Frame;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Punto de extensión para un asistente que ayude a redactar el catálogo de
 * reglas de un proyecto.
 *
 * <p>La ventana principal no sabe cómo funciona el asistente ni de qué depende:
 * solo ofrece el acceso cuando alguien lo proporciona. Así la interfaz no
 * acumula dependencias por una función opcional, y es la raíz de composición la
 * que decide si la aplicación lo incluye.</p>
 */
@FunctionalInterface
public interface AsistenteCatalogo {

    /**
     * Abre el asistente.
     *
     * @param proyecto        proyecto seleccionado.
     * @param catalogoActual  catálogo en uso, o {@code null} si no hay ninguno.
     * @param usar            recibe la ruta del catálogo que la persona decida guardar.
     */
    void abrir(Frame ventana, Path proyecto, Path catalogoActual, Consumer<Path> usar);
}
