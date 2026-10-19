package co.javeriana.msw.arquitecta.interfaz;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/** Barra de desplazamiento fina y redondeada, sin flechas, coherente con el resto de la interfaz. */
final class BarraDesplazamiento extends BasicScrollBarUI {

    private static final int GROSOR = 10;
    private final Color fondo;

    private BarraDesplazamiento(Color fondo) {
        this.fondo = fondo;
    }

    /** Aplica la barra a un panel desplazable, con el color de fondo de su contenido. */
    static JScrollPane aplicar(JScrollPane desplazable, Color fondo) {
        desplazable.getVerticalScrollBar().setUI(new BarraDesplazamiento(fondo));
        desplazable.getHorizontalScrollBar().setUI(new BarraDesplazamiento(fondo));
        desplazable.getVerticalScrollBar().setPreferredSize(new Dimension(GROSOR, 0));
        desplazable.getHorizontalScrollBar().setPreferredSize(new Dimension(0, GROSOR));
        desplazable.getVerticalScrollBar().setUnitIncrement(16);
        desplazable.getHorizontalScrollBar().setUnitIncrement(16);
        desplazable.setBackground(fondo);
        desplazable.getViewport().setBackground(fondo);
        desplazable.setCorner(JScrollPane.LOWER_RIGHT_CORNER, null);
        return desplazable;
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor = new Color(0xC3CCD9);
        trackColor = fondo;
    }

    @Override
    protected JButton createDecreaseButton(int orientacion) {
        return sinBoton();
    }

    @Override
    protected JButton createIncreaseButton(int orientacion) {
        return sinBoton();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(fondo);
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override
    protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        Tema.suavizar(g);
        g.setColor(isDragging ? new Color(0x8A97AA) : isThumbRollover() ? new Color(0xA9B5C7) : thumbColor);
        g.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 2, r.width - 4, r.height - 4, GROSOR - 4, GROSOR - 4));
        g.dispose();
    }

    private static JButton sinBoton() {
        JButton b = new JButton();
        Dimension cero = new Dimension(0, 0);
        b.setPreferredSize(cero);
        b.setMinimumSize(cero);
        b.setMaximumSize(cero);
        return b;
    }
}
