package co.javeriana.msw.arquitecta.interfaz;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Barra superior con el nombre de la herramienta, y el logotipo con que se dibuja su ícono. */
final class Cabecera extends JComponent {

    private static final long serialVersionUID = 1L;

    Cabecera() {
        setPreferredSize(new Dimension(800, 64));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Tema.suavizar(g);
        int w = getWidth();
        int h = getHeight();
        g.setPaint(new GradientPaint(0, 0, Tema.CABECERA, w, h, Tema.CABECERA_2));
        g.fillRect(0, 0, w, h);

        dibujarLogotipo(g, 20, 14, 36, Color.WHITE, new Color(0x8FB3EA), new Color(0xF2B8B0));

        g.setColor(Color.WHITE);
        g.setFont(Tema.negrita(19f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("Arquitecta", 70, 30);
        g.setFont(Tema.texto(12.5f));
        g.setColor(new Color(0xC9D6EA));
        g.drawString("Detección de deriva arquitectónica · Fitness Functions sobre grafos de dependencia", 70, 49);

        String derecha = "Maestría en Ingeniería de Software · Pontificia Universidad Javeriana Cali";
        g.setFont(Tema.texto(11.5f));
        FontMetrics fd = g.getFontMetrics();
        if (w > 70 + fm.stringWidth("Arquitecta") + 560 + fd.stringWidth(derecha)) {
            g.setColor(new Color(0x9FB2CF));
            g.drawString(derecha, w - 22 - fd.stringWidth(derecha), 38);
        }
        g.dispose();
    }

    /**
     * Logotipo: tres capas apiladas unidas por dependencias descendentes, y una que
     * remonta la estratificación en rojo. Resume en un ícono lo que la herramienta
     * busca.
     */
    static void dibujarLogotipo(Graphics2D g, double x, double y, double lado, Color capa, Color linea, Color alerta) {
        double ancho = lado * 0.62;
        double alto = lado * 0.2;
        double cx = x + lado * 0.42;
        double[] ys = {y + lado * 0.04, y + lado * 0.40, y + lado * 0.76};
        g.setStroke(new BasicStroke((float) Math.max(1.2, lado / 18), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(linea);
        for (int i = 0; i < 2; i++) {
            g.draw(new Line2D.Double(cx, ys[i] + alto, cx, ys[i + 1]));
        }
        g.setColor(alerta);
        double xa = cx + ancho / 2 + lado * 0.12;
        g.draw(new Line2D.Double(cx + ancho / 2, ys[2] + alto / 2, xa, ys[2] + alto / 2));
        g.draw(new Line2D.Double(xa, ys[2] + alto / 2, xa, ys[0] + alto / 2));
        g.draw(new Line2D.Double(xa, ys[0] + alto / 2, cx + ancho / 2, ys[0] + alto / 2));
        g.setColor(capa);
        for (double yc : ys) {
            g.fill(new RoundRectangle2D.Double(cx - ancho / 2, yc, ancho, alto, alto * 0.6, alto * 0.6));
        }
    }

    /** Ícono de la ventana en varios tamaños, dibujado a partir del logotipo. */
    static List<Image> iconos() {
        return List.of(icono(16), icono(32), icono(48), icono(64), icono(128));
    }

    private static Image icono(int lado) {
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Tema.suavizar(g);
        g.setPaint(new GradientPaint(0, 0, Tema.CABECERA_2, lado, lado, Tema.CABECERA));
        g.fill(new RoundRectangle2D.Double(0, 0, lado, lado, lado * 0.28, lado * 0.28));
        double margen = lado * 0.16;
        dibujarLogotipo(g, margen, margen, lado - 2 * margen, Color.WHITE, new Color(0x8FB3EA), new Color(0xF28B7E));
        g.dispose();
        return img;
    }
}
