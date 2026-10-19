package co.javeriana.msw.arquitecta.interfaz;

import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Texto de varias líneas que se ajusta a un ancho fijo, para la barra lateral.
 *
 * <p>Las etiquetas HTML de Swing calculan mal el ancho disponible y recortan el
 * texto; un área de texto con ajuste de línea y un ancho conocido lo parte
 * siempre por palabras y crece en altura lo que haga falta.</p>
 */
final class TextoAjustado extends JTextArea {

    private static final long serialVersionUID = 1L;

    private final int ancho;

    TextoAjustado(String texto, Font fuente, Color color, int ancho) {
        super(texto);
        this.ancho = ancho;
        setFont(fuente);
        setForeground(color);
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setBorder(null);
        setHighlighter(null);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    void mostrar(String texto, Color color) {
        setText(texto);
        setForeground(color);
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        setSize(ancho, Short.MAX_VALUE);
        Dimension d = super.getPreferredSize();
        return new Dimension(ancho, d.height);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
