package co.javeriana.msw.arquitecta.reporte;

import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Arista;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Banda;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.EstadoNodo;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Nodo;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.TipoArista;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Trayecto;

import java.util.Locale;

/**
 * Dibuja el grafo de dependencias como una imagen SVG autocontenida.
 *
 * <p>El formato se eligió por una razón práctica: cualquier navegador lo abre
 * sin instalar nada, de modo que el grafo puede consultarse en el mismo entorno
 * en que se ejecuta el mecanismo, sin depender de Graphviz ni de ninguna otra
 * herramienta externa. Al ser vectorial, además, admite ampliación sin pérdida y
 * puede incorporarse a documentos y páginas web.</p>
 *
 * <p>Cada paquete lleva un texto emergente con su nombre completo, su capa, sus
 * acoplamientos y su estado, de modo que el dibujo puede mantenerse compacto sin
 * perder información.</p>
 */
public final class ExportadorSvg {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final String FUENTE_CODIGO =
            "ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace";
    private static final String FUENTE_TEXTO =
            "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif";

    private ExportadorSvg() {
    }

    public static String exportar(DisposicionGrafo d, Resultado resultado, String titulo) {
        StringBuilder sb = new StringBuilder();
        String subtitulo = String.format(ES, "Tasa de Conformidad %.2f %% · %s · %d paquetes · %d dependencias",
                resultado.conformidad(), resultado.estado().etiqueta(), d.nodos().size(), d.aristas().size());

        // El lienzo se ensancha si el encabezado o la leyenda no caben en el grafo.
        double ancho = Math.max(d.ancho(), Math.max(anchoLeyenda(d), 48 + subtitulo.length() * 6.6));
        sb.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.0f\" height=\"%.0f\" "
                        + "viewBox=\"0 0 %.0f %.0f\" role=\"img\" aria-labelledby=\"titulo\">%n",
                ancho, d.alto(), ancho, d.alto()));
        sb.append("  <title id=\"titulo\">").append(xml(titulo)).append(" — ").append(xml(subtitulo))
          .append("</title>\n");
        sb.append(definiciones());
        sb.append(String.format(Locale.ROOT,
                "  <rect width=\"%.0f\" height=\"%.0f\" fill=\"#FFFFFF\"/>%n", ancho, d.alto()));

        // Encabezado
        sb.append(String.format(Locale.ROOT,
                "  <text x=\"24\" y=\"32\" font-family=\"%s\" font-size=\"17\" font-weight=\"600\" "
                        + "fill=\"#1F2D44\">%s</text>%n", FUENTE_TEXTO, xml(titulo)));
        sb.append(String.format(Locale.ROOT,
                "  <text x=\"24\" y=\"54\" font-family=\"%s\" font-size=\"12.5\" fill=\"%s\">%s</text>%n",
                FUENTE_TEXTO, colorEstado(resultado), xml(subtitulo)));

        // Bandas de capa
        int indice = 0;
        for (Banda banda : d.bandas()) {
            if (banda.capa() == null) {
                continue;
            }
            String relleno = indice++ % 2 == 0 ? "#F3F6FB" : "#E9EEF6";
            sb.append(String.format(Locale.ROOT,
                    "  <rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"6\" fill=\"%s\"/>%n",
                    d.xBandas(), banda.y(), d.anchoBandas(), banda.alto(), relleno));
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"11\" font-weight=\"600\" "
                            + "letter-spacing=\"0.6\" fill=\"#3B5C8F\">CAPA %s</text>%n",
                    d.xBandas() + 10, banda.y() + 17, FUENTE_TEXTO, xml(banda.capa().toUpperCase(ES))));
        }
        if (d.tieneColumnaLateral()) {
            double yMin = Double.MAX_VALUE;
            double yMax = 0;
            for (Nodo n : d.nodos()) {
                if (n.lateral()) {
                    yMin = Math.min(yMin, n.y());
                    yMax = Math.max(yMax, n.abajo());
                }
            }
            double yPanel = Math.min(yMin - 30, d.bandas().get(0).y());
            sb.append(String.format(Locale.ROOT,
                    "  <rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"6\" fill=\"none\" "
                            + "stroke=\"#C5CEDC\" stroke-dasharray=\"5 4\"/>%n",
                    d.xColumna(), yPanel, d.anchoColumna(), yMax + 16 - yPanel));
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"11\" font-weight=\"600\" "
                            + "letter-spacing=\"0.6\" fill=\"#6B7A90\">SIN CAPA</text>%n",
                    d.xColumna() + 10, yPanel + 17, FUENTE_TEXTO));
        }

        // Aristas (antes que los nodos, para que estos queden encima)
        sb.append("  <g fill=\"none\">\n");
        for (Arista a : d.aristas()) {
            Trayecto t = a.trayecto();
            sb.append(String.format(Locale.ROOT,
                    "    <path d=\"M%.1f,%.1f C%.1f,%.1f %.1f,%.1f %.1f,%.1f\" %s>"
                            + "<title>%s → %s%s</title></path>%n",
                    t.x0(), t.y0(), t.cx1(), t.cy1(), t.cx2(), t.cy2(), t.x1(), t.y1(),
                    estiloArista(a.tipo()),
                    xml(a.origen().paquete()), xml(a.destino().paquete()), xml(descripcion(a.tipo()))));
        }
        sb.append("  </g>\n");

        // Nodos
        for (Nodo n : d.nodos()) {
            sb.append("  <g>\n");
            sb.append("    <title>").append(xml(tooltip(n))).append("</title>\n");
            sb.append(String.format(Locale.ROOT,
                    "    <rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"6\" %s/>%n",
                    n.x(), n.y(), n.ancho(), DisposicionGrafo.ALTO_NODO, estiloNodo(n.estado())));
            sb.append(String.format(Locale.ROOT,
                    "    <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" dominant-baseline=\"central\" "
                            + "font-family=\"%s\" font-size=\"12\" fill=\"#1F2D44\">%s</text>%n",
                    n.cx(), n.cy(), FUENTE_CODIGO, xml(n.etiqueta())));
            sb.append("  </g>\n");
        }

        sb.append(leyenda(d));
        sb.append("</svg>\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------ estilos

    private static String definiciones() {
        StringBuilder sb = new StringBuilder("  <defs>\n");
        for (TipoArista tipo : TipoArista.values()) {
            sb.append(String.format(Locale.ROOT,
                    "    <marker id=\"punta-%s\" viewBox=\"0 0 10 10\" refX=\"9\" refY=\"5\" "
                            + "markerWidth=\"7\" markerHeight=\"7\" markerUnits=\"userSpaceOnUse\" "
                            + "orient=\"auto-start-reverse\">"
                            + "<path d=\"M0,0 L10,5 L0,10 z\" fill=\"%s\"/></marker>%n",
                    tipo.name().toLowerCase(Locale.ROOT), colorArista(tipo)));
        }
        return sb.append("  </defs>\n").toString();
    }

    static String colorArista(TipoArista tipo) {
        return switch (tipo) {
            case NUCLEO -> "#C9CED6";
            case NORMAL -> "#7F93B5";
            case CICLO -> "#C0392B";
            case VIOLA_CAPAS -> "#C0392B";
        };
    }

    private static String estiloArista(TipoArista tipo) {
        String base = String.format(Locale.ROOT, "stroke=\"%s\" marker-end=\"url(#punta-%s)\"",
                colorArista(tipo), tipo.name().toLowerCase(Locale.ROOT));
        return switch (tipo) {
            case NUCLEO -> base + " stroke-width=\"1\"";
            case NORMAL -> base + " stroke-width=\"1.3\"";
            case CICLO -> base + " stroke-width=\"1.7\" stroke-dasharray=\"6 4\"";
            case VIOLA_CAPAS -> base + " stroke-width=\"2.6\"";
        };
    }

    private static String estiloNodo(EstadoNodo estado) {
        return switch (estado) {
            case NORMAL -> "fill=\"#FFFFFF\" stroke=\"#3B5C8F\" stroke-width=\"1.3\"";
            case CICLO -> "fill=\"#FBE4E2\" stroke=\"#C0392B\" stroke-width=\"1.8\"";
            case CONCENTRACION -> "fill=\"#FDF3DC\" stroke=\"#B7791F\" stroke-width=\"1.8\"";
            case CICLO_Y_CONCENTRACION -> "fill=\"#FBE4E2\" stroke=\"#B7791F\" stroke-width=\"2.6\"";
            case COMPARTIDO -> "fill=\"#F4F5F7\" stroke=\"#8A94A6\" stroke-width=\"1.3\" stroke-dasharray=\"4 3\"";
        };
    }

    private static String colorEstado(Resultado resultado) {
        return switch (resultado.estado()) {
            case ACEPTABLE -> "#2E7D4F";
            case EN_OBSERVACION -> "#9A6A12";
            case NO_CONFORME -> "#B03A2E";
        };
    }

    // ------------------------------------------------------------------ textos

    static String descripcion(TipoArista tipo) {
        return switch (tipo) {
            case NUCLEO -> " (hacia o desde un núcleo compartido)";
            case NORMAL -> "";
            case CICLO -> " (forma parte de un ciclo)";
            case VIOLA_CAPAS -> " (viola la integridad de capas)";
        };
    }

    static String nombreEstado(EstadoNodo estado) {
        return switch (estado) {
            case NORMAL -> "Paquete";
            case CICLO -> "En un ciclo";
            case CONCENTRACION -> "Concentración excesiva";
            case CICLO_Y_CONCENTRACION -> "Ciclo y concentración";
            case COMPARTIDO -> "Núcleo compartido (exento)";
        };
    }

    static String nombreTipo(TipoArista tipo) {
        return switch (tipo) {
            case NUCLEO -> "Hacia núcleo compartido";
            case NORMAL -> "Dependencia";
            case CICLO -> "Dependencia en ciclo";
            case VIOLA_CAPAS -> "Viola la integridad de capas";
        };
    }

    private static String tooltip(Nodo n) {
        return n.paquete()
                + "\nCapa: " + (n.capa() == null ? "sin capa" : n.capa())
                + "\nCe = " + n.ce() + " · Ca = " + n.ca()
                + "\nEstado: " + nombreEstado(n.estado());
    }

    /** Anchura que ocupa la leyenda, calculada con las mismas reglas con que se dibuja. */
    static double anchoLeyenda(DisposicionGrafo d) {
        double nodos = 24;
        for (EstadoNodo estado : EstadoNodo.values()) {
            if (d.estadosPresentes().contains(estado)) {
                nodos += 40 + nombreEstado(estado).length() * 6.4;
            }
        }
        double aristas = 24;
        for (TipoArista tipo : TipoArista.values()) {
            if (d.tiposPresentes().contains(tipo)) {
                aristas += 44 + nombreTipo(tipo).length() * 6.4;
            }
        }
        return Math.max(nodos, aristas) + 16;
    }

    private static String leyenda(DisposicionGrafo d) {
        StringBuilder sb = new StringBuilder("  <g font-family=\"" + FUENTE_TEXTO + "\" font-size=\"11.5\" fill=\"#3A4658\">\n");
        double x = 24;
        double y = d.yLeyenda();
        for (EstadoNodo estado : EstadoNodo.values()) {
            if (!d.estadosPresentes().contains(estado)) {
                continue;
            }
            String nombre = nombreEstado(estado);
            sb.append(String.format(Locale.ROOT,
                    "    <rect x=\"%.1f\" y=\"%.1f\" width=\"22\" height=\"14\" rx=\"3\" %s/>%n",
                    x, y - 11, estiloNodo(estado)));
            sb.append(String.format(Locale.ROOT, "    <text x=\"%.1f\" y=\"%.1f\">%s</text>%n",
                    x + 28, y, xml(nombre)));
            x += 40 + nombre.length() * 6.4;
        }
        x = 24;
        y += 22;
        for (TipoArista tipo : TipoArista.values()) {
            if (!d.tiposPresentes().contains(tipo)) {
                continue;
            }
            String nombre = nombreTipo(tipo);
            sb.append(String.format(Locale.ROOT,
                    "    <path d=\"M%.1f,%.1f L%.1f,%.1f\" fill=\"none\" %s/>%n",
                    x, y - 4, x + 24, y - 4, estiloArista(tipo)));
            sb.append(String.format(Locale.ROOT, "    <text x=\"%.1f\" y=\"%.1f\">%s</text>%n",
                    x + 32, y, xml(nombre)));
            x += 44 + nombre.length() * 6.4;
        }
        return sb.append("  </g>\n").toString();
    }

    static String xml(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
