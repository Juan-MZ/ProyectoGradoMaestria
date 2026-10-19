package co.javeriana.msw.arquitecta.interfaz;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

/** Panel con fondo blanco, esquinas redondeadas y un borde tenue. */
public class Tarjeta extends JPanel {

    private static final long serialVersionUID = 1L;

    private Color fondo = Tema.SUPERFICIE;
    private Color borde = Tema.BORDE;

    public Tarjeta(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 14, 16));
    }

    public void colores(Color fondo, Color borde) {
        this.fondo = fondo;
        this.borde = borde;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Tema.suavizar(g);
        RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 12, 12);
        g.setColor(fondo);
        g.fill(forma);
        g.setColor(borde);
        g.draw(forma);
        g.dispose();
        super.paintComponent(g0);
    }
}
