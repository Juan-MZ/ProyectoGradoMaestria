package co.javeriana.msw.arquitecta.reporte;

import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Exporta el grafo de dependencias en formato DOT (Graphviz).
 *
 * <p>Es la respuesta al requisito de que la representación estructural sea
 * navegable y no solo interna: agrupa los paquetes por capa, resalta en rojo las
 * aristas que incumplen la integridad de capas y marca los paquetes atrapados en
 * un ciclo o señalados por concentración excesiva. El resultado es una figura
 * que un equipo puede leer sin conocer la implementación.</p>
 */
public final class ExportadorDot {

    private ExportadorDot() {
    }

    public static String exportar(GrafoDependencias grafo, ConfiguracionReglas config,
            Resultado resultado, String etiqueta) {

        Set<String> aristasInfractoras = new HashSet<>();
        Set<String> enCiclo = new HashSet<>();
        Set<String> concentrados = new HashSet<>();

        for (ResultadoRegla rr : resultado.porRegla()) {
            for (Violacion v : rr.violaciones()) {
                List<String> ubicacion = v.ubicacion();
                switch (v.severidad()) {
                    case CRITICA -> enCiclo.addAll(ubicacion);
                    default -> {
                        if (ubicacion.size() == 2) {
                            aristasInfractoras.add(ubicacion.get(0) + "->" + ubicacion.get(1));
                        } else {
                            concentrados.addAll(ubicacion);
                        }
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("digraph dependencias {\n");
        sb.append("  label=\"").append(escapar(etiqueta)).append("\";\n");
        sb.append("  labelloc=t;\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=box, style=rounded, fontname=\"Helvetica\"];\n");
        sb.append("  edge [fontname=\"Helvetica\"];\n\n");

        int cluster = 0;
        for (String capa : config.nombresCapas()) {
            List<String> paquetes = grafo.nodos().stream()
                    .filter(p -> capa.equals(config.capaDe(p)))
                    .toList();
            if (paquetes.isEmpty()) {
                continue;
            }
            sb.append("  subgraph cluster_").append(cluster++).append(" {\n");
            sb.append("    label=\"capa ").append(escapar(capa)).append("\";\n");
            sb.append("    style=dashed;\n");
            for (String p : paquetes) {
                sb.append("    ").append(nodo(p, enCiclo, concentrados)).append('\n');
            }
            sb.append("  }\n");
        }
        for (String p : grafo.nodos()) {
            if (config.capaDe(p) == null) {
                sb.append("  ").append(nodo(p, enCiclo, concentrados)).append('\n');
            }
        }
        sb.append('\n');

        for (String[] arista : grafo.aristas()) {
            boolean infractora = aristasInfractoras.contains(arista[0] + "->" + arista[1]);
            sb.append("  \"").append(escapar(arista[0])).append("\" -> \"")
              .append(escapar(arista[1])).append('"');
            if (infractora) {
                sb.append(" [color=red, penwidth=2, label=\"viola capas\"]");
            }
            sb.append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String nodo(String paquete, Set<String> enCiclo, Set<String> concentrados) {
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(escapar(paquete)).append('"');
        if (enCiclo.contains(paquete)) {
            sb.append(" [style=\"rounded,filled\", fillcolor=\"#ffd6d6\", color=red]");
        } else if (concentrados.contains(paquete)) {
            sb.append(" [style=\"rounded,filled\", fillcolor=\"#fff2cc\", color=\"#bf9000\"]");
        }
        return sb.append(';').toString();
    }

    private static String escapar(String valor) {
        return valor == null ? "" : valor.replace("\"", "\\\"");
    }
}
