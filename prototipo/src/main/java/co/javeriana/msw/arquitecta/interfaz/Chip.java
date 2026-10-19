package co.javeriana.msw.arquitecta.interfaz;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Etiqueta en forma de píldora, con un punto de color: estados y severidades. */
final class Chip extends JComponent {

    private static final long serialVersionUID = 1L;

    private String texto = "";
    private Color color = Tema.TEXTO_SUAVE;
    private Color fondo = Tema.BORDE_SUAVE;

    Chip(float tamano) {
        setFont(Tema.negrita(tamano));
        setOpaque(false);
    }

    void mostrar(String texto, Color color, Color fondo) {
        this.texto = texto == null ? "" : texto;
        this.color = color;
        this.fondo = fondo;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(texto) + 34, fm.getHeight() + 10);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (texto.isEmpty()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        Tema.suavizar(g);
        Dimension d = getPreferredSize();
        double h = Math.min(getHeight(), d.height);
        double y = (getHeight() - h) / 2;
        g.setColor(fondo);
        g.fill(new RoundRectangle2D.Double(0, y, Math.min(getWidth(), d.width), h, h, h));
        g.setColor(color);
        double punto = 8;
        g.fill(new Ellipse2D.Double(11, y + (h - punto) / 2, punto, punto));
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, 25, (float) (y + (h - fm.getHeight()) / 2 + fm.getAscent()));
        g.dispose();
    }
}
