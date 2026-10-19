package co.javeriana.msw.arquitecta.interfaz;

import co.javeriana.msw.arquitecta.motor.EstadoArquitectonico;
import co.javeriana.msw.arquitecta.reglas.Violacion;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Paleta, tipografía y utilidades de dibujo de la interfaz.
 *
 * <p>Los colores coinciden con los del SVG, el TikZ y las figuras de la
 * monografía, de modo que un mismo hallazgo se reconoce igual en cualquiera de
 * las salidas: rojo para lo crítico, ámbar para lo que requiere atención, verde
 * para lo conforme.</p>
 */
final class Tema {

    static final Color FONDO = new Color(0xF3F5F9);
    static final Color SUPERFICIE = Color.WHITE;
    static final Color LATERAL = new Color(0xFAFBFD);
    static final Color BORDE = new Color(0xDCE2EB);
    static final Color BORDE_SUAVE = new Color(0xE8ECF2);
    static final Color TEXTO = new Color(0x1F2D44);
    static final Color TEXTO_SUAVE = new Color(0x5B6B82);
    static final Color TEXTO_TENUE = new Color(0x8A97AA);
    static final Color CABECERA = new Color(0x18263C);
    static final Color CABECERA_2 = new Color(0x26406A);
    static final Color PRIMARIO = new Color(0x2F5BA8);
    static final Color PRIMARIO_HOVER = new Color(0x274E93);
    static final Color PRIMARIO_SUAVE = new Color(0xE8EFFA);
    static final Color BORDE_GRAFO = new Color(0x3B5C8F);

    static final Color EXITO = new Color(0x2E7D4F);
    static final Color EXITO_FONDO = new Color(0xE3F3E9);
    static final Color AVISO = new Color(0xB7791F);
    static final Color AVISO_FONDO = new Color(0xFDF3DC);
    static final Color ALERTA = new Color(0xC0392B);
    static final Color ALERTA_FONDO = new Color(0xFBE4E2);
    static final Color NUCLEO = new Color(0x8A94A6);
    static final Color NUCLEO_FONDO = new Color(0xF4F5F7);

    private static final String FAMILIA_TEXTO = elegir(
            List.of("Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Roboto", "Noto Sans", "DejaVu Sans"),
            Font.SANS_SERIF);
    private static final String FAMILIA_CODIGO = elegir(
            List.of("Cascadia Mono", "Consolas", "JetBrains Mono", "SF Mono", "Menlo", "DejaVu Sans Mono"),
            Font.MONOSPACED);

    private Tema() {
    }

    static Font texto(float tamano) {
        return new Font(FAMILIA_TEXTO, Font.PLAIN, 12).deriveFont(tamano);
    }

    static Font negrita(float tamano) {
        return new Font(FAMILIA_TEXTO, Font.BOLD, 12).deriveFont(tamano);
    }

    static Font codigo(float tamano) {
        return new Font(FAMILIA_CODIGO, Font.PLAIN, 12).deriveFont(tamano);
    }

    static Color colorEstado(EstadoArquitectonico estado) {
        return switch (estado) {
            case ACEPTABLE -> EXITO;
            case EN_OBSERVACION -> AVISO;
            case NO_CONFORME -> ALERTA;
        };
    }

    static Color fondoEstado(EstadoArquitectonico estado) {
        return switch (estado) {
            case ACEPTABLE -> EXITO_FONDO;
            case EN_OBSERVACION -> AVISO_FONDO;
            case NO_CONFORME -> ALERTA_FONDO;
        };
    }

    static Color colorSeveridad(Violacion.Severidad severidad) {
        return switch (severidad) {
            case CRITICA -> ALERTA;
            case MAYOR -> AVISO;
            case MENOR -> TEXTO_SUAVE;
        };
    }

    static Color fondoSeveridad(Violacion.Severidad severidad) {
        return switch (severidad) {
            case CRITICA -> ALERTA_FONDO;
            case MAYOR -> AVISO_FONDO;
            case MENOR -> BORDE_SUAVE;
        };
    }

    /** Color de una barra de conformidad según su valor, con los mismos umbrales que la CR. */
    static Color colorConformidad(double porcentaje) {
        if (porcentaje >= 85) {
            return EXITO;
        }
        return porcentaje >= 70 ? AVISO : ALERTA;
    }

    static void suavizar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    static Color conAlfa(Color c, float alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(alfa * 255));
    }

    private static String elegir(List<String> preferidas, String porDefecto) {
        try {
            Set<String> disponibles = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()).collect(Collectors.toSet());
            for (String familia : preferidas) {
                if (disponibles.contains(familia)) {
                    return familia;
                }
            }
        } catch (RuntimeException e) {
            // Sin acceso a las fuentes del sistema se usa la lógica de Java.
        }
        return porDefecto;
    }
}
