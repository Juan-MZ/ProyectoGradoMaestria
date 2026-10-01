package co.javeriana.msw.arquitecta.reporte;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Lee la Tasa de Conformidad registrada en corridas anteriores.
 *
 * <p>El valor absoluto de la métrica padece un efecto de dilución: al ser una
 * proporción, una violación aislada pesa cada vez menos conforme crece el
 * sistema, de modo que en un proyecto grande la cifra apenas se mueve. Su
 * <em>variación</em> no sufre ese problema. Un descenso de cuatro puntos en una
 * iteración es igual de significativo en un sistema de cinco paquetes que en uno
 * de quinientos, porque se mide contra el propio sistema y no contra un umbral
 * universal. Comparar cada corrida con la anterior convierte así un detector
 * puntual en un indicador de tendencia, que es la forma en que la gobernanza
 * continua resulta verdaderamente útil.</p>
 */
public final class Historico {

    private static final int COLUMNA_ESCENARIO = 0;
    private static final int COLUMNA_CR = 5;

    private Historico() {
    }

    /**
     * Devuelve la última Tasa de Conformidad registrada para el escenario
     * indicado, o un valor vacío si no hay historia previa.
     *
     * <p>La ausencia de historia es un caso normal ---la primera corrida--- y no
     * un error: el reporte simplemente omite la tendencia.</p>
     */
    public static OptionalDouble crAnterior(Path resumenCsv, String escenario) {
        if (resumenCsv == null || escenario == null || !Files.isRegularFile(resumenCsv)) {
            return OptionalDouble.empty();
        }
        try {
            OptionalDouble ultima = OptionalDouble.empty();
            for (String linea : Files.readAllLines(resumenCsv, StandardCharsets.UTF_8)) {
                if (linea.isBlank() || linea.startsWith(ExportadorCsv.CABECERA_RESUMEN.substring(0, 9))) {
                    continue;
                }
                List<String> celdas = separar(linea);
                if (celdas.size() <= COLUMNA_CR || !escenario.equals(celdas.get(COLUMNA_ESCENARIO))) {
                    continue;
                }
                try {
                    ultima = OptionalDouble.of(Double.parseDouble(celdas.get(COLUMNA_CR)));
                } catch (NumberFormatException ignorada) {
                    // Una fila corrupta no debe impedir el diagnóstico actual.
                }
            }
            return ultima;
        } catch (IOException e) {
            return OptionalDouble.empty();
        }
    }

    /** Describe la variación entre dos tasas, en puntos porcentuales. */
    public static String describirTendencia(double actual, double anterior) {
        double delta = actual - anterior;
        String etiqueta;
        if (Math.abs(delta) < 0.005) {
            etiqueta = "sin cambio";
        } else if (delta > 0) {
            etiqueta = "mejora";
        } else {
            etiqueta = "deterioro";
        }
        return String.format(Locale.ROOT, "%+.2f pp respecto de la corrida anterior (%.2f%%) · %s",
                delta, anterior, etiqueta);
    }

    /** Separa una línea CSV respetando los campos entrecomillados. */
    private static List<String> separar(String linea) {
        List<String> celdas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == ',' && !entreComillas) {
                celdas.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        celdas.add(actual.toString());
        return celdas;
    }
}
