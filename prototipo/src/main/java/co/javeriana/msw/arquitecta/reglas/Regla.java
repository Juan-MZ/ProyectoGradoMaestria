package co.javeriana.msw.arquitecta.reglas;

import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;

/**
 * Contrato de una <em>Fitness Function</em> arquitectónica.
 *
 * <p>Además de evaluar el grafo, cada regla se describe a sí misma: expone la
 * propiedad arquitectónica que protege, la unidad sobre la que realiza cada
 * verificación atómica, el criterio con el que declara un incumplimiento y la
 * severidad que asigna a sus hallazgos. Gracias a ello el catálogo de reglas no
 * vive únicamente en la documentación: el propio mecanismo puede imprimirlo y
 * mantenerlo sincronizado con la implementación.</p>
 */
public interface Regla {

    /** Nombre corto de la regla, usado como encabezado en el reporte. */
    String nombre();

    /** Propiedad arquitectónica que la regla preserva. */
    String propiedad();

    /** Racionalidad: por qué esa propiedad importa para la mantenibilidad. */
    String racionalidad();

    /** Unidad sobre la que se realiza cada verificación atómica. */
    String unidadVerificacion();

    /** Criterio explícito bajo el cual una verificación se declara incumplida. */
    String criterioIncumplimiento();

    /** Severidad asignada a los hallazgos de esta regla. */
    Violacion.Severidad severidad();

    /** Evalúa el grafo en una sola pasada y devuelve conteos y hallazgos coherentes entre sí. */
    ResultadoRegla evaluar(GrafoDependencias grafo);
}
