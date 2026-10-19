package co.javeriana.msw.arquitecta.interfaz;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Pestañas con subrayado, dibujadas por la propia interfaz.
 *
 * <p>Sustituyen al componente de pestañas de Swing, cuyo aspecto depende del
 * sistema operativo y no encaja con el resto de la interfaz. El contenido de cada
 * pestaña se presenta dentro de una tarjeta, igual que los indicadores.</p>
 */
final class Pestanas extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final List<Pestana> pestanas = new ArrayList<>();
    private int seleccion;

    Pestanas() {
        super(new BorderLayout(0, 0));
        setOpaque(false);
        barra.setOpaque(false);
        barra.setBorder(new EmptyBorder(0, 4, 0, 0));
        add(barra, BorderLayout.NORTH);

        Tarjeta marco = new Tarjeta(new BorderLayout());
        marco.setBorder(new EmptyBorder(1, 1, 1, 1));
        contenido.setOpaque(false);
        marco.add(contenido, BorderLayout.CENTER);
        add(marco, BorderLayout.CENTER);
    }

    void agregar(String titulo, JComponent componente) {
        int indice = pestanas.size();
        Pestana p = new Pestana(titulo, indice);
        pestanas.add(p);
        barra.add(p);
        contenido.add(componente, Integer.toString(indice));
        if (indice == 0) {
            seleccionar(0);
        }
    }

    void titulo(int indice, String titulo) {
        pestanas.get(indice).texto = titulo;
        pestanas.get(indice).revalidate();
        barra.repaint();
    }

    void seleccionar(int indice) {
        seleccion = indice;
        tarjetas.show(contenido, Integer.toString(indice));
        barra.repaint();
    }

    int seleccionada() {
        return seleccion;
    }

    private final class Pestana extends JComponent {

        private static final long serialVersionUID = 1L;
        private String texto;
        private final int indice;
        private boolean encima;

        Pestana(String texto, int indice) {
            this.texto = texto;
            this.indice = indice;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    seleccionar(Pestana.this.indice);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    encima = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    encima = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Tema.negrita(13.5f));
            return new Dimension(fm.stringWidth(texto) + 34, 40);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suavizar(g);
            boolean activa = indice == seleccion;
            if (encima && !activa) {
                g.setColor(Tema.PRIMARIO_SUAVE);
                g.fill(new RoundRectangle2D.Double(4, 5, getWidth() - 8, getHeight() - 12, 8, 8));
            }
            g.setFont(activa ? Tema.negrita(13.5f) : Tema.texto(13.5f));
            g.setColor(activa ? Tema.PRIMARIO : Tema.TEXTO_SUAVE);
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(texto)) / 2;
            int y = (getHeight() - 6 - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(texto, x, y);
            if (activa) {
                g.fill(new RoundRectangle2D.Double(12, getHeight() - 5, getWidth() - 24, 3, 3, 3));
            }
            g.dispose();
        }
    }
}
