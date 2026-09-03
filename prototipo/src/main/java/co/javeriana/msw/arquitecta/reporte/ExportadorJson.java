package co.javeriana.msw.arquitecta.reporte;

import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;

import java.util.List;
import java.util.Locale;

/**
 * Serializa el resultado de una evaluación como JSON.
 *
 * <p>Mientras el reporte textual está pensado para ser leído, esta salida está
 * pensada para ser verificada: permite comparar corridas, versionar la evidencia
 * junto al código y reconstruir cualquier tabla de resultados sin recurrir a la
 * transcripción manual. Se genera sin bibliotecas externas para no comprometer la
 * ligereza del mecanismo.</p>
 */
public final class ExportadorJson {

    private ExportadorJson() {
    }

    public static String exportar(Resultado resultado, GrafoDependencias grafo,
            ConfiguracionReglas config, String etiqueta) {

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"escenario\": ").append(txt(etiqueta)).append(",\n");
        sb.append("  \"raiz\": ").append(txt(config.raiz())).append(",\n");
        sb.append("  \"grafo\": {\n");
        sb.append("    \"paquetes\": ").append(grafo.nodos().size()).append(",\n");
        sb.append("    \"dependencias\": ").append(grafo.cantidadAristas()).append(",\n");
        sb.append("    \"nodos\": ").append(arreglo(grafo.nodos().stream().toList())).append(",\n");
        sb.append("    \"aristas\": [");
        List<String[]> aristas = grafo.aristas();
        for (int i = 0; i < aristas.size(); i++) {
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("      {\"origen\": ").append(txt(aristas.get(i)[0]))
              .append(", \"destino\": ").append(txt(aristas.get(i)[1])).append('}');
        }
        sb.append(aristas.isEmpty() ? "]\n" : "\n    ]\n");
        sb.append("  },\n");

        sb.append("  \"conformidad\": {\n");
        sb.append("    \"cr\": ").append(num(resultado.conformidad())).append(",\n");
        sb.append("    \"verificaciones\": ").append(resultado.verificaciones()).append(",\n");
        sb.append("    \"incumplimientos\": ").append(resultado.incumplimientos()).append(",\n");
        sb.append("    \"umbralAceptable\": ").append(num(resultado.umbralAceptable())).append(",\n");
        sb.append("    \"umbralObservacion\": ").append(num(resultado.umbralObservacion())).append(",\n");
        sb.append("    \"modo\": ").append(txt(resultado.modo().clave())).append(",\n");
        sb.append("    \"estadoPorCR\": ").append(txt(resultado.estadoPorCR().etiqueta())).append(",\n");
        sb.append("    \"severidadMaxima\": ").append(
                resultado.severidadMaxima() == null ? "null" : txt(resultado.severidadMaxima().etiqueta())).append(",\n");
        sb.append("    \"estadoPorSeveridad\": ").append(txt(resultado.estadoPorSeveridad().etiqueta())).append(",\n");
        sb.append("    \"estado\": ").append(txt(resultado.estado().etiqueta())).append('\n');
        sb.append("  },\n");

        sb.append("  \"reglas\": [\n");
        List<ResultadoRegla> reglas = resultado.porRegla();
        for (int i = 0; i < reglas.size(); i++) {
            ResultadoRegla rr = reglas.get(i);
            sb.append("    {\n");
            sb.append("      \"nombre\": ").append(txt(rr.regla())).append(",\n");
            sb.append("      \"verificaciones\": ").append(rr.verificaciones()).append(",\n");
            sb.append("      \"incumplimientos\": ").append(rr.incumplimientos()).append(",\n");
            sb.append("      \"conformidadLocal\": ").append(num(rr.conformidadLocal())).append(",\n");
            sb.append("      \"violaciones\": [\n");
            List<Violacion> vs = rr.violaciones();
            for (int j = 0; j < vs.size(); j++) {
                Violacion v = vs.get(j);
                sb.append("        {\n");
                sb.append("          \"severidad\": ").append(txt(v.severidad().etiqueta())).append(",\n");
                sb.append("          \"descripcion\": ").append(txt(v.descripcion())).append(",\n");
                sb.append("          \"ubicacion\": ").append(arreglo(v.ubicacion())).append(",\n");
                sb.append("          \"evidencia\": ").append(arreglo(v.evidencia())).append('\n');
                sb.append("        }").append(j < vs.size() - 1 ? ",\n" : "\n");
            }
            sb.append("      ]\n");
            sb.append("    }").append(i < reglas.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String arreglo(List<String> valores) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(txt(valores.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String num(double valor) {
        return String.format(Locale.ROOT, "%.4f", valor);
    }

    private static String txt(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
