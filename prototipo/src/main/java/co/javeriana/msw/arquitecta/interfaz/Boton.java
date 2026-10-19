package co.javeriana.msw.arquitecta.interfaz;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Botón plano y redondeado, dibujado por completo para que su aspecto no dependa
 * del aspecto del sistema operativo.
 */
final class Boton extends JButton {

    private static final long serialVersionUID = 1L;

    enum Estilo { PRIMARIO, SECUNDARIO, DISCRETO }

    private final Estilo estilo;
    private boolean encima;
    private boolean pulsado;

    Boton(String texto, Estilo estilo) {
        super(texto);
        this.estilo = estilo;
        setFont(estilo == Estilo.PRIMARIO ? Tema.negrita(13.5f) : Tema.texto(13f));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 0, 0, 0));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                encima = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encima = false;
                pulsado = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pulsado = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pulsado = false;
                repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int alto = estilo == Estilo.PRIMARIO ? 42 : 34;
        return new Dimension(fm.stringWidth(getText()) + 32, alto);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Tema.suavizar(g);
        double w = getWidth();
        double h = getHeight();
        RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 10, 10);

        Color fondo;
        Color borde;
        Color texto;
        boolean activo = isEnabled();
        switch (estilo) {
            case PRIMARIO -> {
                fondo = !activo ? new Color(0xB9C5D8) : pulsado ? Tema.CABECERA_2
                        : encima ? Tema.PRIMARIO_HOVER : Tema.PRIMARIO;
                borde = fondo;
                texto = Color.WHITE;
            }
            case SECUNDARIO -> {
                fondo = !activo ? Tema.LATERAL : pulsado ? Tema.PRIMARIO_SUAVE
                        : encima ? new Color(0xF0F4FB) : Tema.SUPERFICIE;
                borde = !activo ? Tema.BORDE_SUAVE : encima ? Tema.PRIMARIO : Tema.BORDE;
                texto = activo ? Tema.PRIMARIO : Tema.TEXTO_TENUE;
            }
            default -> {
                fondo = activo && (encima || pulsado) ? Tema.PRIMARIO_SUAVE : new Color(0, 0, 0, 0);
                borde = fondo;
                texto = activo ? Tema.PRIMARIO : Tema.TEXTO_TENUE;
            }
        }
        g.setColor(fondo);
        g.fill(forma);
        g.setColor(borde);
        g.draw(forma);
        if (hasFocus() && activo) {
            g.setColor(Tema.conAlfa(Tema.PRIMARIO, 0.35f));
            g.draw(new RoundRectangle2D.Double(2.5, 2.5, w - 5, h - 5, 8, 8));
        }

        g.setFont(getFont());
        g.setColor(texto);
        FontMetrics fm = g.getFontMetrics();
        int x = (int) Math.round((w - fm.stringWidth(getText())) / 2);
        int y = (int) Math.round((h - fm.getHeight()) / 2 + fm.getAscent());
        g.drawString(getText(), x, y);
        g.dispose();
    }
}
