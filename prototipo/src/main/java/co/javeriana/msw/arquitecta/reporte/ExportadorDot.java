package co.javeriana.msw.arquitecta.reporte;

import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Arista;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.EstadoNodo;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Fila;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Nodo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exporta el grafo de dependencias en formato DOT, para quien prefiera
 * procesarlo con Graphviz u otra herramienta compatible.
 *
 * <p>El mecanismo ya dibuja el grafo por sí mismo en SVG y TikZ; este formato se
 * conserva por interoperabilidad. Para que Graphviz no reordene las capas a su
 * criterio ---su algoritmo coloca los nodos según el sentido de las aristas, de
 * modo que una violación que remonta la estratificación podría invertirla---,
 * el archivo fija las filas calculadas por {@link DisposicionGrafo}, las encadena
 * con aristas invisibles y declara las dependencias ascendentes como aristas que
 * no restringen la posición.</p>
 */
public final class ExportadorDot {

    private ExportadorDot() {
    }

    public static String exportar(DisposicionGrafo d, String etiqueta) {
        Map<String, Nodo> nodoDe = d.nodos().stream()
                .collect(Collectors.toMap(Nodo::paquete, Function.identity()));

        StringBuilder sb = new StringBuilder();
        sb.append("digraph dependencias {\n");
        sb.append("  label=\"").append(escapar(etiqueta)).append("\";\n");
        sb.append("  labelloc=t;\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  newrank=true;\n");
        sb.append("  node [shape=box, style=rounded, fontname=\"Helvetica\"];\n");
        sb.append("  edge [fontname=\"Helvetica\", color=\"#7F93B5\"];\n\n");

        String capaActual = null;
        int cluster = 0;
        boolean abierto = false;
        for (Fila fila : d.filas()) {
            if (fila.capa() != null && !fila.capa().equals(capaActual)) {
                if (abierto) {
                    sb.append("  }\n");
                }
                sb.append("  subgraph cluster_").append(cluster++).append(" {\n");
                sb.append("    label=\"capa ").append(escapar(fila.capa())).append("\";\n");
                sb.append("    style=dashed;\n");
                abierto = true;
                capaActual = fila.capa();
            }
            String sangria = abierto ? "    " : "  ";
            sb.append(sangria).append("{ rank=same;");
            for (String paquete : fila.paquetes()) {
                sb.append(' ').append(nodo(nodoDe.get(paquete)));
            }
            sb.append(" }\n");
        }
        if (abierto) {
            sb.append("  }\n");
        }
        for (Nodo n : d.nodos()) {
            if (n.lateral()) {
                sb.append("  ").append(nodo(n)).append('\n');
            }
        }
        sb.append('\n');

        // Aristas invisibles que fijan el orden vertical de las filas.
        List<Fila> filas = d.filas();
        for (int i = 0; i + 1 < filas.size(); i++) {
            sb.append("  \"").append(escapar(filas.get(i).paquetes().get(0))).append("\" -> \"")
              .append(escapar(filas.get(i + 1).paquetes().get(0))).append("\" [style=invis, weight=10];\n");
        }

        for (Arista a : d.aristas()) {
            sb.append("  \"").append(escapar(a.origen().paquete())).append("\" -> \"")
              .append(escapar(a.destino().paquete())).append('"');
            StringBuilder atributos = new StringBuilder();
            switch (a.tipo()) {
                case VIOLA_CAPAS -> atributos.append("color=\"#C0392B\", penwidth=2.5, label=\"viola capas\"");
                case CICLO -> atributos.append("color=\"#C0392B\", style=dashed");
                case NUCLEO -> atributos.append("color=\"#C9CED6\"");
                case NORMAL -> { }
            }
            if (a.ascendente() || a.origen().lateral() || a.destino().lateral()) {
                if (!atributos.isEmpty()) {
                    atributos.append(", ");
                }
                atributos.append("constraint=false");
            }
            if (!atributos.isEmpty()) {
                sb.append(" [").append(atributos).append(']');
            }
            sb.append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String nodo(Nodo n) {
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(escapar(n.paquete())).append("\" [label=\"").append(escapar(n.etiqueta())).append('"');
        EstadoNodo estado = n.estado();
        switch (estado) {
            case CICLO, CICLO_Y_CONCENTRACION -> sb.append(", style=\"rounded,filled\", fillcolor=\"#FBE4E2\", color=\"#C0392B\"");
            case CONCENTRACION -> sb.append(", style=\"rounded,filled\", fillcolor=\"#FDF3DC\", color=\"#B7791F\"");
            case COMPARTIDO -> sb.append(", style=\"rounded,dashed\", color=\"#8A94A6\"");
            case NORMAL -> { }
        }
        return sb.append("];").toString();
    }

    private static String escapar(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
