package co.javeriana.msw.arquitecta.reporte;

import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Arista;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Banda;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.EstadoNodo;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Nodo;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.TipoArista;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Trayecto;

import java.util.Locale;

/**
 * Dibuja el grafo de dependencias como una figura TikZ lista para incluirse en
 * un documento LaTeX.
 *
 * <p>La motivación es de rigor más que de comodidad. Una figura dibujada a mano
 * puede discrepar del grafo que el mecanismo realmente extrajo, y nada lo
 * delataría. Una figura generada por el propio mecanismo, en cambio, es por
 * construcción una representación fiel de su resultado, y puede regenerarse
 * cada vez que el sistema cambia. Comparte con el SVG la misma disposición, de
 * modo que ambos formatos muestran exactamente el mismo dibujo.</p>
 *
 * <p>El archivo resultante contiene un único entorno {@code tikzpicture} y
 * requiere los paquetes {@code tikz} y {@code xcolor} con la biblioteca
 * {@code arrows.meta}. No incluye título: en un documento, esa función la cumple
 * el pie de la figura.</p>
 */
public final class ExportadorTikz {

    /** Escala de las unidades de dibujo: una unidad equivale a 0,75 pt. */
    private static final double ESCALA = 0.75;

    private ExportadorTikz() {
    }

    public static String exportar(DisposicionGrafo d, String titulo) {
        StringBuilder sb = new StringBuilder();
        sb.append("% Figura generada por Arquitecta a partir del grafo extraido del codigo.\n");
        sb.append("% Escenario: ").append(comentario(titulo)).append('\n');
        sb.append("% No editar a mano: regenerar con el prototipo (figuras.bat / figuras.sh).\n");
        sb.append("% Requiere \\usepackage{tikz} y \\usetikzlibrary{arrows.meta}.\n");
        sb.append(String.format(Locale.ROOT,
                "\\begin{tikzpicture}[x=%.2fpt, y=-%.2fpt, line cap=round, line join=round]%n", ESCALA, ESCALA));
        sb.append(colores());

        int indice = 0;
        for (Banda banda : d.bandas()) {
            if (banda.capa() == null) {
                continue;
            }
            String relleno = indice++ % 2 == 0 ? "arqBandaA" : "arqBandaB";
            sb.append(String.format(Locale.ROOT, "\\fill[%s, rounded corners=4.5pt] (%s,%s) rectangle (%s,%s);%n",
                    relleno, n(d.xBandas()), n(banda.y()), n(d.xBandas() + d.anchoBandas()),
                    n(banda.y() + banda.alto())));
            sb.append(String.format(Locale.ROOT,
                    "\\node[anchor=west, inner sep=0, text=arqBorde, font=\\sffamily\\bfseries\\fontsize{7}{8}\\selectfont]"
                            + " at (%s,%s) {CAPA %s};%n",
                    n(d.xBandas() + 10), n(banda.y() + 13), latex(banda.capa().toUpperCase(Locale.ROOT))));
        }

        if (d.tieneColumnaLateral()) {
            double yMin = Double.MAX_VALUE;
            double yMax = 0;
            for (Nodo nodo : d.nodos()) {
                if (nodo.lateral()) {
                    yMin = Math.min(yMin, nodo.y());
                    yMax = Math.max(yMax, nodo.abajo());
                }
            }
            double yPanel = Math.min(yMin - 30, d.bandas().get(0).y());
            sb.append(String.format(Locale.ROOT,
                    "\\draw[arqPanel, dash pattern=on 3.5pt off 3pt, rounded corners=4.5pt] (%s,%s) rectangle (%s,%s);%n",
                    n(d.xColumna()), n(yPanel), n(d.xColumna() + d.anchoColumna()), n(yMax + 16)));
            sb.append(String.format(Locale.ROOT,
                    "\\node[anchor=west, inner sep=0, text=arqTenue, font=\\sffamily\\bfseries\\fontsize{7}{8}\\selectfont]"
                            + " at (%s,%s) {SIN CAPA};%n",
                    n(d.xColumna() + 10), n(yPanel + 13)));
        }

        for (Arista a : d.aristas()) {
            Trayecto t = a.trayecto();
            sb.append(String.format(Locale.ROOT, "\\draw[%s] (%s,%s) .. controls (%s,%s) and (%s,%s) .. (%s,%s);%n",
                    estiloArista(a.tipo()), n(t.x0()), n(t.y0()), n(t.cx1()), n(t.cy1()),
                    n(t.cx2()), n(t.cy2()), n(t.x1()), n(t.y1())));
        }

        for (Nodo nodo : d.nodos()) {
            sb.append(String.format(Locale.ROOT, "\\draw[%s, rounded corners=4.5pt] (%s,%s) rectangle (%s,%s);%n",
                    estiloNodo(nodo.estado()), n(nodo.x()), n(nodo.y()), n(nodo.derecha()), n(nodo.abajo())));
            sb.append(String.format(Locale.ROOT,
                    "\\node[inner sep=0, text=arqTexto, font=\\ttfamily\\fontsize{9}{10}\\selectfont] at (%s,%s) {%s};%n",
                    n(nodo.cx()), n(nodo.cy()), latex(nodo.etiqueta())));
        }

        sb.append(leyenda(d));
        sb.append("\\end{tikzpicture}\n");
        return sb.toString();
    }

    private static String colores() {
        return """
                \\definecolor{arqBorde}{HTML}{3B5C8F}
                \\definecolor{arqTexto}{HTML}{1F2D44}
                \\definecolor{arqTenue}{HTML}{6B7A90}
                \\definecolor{arqPanel}{HTML}{C5CEDC}
                \\definecolor{arqBandaA}{HTML}{F3F6FB}
                \\definecolor{arqBandaB}{HTML}{E9EEF6}
                \\definecolor{arqAlerta}{HTML}{C0392B}
                \\definecolor{arqFondoAlerta}{HTML}{FBE4E2}
                \\definecolor{arqAviso}{HTML}{B7791F}
                \\definecolor{arqFondoAviso}{HTML}{FDF3DC}
                \\definecolor{arqNucleo}{HTML}{8A94A6}
                \\definecolor{arqFondoNucleo}{HTML}{F4F5F7}
                \\definecolor{arqArista}{HTML}{7F93B5}
                \\definecolor{arqAristaNucleo}{HTML}{C9CED6}
                """;
    }

    private static String estiloArista(TipoArista tipo) {
        String punta = "-{Stealth[length=4.5pt, width=4pt]}";
        return switch (tipo) {
            case NUCLEO -> punta + ", draw=arqAristaNucleo, line width=0.75pt";
            case NORMAL -> punta + ", draw=arqArista, line width=0.95pt";
            case CICLO -> punta + ", draw=arqAlerta, line width=1.25pt, dash pattern=on 4.5pt off 3pt";
            case VIOLA_CAPAS -> punta + ", draw=arqAlerta, line width=1.95pt";
        };
    }

    private static String estiloNodo(EstadoNodo estado) {
        return switch (estado) {
            case NORMAL -> "fill=white, draw=arqBorde, line width=0.95pt";
            case CICLO -> "fill=arqFondoAlerta, draw=arqAlerta, line width=1.35pt";
            case CONCENTRACION -> "fill=arqFondoAviso, draw=arqAviso, line width=1.35pt";
            case CICLO_Y_CONCENTRACION -> "fill=arqFondoAlerta, draw=arqAviso, line width=1.95pt";
            case COMPARTIDO -> "fill=arqFondoNucleo, draw=arqNucleo, line width=0.95pt, dash pattern=on 3pt off 2.25pt";
        };
    }

    /**
     * Leyenda con los elementos presentes en la figura, distribuida en líneas que
     * no exceden la anchura del dibujo.
     */
    private static String leyenda(DisposicionGrafo d) {
        StringBuilder sb = new StringBuilder();
        String fuente = "font=\\sffamily\\fontsize{7.5}{9}\\selectfont";
        double limite = Math.max(d.ancho(), 360);
        double x = 24;
        double y = d.yLeyenda() - 4;
        for (EstadoNodo estado : EstadoNodo.values()) {
            if (!d.estadosPresentes().contains(estado)) {
                continue;
            }
            String nombre = ExportadorSvg.nombreEstado(estado);
            double ancho = 40 + nombre.length() * 5.4;
            if (x + ancho > limite && x > 24) {
                x = 24;
                y += 20;
            }
            sb.append(String.format(Locale.ROOT, "\\draw[%s, rounded corners=2pt] (%s,%s) rectangle (%s,%s);%n",
                    estiloNodo(estado), n(x), n(y - 7), n(x + 22), n(y + 7)));
            sb.append(String.format(Locale.ROOT, "\\node[anchor=west, inner sep=0, text=arqTexto, %s] at (%s,%s) {%s};%n",
                    fuente, n(x + 28), n(y), latex(nombre)));
            x += ancho;
        }
        x = 24;
        y += 20;
        for (TipoArista tipo : TipoArista.values()) {
            if (!d.tiposPresentes().contains(tipo)) {
                continue;
            }
            String nombre = ExportadorSvg.nombreTipo(tipo);
            double ancho = 44 + nombre.length() * 5.4;
            if (x + ancho > limite && x > 24) {
                x = 24;
                y += 20;
            }
            sb.append(String.format(Locale.ROOT, "\\draw[%s] (%s,%s) -- (%s,%s);%n",
                    estiloArista(tipo), n(x), n(y), n(x + 24), n(y)));
            sb.append(String.format(Locale.ROOT, "\\node[anchor=west, inner sep=0, text=arqTexto, %s] at (%s,%s) {%s};%n",
                    fuente, n(x + 32), n(y), latex(nombre)));
            x += ancho;
        }
        return sb.toString();
    }

    private static String n(double valor) {
        return String.format(Locale.ROOT, "%.1f", valor);
    }

    /** Escapa los caracteres que LaTeX interpreta como órdenes. */
    public static String latex(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\textbackslash{}");
                case '_' -> sb.append("\\_");
                case '$' -> sb.append("\\$");
                case '#' -> sb.append("\\#");
                case '%' -> sb.append("\\%");
                case '&' -> sb.append("\\&");
                case '{' -> sb.append("\\{");
                case '}' -> sb.append("\\}");
                case '~' -> sb.append("\\textasciitilde{}");
                case '^' -> sb.append("\\textasciicircum{}");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String comentario(String texto) {
        return texto == null ? "" : texto.replace('\n', ' ').replace('%', ' ');
    }
}
