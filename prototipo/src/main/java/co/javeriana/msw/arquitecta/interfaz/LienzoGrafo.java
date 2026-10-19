package co.javeriana.msw.arquitecta.interfaz;

import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Arista;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Banda;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.EstadoNodo;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Nodo;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.TipoArista;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo.Trayecto;
import co.javeriana.msw.arquitecta.reporte.ExportadorSvg;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Dibuja el grafo de dependencias de forma interactiva.
 *
 * <p>No tiene algoritmo de disposición propio: pinta la misma
 * {@link DisposicionGrafo} que producen el SVG y el TikZ, de modo que lo que se
 * ve en pantalla es exactamente lo que se exporta. Añade lo que solo una pantalla
 * permite: acercar con la rueda, desplazarse arrastrando, consultar un paquete
 * pasando el cursor y, al hacer clic, atenuar todo lo que no le concierne para
 * seguir sus dependencias sin perderse en el resto.</p>
 */
final class LienzoGrafo extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final double ESCALA_MINIMA = 0.12;
    private static final double ESCALA_MAXIMA = 4.0;
    private static final int MARGEN = 24;
    private static final int ALTO_LEYENDA = 64;

    private static final Color ARISTA = new Color(0x7F93B5);
    private static final Color ARISTA_NUCLEO = new Color(0xC9CED6);
    private static final Stroke DISCONTINUO_NUCLEO =
            new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] {4f, 3f}, 0f);

    private DisposicionGrafo disposicion;
    private double escala = 1;
    private double desplazamientoX;
    private double desplazamientoY;
    /** Verdadero cuando el usuario acercó o desplazó la vista; hasta entonces el grafo se ajusta solo. */
    private boolean vistaManual;
    /** Ancho de la leyenda en el último repintado; antes del primero se estima. */
    private int anchoLeyenda = 380;
    private Point inicioArrastre;
    private double xInicio;
    private double yInicio;
    private boolean arrastrado;
    private String seleccionado;
    private String encima;
    private Set<String> resaltados = Set.of();
    private Consumer<String> alSeleccionar = p -> { };

    LienzoGrafo() {
        setOpaque(true);
        setToolTipText("");
        setFocusable(true);

        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                inicioArrastre = e.getPoint();
                xInicio = desplazamientoX;
                yInicio = desplazamientoY;
                arrastrado = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (inicioArrastre == null) {
                    return;
                }
                int dx = e.getX() - inicioArrastre.x;
                int dy = e.getY() - inicioArrastre.y;
                if (Math.abs(dx) + Math.abs(dy) > 3) {
                    arrastrado = true;
                    vistaManual = true;
                }
                desplazamientoX = xInicio + dx;
                desplazamientoY = yInicio + dy;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!arrastrado && SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() >= 2) {
                        ajustar();
                    } else {
                        Nodo n = nodoEn(e.getPoint());
                        seleccionar(n == null ? null : n.paquete());
                    }
                }
                inicioArrastre = null;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Nodo n = nodoEn(e.getPoint());
                String nuevo = n == null ? null : n.paquete();
                if (!java.util.Objects.equals(nuevo, encima)) {
                    encima = nuevo;
                    setCursor(java.awt.Cursor.getPredefinedCursor(
                            nuevo == null ? java.awt.Cursor.DEFAULT_CURSOR : java.awt.Cursor.HAND_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = Math.pow(1.12, -e.getPreciseWheelRotation());
                acercarEn(e.getPoint(), factor);
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
        addMouseWheelListener(raton);
    }

    void mostrar(DisposicionGrafo disposicion) {
        this.disposicion = disposicion;
        this.seleccionado = null;
        this.encima = null;
        this.resaltados = Set.of();
        ajustar();
    }

    void alSeleccionar(Consumer<String> accion) {
        this.alSeleccionar = accion == null ? p -> { } : accion;
    }

    /** Vuelve a encajar el grafo completo en el área visible. */
    void ajustar() {
        vistaManual = false;
        repaint();
    }

    /**
     * Calcula la escala y el desplazamiento que encajan el contenido dibujado
     * ---bandas, columna lateral y paquetes--- en el área visible, dejando sitio a la
     * leyenda. Se invoca en cada repintado mientras la vista no se haya tocado, de
     * modo que el grafo se reajusta solo al cambiar el tamaño de la ventana.
     */
    private void calcularAjuste() {
        if (disposicion == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        boolean conBandas = !disposicion.bandas().isEmpty();
        double minX = conBandas ? disposicion.xBandas() : Double.MAX_VALUE;
        double maxX = conBandas ? disposicion.xBandas() + disposicion.anchoBandas() : 0;
        double minY = Double.MAX_VALUE;
        double maxY = 0;
        for (Banda b : disposicion.bandas()) {
            minY = Math.min(minY, b.y());
            maxY = Math.max(maxY, b.y() + b.alto());
        }
        for (Nodo n : disposicion.nodos()) {
            minX = Math.min(minX, n.x());
            maxX = Math.max(maxX, n.derecha());
            minY = Math.min(minY, n.y() - (n.lateral() ? 30 : 0));
            maxY = Math.max(maxY, n.abajo() + (n.lateral() ? 16 : 0));
        }
        if (disposicion.tieneColumnaLateral()) {
            maxX = Math.max(maxX, disposicion.xColumna() + disposicion.anchoColumna());
        }
        double ancho = Math.max(1, getWidth() - 2.0 * MARGEN);
        double contenidoAncho = Math.max(1, maxX - minX);
        double contenidoAlto = Math.max(1, maxY - minY);
        // Primero se prueba con toda la altura; si el grafo, centrado, invadiría la
        // leyenda de la esquina inferior izquierda, se le reserva su franja.
        double alto = Math.max(1, getHeight() - 2.0 * MARGEN);
        escala = Math.max(ESCALA_MINIMA, Math.min(1.5, Math.min(ancho / contenidoAncho, alto / contenidoAlto)));
        if ((getWidth() - contenidoAncho * escala) / 2 < 14 + anchoLeyenda + 12) {
            alto = Math.max(1, alto - ALTO_LEYENDA);
            escala = Math.max(ESCALA_MINIMA, Math.min(1.5, Math.min(ancho / contenidoAncho, alto / contenidoAlto)));
        }
        desplazamientoX = (getWidth() - contenidoAncho * escala) / 2 - minX * escala;
        desplazamientoY = MARGEN + Math.max(0, (alto - contenidoAlto * escala) / 2) - minY * escala;
    }

    void acercar(double factor) {
        acercarEn(new Point(getWidth() / 2, getHeight() / 2), factor);
    }

    private void acercarEn(Point punto, double factor) {
        vistaManual = true;
        double nueva = Math.max(ESCALA_MINIMA, Math.min(ESCALA_MAXIMA, escala * factor));
        double real = nueva / escala;
        desplazamientoX = punto.x - (punto.x - desplazamientoX) * real;
        desplazamientoY = punto.y - (punto.y - desplazamientoY) * real;
        escala = nueva;
        repaint();
    }

    /** Selecciona un paquete y atenúa todo lo que no está conectado con él. */
    void seleccionar(String paquete) {
        seleccionado = paquete;
        resaltados = Set.of();
        repaint();
        alSeleccionar.accept(paquete);
    }

    /** Resalta un conjunto de paquetes ---los de un hallazgo--- y atenúa el resto. */
    void resaltar(Set<String> paquetes) {
        seleccionado = null;
        resaltados = paquetes == null ? Set.of() : Set.copyOf(paquetes);
        repaint();
    }

    String seleccionado() {
        return seleccionado;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Nodo n = nodoEn(e.getPoint());
        if (n == null) {
            return null;
        }
        return "<html><div style='padding:2px'><b>" + html(n.paquete()) + "</b><br>"
                + "Capa: " + html(n.capa() == null ? "sin capa" : n.capa()) + "<br>"
                + "Ce = " + n.ce() + " &middot; Ca = " + n.ca() + "<br>"
                + "Estado: " + html(ExportadorSvg.nombreEstado(n.estado()))
                + "<br><span style='color:#8A97AA'>Clic para ver sus dependencias</span></div></html>";
    }

    // --------------------------------------------------------------- dibujo

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Tema.suavizar(g);
        g.setColor(Tema.SUPERFICIE);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (disposicion == null) {
            g.dispose();
            return;
        }

        if (!vistaManual) {
            calcularAjuste();
        }
        AffineTransform base = g.getTransform();
        g.translate(desplazamientoX, desplazamientoY);
        g.scale(escala, escala);

        Set<String> foco = foco();
        Set<String> visibles = visibles(foco);
        boolean hayFoco = !foco.isEmpty();

        dibujarBandas(g);
        for (Arista a : disposicion.aristas()) {
            boolean relevante = !hayFoco || relevante(a, foco);
            dibujarArista(g, a, relevante ? 1f : 0.10f);
        }
        for (Nodo n : disposicion.nodos()) {
            dibujarNodo(g, n, !hayFoco || visibles.contains(n.paquete()) ? 1f : 0.28f);
        }

        g.setTransform(base);
        dibujarLeyenda(g);
        g.dispose();
    }

    private void dibujarBandas(Graphics2D g) {
        int indice = 0;
        g.setFont(Tema.negrita(11f));
        for (Banda banda : disposicion.bandas()) {
            if (banda.capa() == null) {
                continue;
            }
            g.setColor(indice++ % 2 == 0 ? new Color(0xF3F6FB) : new Color(0xE9EEF6));
            g.fill(new RoundRectangle2D.Double(disposicion.xBandas(), banda.y(),
                    disposicion.anchoBandas(), banda.alto(), 12, 12));
            g.setColor(Tema.BORDE_GRAFO);
            g.drawString("CAPA " + banda.capa().toUpperCase(Locale.ROOT),
                    (float) disposicion.xBandas() + 10, (float) banda.y() + 18);
        }
        if (disposicion.tieneColumnaLateral()) {
            double yMin = Double.MAX_VALUE;
            double yMax = 0;
            for (Nodo n : disposicion.nodos()) {
                if (n.lateral()) {
                    yMin = Math.min(yMin, n.y());
                    yMax = Math.max(yMax, n.abajo());
                }
            }
            double yPanel = Math.min(yMin - 30, disposicion.bandas().get(0).y());
            g.setColor(new Color(0xC5CEDC));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[] {5f, 4f}, 0f));
            g.draw(new RoundRectangle2D.Double(disposicion.xColumna(), yPanel,
                    disposicion.anchoColumna(), yMax + 16 - yPanel, 12, 12));
            g.setColor(Tema.TEXTO_TENUE);
            g.drawString("SIN CAPA", (float) disposicion.xColumna() + 10, (float) yPanel + 18);
        }
    }

    private void dibujarArista(Graphics2D g, Arista a, float alfa) {
        Composite previo = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
        Color color;
        Stroke trazo;
        switch (a.tipo()) {
            case NUCLEO -> {
                color = ARISTA_NUCLEO;
                trazo = new BasicStroke(1f);
            }
            case CICLO -> {
                color = Tema.ALERTA;
                trazo = new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                        new float[] {6f, 4f}, 0f);
            }
            case VIOLA_CAPAS -> {
                color = Tema.ALERTA;
                trazo = new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            }
            default -> {
                color = ARISTA;
                trazo = new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            }
        }
        Trayecto t = a.trayecto();
        Path2D curva = new Path2D.Double();
        curva.moveTo(t.x0(), t.y0());
        curva.curveTo(t.cx1(), t.cy1(), t.cx2(), t.cy2(), t.x1(), t.y1());
        g.setColor(color);
        g.setStroke(trazo);
        g.draw(curva);

        // Punta de flecha orientada según el último tramo de la curva.
        double dx = t.x1() - t.cx2();
        double dy = t.y1() - t.cy2();
        if (Math.hypot(dx, dy) < 0.5) {
            dx = t.x1() - t.x0();
            dy = t.y1() - t.y0();
        }
        double angulo = Math.atan2(dy, dx);
        double largo = a.tipo() == TipoArista.VIOLA_CAPAS ? 9 : 7.5;
        Path2D punta = new Path2D.Double();
        punta.moveTo(t.x1(), t.y1());
        punta.lineTo(t.x1() - largo * Math.cos(angulo - 0.42), t.y1() - largo * Math.sin(angulo - 0.42));
        punta.lineTo(t.x1() - largo * Math.cos(angulo + 0.42), t.y1() - largo * Math.sin(angulo + 0.42));
        punta.closePath();
        g.fill(punta);
        g.setComposite(previo);
    }

    private void dibujarNodo(Graphics2D g, Nodo n, float alfa) {
        Composite previo = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
        RoundRectangle2D forma = new RoundRectangle2D.Double(n.x(), n.y(), n.ancho(), DisposicionGrafo.ALTO_NODO, 10, 10);

        boolean esSeleccionado = n.paquete().equals(seleccionado);
        boolean esEncima = n.paquete().equals(encima);
        if (esSeleccionado || esEncima || resaltados.contains(n.paquete())) {
            g.setColor(Tema.conAlfa(Tema.PRIMARIO, esSeleccionado ? 0.28f : 0.16f));
            g.fill(new RoundRectangle2D.Double(n.x() - 4, n.y() - 4, n.ancho() + 8,
                    DisposicionGrafo.ALTO_NODO + 8, 14, 14));
        }

        Color relleno;
        Color borde;
        float grosor;
        switch (n.estado()) {
            case CICLO -> {
                relleno = Tema.ALERTA_FONDO;
                borde = Tema.ALERTA;
                grosor = 1.8f;
            }
            case CONCENTRACION -> {
                relleno = Tema.AVISO_FONDO;
                borde = Tema.AVISO;
                grosor = 1.8f;
            }
            case CICLO_Y_CONCENTRACION -> {
                relleno = Tema.ALERTA_FONDO;
                borde = Tema.AVISO;
                grosor = 2.6f;
            }
            case COMPARTIDO -> {
                relleno = Tema.NUCLEO_FONDO;
                borde = Tema.NUCLEO;
                grosor = 1.3f;
            }
            default -> {
                relleno = Tema.SUPERFICIE;
                borde = Tema.BORDE_GRAFO;
                grosor = 1.3f;
            }
        }
        g.setColor(relleno);
        g.fill(forma);
        g.setColor(esSeleccionado ? Tema.PRIMARIO : borde);
        g.setStroke(n.estado() == EstadoNodo.COMPARTIDO ? DISCONTINUO_NUCLEO
                : new BasicStroke(esSeleccionado ? 2.6f : grosor));
        g.draw(forma);

        g.setFont(Tema.codigo(12f));
        g.setColor(Tema.TEXTO);
        FontMetrics fm = g.getFontMetrics();
        String etiqueta = n.etiqueta();
        float x = (float) (n.cx() - fm.stringWidth(etiqueta) / 2.0);
        float y = (float) (n.cy() - fm.getHeight() / 2.0 + fm.getAscent());
        g.drawString(etiqueta, x, y);
        g.setComposite(previo);
    }

    /** Leyenda superpuesta en pantalla: siempre visible, con independencia del zoom. */
    private void dibujarLeyenda(Graphics2D g) {
        g.setFont(Tema.texto(11.5f));
        FontMetrics fm = g.getFontMetrics();
        int x0 = 14;
        int y0 = getHeight() - ALTO_LEYENDA + 6;

        int anchoNodos = 0;
        for (EstadoNodo e : EstadoNodo.values()) {
            if (disposicion.estadosPresentes().contains(e)) {
                anchoNodos += 30 + fm.stringWidth(ExportadorSvg.nombreEstado(e)) + 16;
            }
        }
        int anchoAristas = 0;
        for (TipoArista t : TipoArista.values()) {
            if (disposicion.tiposPresentes().contains(t)) {
                anchoAristas += 34 + fm.stringWidth(ExportadorSvg.nombreTipo(t)) + 16;
            }
        }
        int ancho = Math.max(anchoNodos, anchoAristas) + 14;
        anchoLeyenda = ancho;
        g.setColor(Tema.conAlfa(Color.WHITE, 0.94f));
        g.fill(new RoundRectangle2D.Double(x0, y0, ancho, 52, 10, 10));
        g.setColor(Tema.BORDE_SUAVE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Double(x0, y0, ancho, 52, 10, 10));

        int x = x0 + 12;
        int y = y0 + 19;
        for (EstadoNodo e : EstadoNodo.values()) {
            if (!disposicion.estadosPresentes().contains(e)) {
                continue;
            }
            Graphics2D gm = (Graphics2D) g.create();
            gm.translate(x, y - 10);
            gm.scale(22.0 / 60, 13.0 / DisposicionGrafo.ALTO_NODO);
            dibujarNodo(gm, new Nodo("", "", null, e, 0, 0, 60, false, 0, 0, 0), 1f);
            gm.dispose();
            g.setColor(Tema.TEXTO_SUAVE);
            String nombre = ExportadorSvg.nombreEstado(e);
            g.drawString(nombre, x + 30, y + 1);
            x += 30 + fm.stringWidth(nombre) + 16;
        }
        x = x0 + 12;
        y += 22;
        for (TipoArista t : TipoArista.values()) {
            if (!disposicion.tiposPresentes().contains(t)) {
                continue;
            }
            Graphics2D ga = (Graphics2D) g.create();
            Nodo a = new Nodo("", "", null, EstadoNodo.NORMAL, x, y - 4, 0, false, 0, 0, 0);
            Nodo b = new Nodo("", "", null, EstadoNodo.NORMAL, x + 24, y - 4, 0, false, 0, 0, 0);
            dibujarArista(ga, new Arista(a, b, t, new Trayecto(x, y - 4, x + 8, y - 4, x + 16, y - 4, x + 24, y - 4),
                    false), 1f);
            ga.dispose();
            g.setColor(Tema.TEXTO_SUAVE);
            String nombre = ExportadorSvg.nombreTipo(t);
            g.drawString(nombre, x + 34, y + 1);
            x += 34 + fm.stringWidth(nombre) + 16;
        }
    }

    // ---------------------------------------------------------------- foco

    private Set<String> foco() {
        if (seleccionado != null) {
            return Set.of(seleccionado);
        }
        return resaltados;
    }

    private Set<String> visibles(Set<String> foco) {
        Set<String> visibles = new HashSet<>(foco);
        if (seleccionado != null) {
            for (Arista a : disposicion.aristas()) {
                if (a.origen().paquete().equals(seleccionado)) {
                    visibles.add(a.destino().paquete());
                }
                if (a.destino().paquete().equals(seleccionado)) {
                    visibles.add(a.origen().paquete());
                }
            }
        }
        return visibles;
    }

    private boolean relevante(Arista a, Set<String> foco) {
        if (seleccionado != null) {
            return foco.contains(a.origen().paquete()) || foco.contains(a.destino().paquete());
        }
        return foco.contains(a.origen().paquete()) && foco.contains(a.destino().paquete());
    }

    private Nodo nodoEn(Point punto) {
        if (disposicion == null) {
            return null;
        }
        double x = (punto.x - desplazamientoX) / escala;
        double y = (punto.y - desplazamientoY) / escala;
        for (Nodo n : disposicion.nodos()) {
            if (x >= n.x() && x <= n.derecha() && y >= n.y() && y <= n.abajo()) {
                return n;
            }
        }
        return null;
    }

    private static String html(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
