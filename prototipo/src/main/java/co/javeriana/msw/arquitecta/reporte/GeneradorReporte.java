package co.javeriana.msw.arquitecta.reporte;

import java.util.List;
import java.util.Locale;

import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.Regla;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;

/**
 * Genera el reporte textual de conformidad.
 *
 * <p>El reporte no se limita al veredicto: declara qué reglas se evaluaron, qué
 * umbrales se aplicaron, qué paquetes entraron en el análisis y qué quedó
 * deliberadamente fuera de él. Esa declaración de alcance es lo que impide que la
 * Tasa de Conformidad se lea como un juicio absoluto sobre la calidad del
 * sistema.</p>
 */
public final class GeneradorReporte {

    private static final int ANCHO = 78;
    private static final int MAX_EVIDENCIAS = 6;

    private GeneradorReporte() {
    }

    public static String generar(Resultado resultado, GrafoDependencias grafo,
            List<Regla> reglas, ConfiguracionReglas config, String etiqueta) {

        StringBuilder sb = new StringBuilder();
        String regla = "=".repeat(ANCHO);
        String sub = "-".repeat(ANCHO);

        sb.append(regla).append('\n');
        sb.append("  REPORTE DE CONFORMIDAD ARQUITECTÓNICA").append('\n');
        if (etiqueta != null && !etiqueta.isBlank()) {
            sb.append("  Escenario: ").append(etiqueta).append('\n');
        }
        sb.append(regla).append('\n');

        sb.append(fila("Paquetes analizados", Integer.toString(grafo.nodos().size())));
        sb.append(fila("Dependencias extraídas", Integer.toString(grafo.cantidadAristas())));
        sb.append(fila("Verificaciones", Integer.toString(resultado.verificaciones())));
        sb.append(fila("Incumplimientos", Integer.toString(resultado.incumplimientos())));
        sb.append(fila("Tasa de Conformidad", porcentaje(resultado.conformidad())));
        sb.append(sub).append('\n');
        sb.append(fila("Estado según CR", resultado.estadoPorCR().etiqueta()));
        sb.append(fila("Severidad máxima",
                resultado.severidadMaxima() == null ? "sin hallazgos" : resultado.severidadMaxima().etiqueta()));
        sb.append(fila("Estado según severidad", resultado.estadoPorSeveridad().etiqueta()));
        sb.append(fila("ESTADO ARQUITECTÓNICO", resultado.estado().etiqueta()
                + "  (modo: " + resultado.modo().clave() + ")"));
        if (resultado.severidadAlteroElVeredicto()) {
            sb.append("  Nota: la CR por sí sola habría clasificado este sistema como '")
              .append(resultado.estadoPorCR().etiqueta())
              .append("'.\n");
        }
        sb.append(regla).append("\n\n");

        for (ResultadoRegla rr : resultado.porRegla()) {
            sb.append("— ").append(rr.regla())
              .append("  [").append(rr.cumplidas()).append('/').append(rr.verificaciones())
              .append(" verificaciones superadas · ").append(porcentaje(rr.conformidadLocal())).append("]\n");
            if (rr.violaciones().isEmpty()) {
                sb.append("    Sin hallazgos.\n");
            } else {
                for (Violacion v : rr.violaciones()) {
                    sb.append("    [").append(v.severidad().etiqueta().toUpperCase(Locale.ROOT)).append("] ")
                      .append(v.descripcion()).append('\n');
                    sb.append("        Ubicación: ").append(String.join(", ", v.ubicacion())).append('\n');
                    int mostradas = 0;
                    for (String evidencia : v.evidencia()) {
                        if (mostradas++ == MAX_EVIDENCIAS) {
                            sb.append("        · (")
                              .append(v.evidencia().size() - MAX_EVIDENCIAS)
                              .append(" evidencias adicionales en el reporte JSON)\n");
                            break;
                        }
                        sb.append("        · ").append(evidencia).append('\n');
                    }
                }
            }
            sb.append('\n');
        }

        sb.append(alcance(resultado, grafo, reglas, config));
        sb.append(regla).append('\n');
        return sb.toString();
    }

    /**
     * Declaración de alcance del diagnóstico: qué se evaluó, con qué parámetros y
     * qué quedó fuera. Sin esta sección el reporte invitaría a extrapolar el
     * veredicto más allá de lo que las reglas realmente comprobaron.
     */
    private static String alcance(Resultado resultado, GrafoDependencias grafo,
            List<Regla> reglas, ConfiguracionReglas config) {

        StringBuilder sb = new StringBuilder();
        sb.append("-".repeat(ANCHO)).append('\n');
        sb.append("  ALCANCE DEL DIAGNÓSTICO\n");
        sb.append("-".repeat(ANCHO)).append('\n');
        sb.append("  Raíz de análisis      : ").append(config.raiz()).append('\n');
        sb.append("  Capas declaradas      : ").append(String.join(", ", config.nombresCapas())).append('\n');
        sb.append("  Umbral de concentración: Ce > ").append(config.umbralConcentracion()).append('\n');
        sb.append("  Umbrales de la CR     : aceptable ≥ ").append(porcentaje(resultado.umbralAceptable()))
          .append(" · en observación ≥ ").append(porcentaje(resultado.umbralObservacion())).append('\n');
        sb.append("  Reglas evaluadas      : ").append(reglas.size()).append('\n');
        for (Regla r : reglas) {
            sb.append("      · ").append(r.nombre())
              .append(" — verificación por: ").append(r.unidadVerificacion()).append('\n');
        }

        List<String> sinCapa = grafo.nodos().stream()
                .filter(p -> config.capaDe(p) == null)
                .toList();
        sb.append("  Paquetes sin capa     : ")
          .append(sinCapa.isEmpty() ? "ninguno" : String.join(", ", sinCapa))
          .append('\n');
        sb.append("  Fuera del diagnóstico : atributos de calidad distintos de la estructura de\n");
        sb.append("      dependencias (rendimiento, seguridad, corrección funcional), análisis\n");
        sb.append("      dinámico, dependencias externas al paquete raíz y toda deriva no\n");
        sb.append("      cubierta por las reglas listadas. Un veredicto 'Aceptable' significa\n");
        sb.append("      que no se hallaron incumplimientos de estas reglas, no que el sistema\n");
        sb.append("      carezca de problemas estructurales.\n");
        return sb.toString();
    }

    private static String fila(String etiqueta, String valor) {
        return String.format("  %-23s : %s%n", etiqueta, valor);
    }

    private static String porcentaje(double valor) {
        return String.format(Locale.ROOT, "%.2f%%", valor);
    }
}
