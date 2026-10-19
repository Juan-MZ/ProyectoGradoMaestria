package co.javeriana.msw.arquitecta.interfaz;

import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

/**
 * Resumen del diagnóstico: cuatro indicadores y la conformidad de cada regla.
 *
 * <p>La Tasa de Conformidad aparece junto al estado que la clasifica y, cuando
 * existe un análisis anterior del mismo proyecto, junto a su variación: el valor
 * absoluto se diluye en sistemas grandes, pero la tendencia no.</p>
 */
final class PanelResumen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Locale ES = Locale.forLanguageTag("es-CO");

    private final JLabel valorCr = valor();
    private final JLabel detalleCr = detalle();
    private final Chip chipEstado = new Chip(15f);
    private final JLabel detalleEstado = detalle();
    private final JLabel valorSistema = valor();
    private final JLabel detalleSistema = detalle();
    private final JLabel valorHallazgos = valor();
    private final JLabel detalleHallazgos = detalle();
    private final JPanel barras = new JPanel(new GridLayout(1, 0, 28, 0));

    PanelResumen() {
        super(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel indicadores = new JPanel(new GridLayout(1, 4, 12, 0));
        indicadores.setOpaque(false);
        indicadores.add(tarjeta("TASA DE CONFORMIDAD", valorCr, detalleCr));
        JPanel contenedorChip = new JPanel(new BorderLayout());
        contenedorChip.setOpaque(false);
        contenedorChip.add(chipEstado, BorderLayout.WEST);
        indicadores.add(tarjeta("ESTADO ARQUITECTÓNICO", contenedorChip, detalleEstado));
        indicadores.add(tarjeta("SISTEMA ANALIZADO", valorSistema, detalleSistema));
        indicadores.add(tarjeta("HALLAZGOS", valorHallazgos, detalleHallazgos));
        add(indicadores, BorderLayout.NORTH);

        Tarjeta reglas = new Tarjeta(new BorderLayout(0, 10));
        reglas.add(titulo("CONFORMIDAD POR REGLA"), BorderLayout.NORTH);
        barras.setOpaque(false);
        reglas.add(barras, BorderLayout.CENTER);
        add(reglas, BorderLayout.CENTER);
    }

    void mostrar(ResultadoAnalisis r) {
        Resultado res = r.resultado();
        valorCr.setText(String.format(ES, "%.2f %%", res.conformidad()));
        valorCr.setForeground(Tema.colorConformidad(res.conformidad()));
        if (r.crAnterior().isPresent()) {
            double delta = res.conformidad() - r.crAnterior().getAsDouble();
            String sentido = Math.abs(delta) < 0.005 ? "sin cambio" : delta > 0 ? "mejora" : "deterioro";
            detalleCr.setText(String.format(ES, "%+.2f pp desde el anterior · %s", delta, sentido));
            detalleCr.setForeground(Math.abs(delta) < 0.005 ? Tema.TEXTO_SUAVE : delta > 0 ? Tema.EXITO : Tema.ALERTA);
        } else {
            detalleCr.setText(res.verificaciones() - res.incumplimientos() + " de " + res.verificaciones()
                    + " verificaciones");
            detalleCr.setForeground(Tema.TEXTO_SUAVE);
        }

        chipEstado.mostrar(res.estado().etiqueta(), Tema.colorEstado(res.estado()), Tema.fondoEstado(res.estado()));
        detalleEstado.setText(res.severidadAlteroElVeredicto()
                ? "La CR sola diría «" + res.estadoPorCR().etiqueta() + "»"
                : "Coincide con la lectura de la CR");
        detalleEstado.setToolTipText(res.severidadAlteroElVeredicto()
                ? "El veredicto se endurece por la severidad del peor hallazgo ("
                        + (res.severidadMaxima() == null ? "" : res.severidadMaxima().etiqueta().toLowerCase(ES)) + ")"
                : null);

        valorSistema.setText(r.paquetes() + (r.paquetes() == 1 ? " paquete" : " paquetes"));
        valorSistema.setForeground(Tema.TEXTO);
        detalleSistema.setText(r.dependencias() + " dependencias · " + String.format(ES, "%.1f s", r.milisegundos() / 1000.0));
        detalleSistema.setToolTipText("Tiempo total del análisis, incluida la lectura del código compilado");

        int criticos = 0;
        int mayores = 0;
        for (Violacion v : res.violaciones()) {
            if (v.severidad() == Violacion.Severidad.CRITICA) {
                criticos++;
            } else if (v.severidad() == Violacion.Severidad.MAYOR) {
                mayores++;
            }
        }
        int total = res.totalViolaciones();
        valorHallazgos.setText(total == 0 ? "Ninguno" : Integer.toString(total));
        valorHallazgos.setForeground(total == 0 ? Tema.EXITO : criticos > 0 ? Tema.ALERTA : Tema.AVISO);
        detalleHallazgos.setText(total == 0 ? "El sistema cumple las reglas evaluadas"
                : criticos + (criticos == 1 ? " crítico" : " críticos") + " · " + mayores
                        + (mayores == 1 ? " mayor" : " mayores"));

        barras.removeAll();
        for (ResultadoRegla regla : res.porRegla()) {
            barras.add(new BarraRegla(regla));
        }
        barras.revalidate();
        barras.repaint();
    }

    // ------------------------------------------------------------ construcción

    private static Tarjeta tarjeta(String titulo, JComponent valor, JLabel detalle) {
        Tarjeta t = new Tarjeta(null);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        JLabel encabezado = titulo(titulo);
        encabezado.setAlignmentX(Component.LEFT_ALIGNMENT);
        valor.setAlignmentX(Component.LEFT_ALIGNMENT);
        detalle.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Las etiquetas ocupan todo el ancho de la tarjeta: con su ancho preferido
        // exacto, una pequeña diferencia de métricas al escalar recortaría el texto.
        for (JComponent c : new JComponent[] {encabezado, valor, detalle}) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height + 2));
        }
        t.add(encabezado);
        t.add(Box.createVerticalStrut(8));
        t.add(valor);
        t.add(Box.createVerticalStrut(6));
        t.add(detalle);
        return t;
    }

    private static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.negrita(10.5f));
        l.setForeground(Tema.TEXTO_TENUE);
        return l;
    }

    private static JLabel valor() {
        JLabel l = new JLabel(" ");
        l.setFont(Tema.negrita(24f));
        l.setForeground(Tema.TEXTO);
        return l;
    }

    private static JLabel detalle() {
        JLabel l = new JLabel(" ");
        l.setFont(Tema.texto(12f));
        l.setForeground(Tema.TEXTO_SUAVE);
        return l;
    }

    /**
     * Columna de una regla: su nombre, una barra de conformidad y el valor. Las
     * reglas se disponen lado a lado para dejar al grafo la mayor altura posible.
     */
    private static final class BarraRegla extends JComponent {

        private static final long serialVersionUID = 1L;
        private static final int ALTO_BARRA = 8;
        private final ResultadoRegla regla;

        BarraRegla(ResultadoRegla regla) {
            this.regla = regla;
            setPreferredSize(new Dimension(220, 44));
            setToolTipText(regla.regla() + ": " + regla.incumplimientos() + " incumplimientos en "
                    + regla.verificaciones() + " verificaciones");
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suavizar(g);
            boolean aplica = regla.verificaciones() > 0;

            g.setFont(Tema.texto(13f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(aplica ? Tema.TEXTO : Tema.TEXTO_TENUE);
            g.drawString(recortar(regla.regla(), fm, getWidth()), 0, fm.getAscent());

            String valor = aplica
                    ? String.format(ES, "%d/%d · %.1f %%", regla.cumplidas(), regla.verificaciones(),
                            regla.conformidadLocal())
                    : "no se verificó";
            g.setFont(aplica ? Tema.negrita(12.5f) : Tema.texto(12f));
            FontMetrics fv = g.getFontMetrics();
            int anchoValor = fv.stringWidth(valor);
            int yFila = getHeight() - Math.max(fv.getHeight(), ALTO_BARRA) - 2;
            int baseValor = yFila + (Math.max(fv.getHeight(), ALTO_BARRA) - fv.getHeight()) / 2 + fv.getAscent();
            g.setColor(aplica ? Tema.TEXTO_SUAVE : Tema.TEXTO_TENUE);
            g.drawString(valor, getWidth() - anchoValor, baseValor);

            int ancho = Math.max(30, getWidth() - anchoValor - 14);
            double yBarra = baseValor - fv.getAscent() / 2.0 - ALTO_BARRA / 2.0 + 1;
            g.setColor(Tema.BORDE_SUAVE);
            g.fill(new RoundRectangle2D.Double(0, yBarra, ancho, ALTO_BARRA, ALTO_BARRA, ALTO_BARRA));
            if (aplica) {
                double lleno = ancho * regla.conformidadLocal() / 100.0;
                if (lleno > 0) {
                    g.setColor(Tema.colorConformidad(regla.conformidadLocal()));
                    g.fill(new RoundRectangle2D.Double(0, yBarra, Math.max(ALTO_BARRA, lleno), ALTO_BARRA,
                            ALTO_BARRA, ALTO_BARRA));
                }
            }
            g.dispose();
        }

        private static String recortar(String texto, FontMetrics fm, int ancho) {
            if (fm.stringWidth(texto) <= ancho) {
                return texto;
            }
            String t = texto;
            while (t.length() > 1 && fm.stringWidth(t + "…") > ancho) {
                t = t.substring(0, t.length() - 1);
            }
            return t.strip() + "…";
        }
    }

}
