package co.javeriana.msw.arquitecta.asistente;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catálogo de reglas propuesto por el asistente, con la justificación de cada
 * decisión.
 *
 * <p>Se traduce al mismo formato {@code .properties} que se escribe a mano, y
 * cada justificación queda como comentario junto a la decisión que explica: el
 * catálogo resultante es también el registro de por qué se declaró así, que es lo
 * que permite discutirlo después.</p>
 */
public record Propuesta(
        String raiz,
        List<Capa> capas,
        Map<String, List<String>> permitidos,
        Integer umbralConcentracion,
        String justificacionUmbral,
        List<Exencion> exenciones,
        List<String> derivasProbables,
        List<String> preguntas) {

    /** Una capa: su nombre, los prefijos de paquete que agrupa y por qué. */
    public record Capa(String nombre, List<String> paquetes, String justificacion) {
    }

    /** Una exención: {@code compartido} o {@code excluir}, con su motivo. */
    public record Exencion(String paquete, String tipo, String justificacion) {
    }

    public Propuesta {
        capas = List.copyOf(capas);
        permitidos = Map.copyOf(permitidos);
        exenciones = List.copyOf(exenciones);
        derivasProbables = List.copyOf(derivasProbables);
        preguntas = List.copyOf(preguntas);
    }

    /**
     * Interpreta la propuesta en el formato JSON acordado con el modelo. Es
     * tolerante con lo accesorio ---campos ausentes, números como texto--- y
     * estricto solo con la raíz, sin la cual no hay catálogo.
     */
    public static Propuesta desdeJson(Object json) {
        if (!(json instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("La propuesta no es un objeto JSON.");
        }
        String raiz = texto(m.get("raiz"));
        if (raiz.isEmpty()) {
            throw new IllegalArgumentException("La propuesta no indica la raíz de paquetes.");
        }
        List<Capa> capas = new ArrayList<>();
        for (Object c : lista(m.get("capas"))) {
            if (c instanceof Map<?, ?> capa) {
                String nombre = nombreCapa(texto(capa.get("nombre")));
                List<String> paquetes = textos(capa.get("paquetes"));
                if (!nombre.isEmpty() && !paquetes.isEmpty()) {
                    capas.add(new Capa(nombre, paquetes, texto(capa.get("justificacion"))));
                }
            }
        }
        Map<String, List<String>> permitidos = new LinkedHashMap<>();
        if (m.get("permitido") instanceof Map<?, ?> p) {
            for (Map.Entry<?, ?> e : p.entrySet()) {
                List<String> destinos = new ArrayList<>();
                for (String d : textos(e.getValue())) {
                    destinos.add(nombreCapa(d));
                }
                permitidos.put(nombreCapa(String.valueOf(e.getKey())), destinos);
            }
        }
        Integer umbral = null;
        String justificacionUmbral = "";
        Object u = m.get("umbral_concentracion");
        if (u instanceof Map<?, ?> umbralMapa) {
            umbral = entero(umbralMapa.get("valor"));
            justificacionUmbral = texto(umbralMapa.get("justificacion"));
        } else {
            umbral = entero(u);
        }
        List<Exencion> exenciones = new ArrayList<>();
        for (Object e : lista(m.get("exenciones"))) {
            if (e instanceof Map<?, ?> ex) {
                String paquete = texto(ex.get("paquete"));
                String tipo = texto(ex.get("tipo")).toLowerCase(Locale.ROOT);
                if (!paquete.isEmpty() && (tipo.equals("compartido") || tipo.equals("excluir"))) {
                    exenciones.add(new Exencion(paquete, tipo, texto(ex.get("justificacion"))));
                }
            }
        }
        return new Propuesta(raiz, capas, permitidos, umbral, justificacionUmbral, exenciones,
                textos(m.get("derivas_probables")), textos(m.get("preguntas")));
    }

    /**
     * El catálogo en formato {@code .properties}, con las justificaciones como
     * comentarios. Los comentarios se limitan a caracteres Latin-1, la
     * codificación con que Java lee estos archivos.
     */
    public String aCatalogo(String proyecto, String modelo) {
        StringBuilder sb = new StringBuilder();
        String linea = "# " + "=".repeat(76) + "\n";
        sb.append(linea);
        comentario(sb, "Catalogo de reglas de " + proyecto);
        comentario(sb, "Propuesto con el asistente de Arquitecta (" + modelo + ") el " + LocalDate.now() + ".");
        comentario(sb, "Revise cada decision antes de adoptarlo: el catalogo debe declarar la"
                + " arquitectura PREVISTA del sistema, no la que el codigo tiene hoy.");
        sb.append(linea).append('\n');

        sb.append("raiz=").append(raiz).append("\n\n");

        if (!capas.isEmpty()) {
            comentario(sb, "Capas, de la mas alta a la mas baja. Cada una agrupa prefijos de paquete;"
                    + " los subpaquetes heredan la capa.");
            sb.append("capas=").append(String.join(",", capas.stream().map(Capa::nombre).toList())).append('\n');
            for (Capa c : capas) {
                if (!c.justificacion().isEmpty()) {
                    comentario(sb, c.nombre() + ": " + c.justificacion());
                }
                sb.append("capa.").append(c.nombre()).append('=').append(String.join(",", c.paquetes())).append('\n');
            }
            sb.append('\n');
            comentario(sb, "Dependencias permitidas entre capas (dentro de una misma capa siempre se permiten).");
            for (Capa c : capas) {
                sb.append("permitido.").append(c.nombre()).append('=')
                        .append(String.join(",", permitidos.getOrDefault(c.nombre(), List.of()))).append('\n');
            }
            sb.append('\n');
        }

        if (umbralConcentracion != null) {
            if (!justificacionUmbral.isEmpty()) {
                comentario(sb, "Umbral de concentracion: " + justificacionUmbral);
            }
            sb.append("umbral.concentracion=").append(umbralConcentracion).append("\n\n");
        }

        List<Exencion> compartidos = exenciones.stream().filter(e -> e.tipo().equals("compartido")).toList();
        List<Exencion> excluidos = exenciones.stream().filter(e -> e.tipo().equals("excluir")).toList();
        if (!compartidos.isEmpty()) {
            comentario(sb, "Paquetes que concentran dependencias por su papel (exentos de ciclos y concentracion):");
            for (Exencion e : compartidos) {
                comentario(sb, "  " + e.paquete() + ": " + e.justificacion());
            }
            sb.append("rol.compartido=").append(String.join(",", compartidos.stream().map(Exencion::paquete).toList()))
                    .append("\n\n");
        }
        if (!excluidos.isEmpty()) {
            comentario(sb, "Codigo que no forma parte del sistema evaluado (sale del analisis):");
            for (Exencion e : excluidos) {
                comentario(sb, "  " + e.paquete() + ": " + e.justificacion());
            }
            sb.append("excluir=").append(String.join(",", excluidos.stream().map(Exencion::paquete).toList()))
                    .append("\n\n");
        }

        if (!derivasProbables.isEmpty()) {
            comentario(sb, "Dependencias actuales que este catalogo NO permite y que el asistente considera"
                    + " probable deriva (apareceran como hallazgos):");
            for (String d : derivasProbables) {
                comentario(sb, "  - " + d);
            }
            sb.append('\n');
        }
        sb.append("clasificacion.modo=cr+severidad\n");
        return sb.toString();
    }

    // ------------------------------------------------------------- apoyo

    /** Normaliza un nombre de capa: minúsculas, sin acentos ni espacios ni puntos. */
    static String nombreCapa(String nombre) {
        String sinAcentos = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).strip().replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
    }

    /**
     * Escribe un comentario partido en renglones de hasta 78 caracteres. Un texto
     * que empieza con dos espacios es un elemento de lista y se sangra.
     */
    private static void comentario(StringBuilder sb, String texto) {
        String limpio = latin1(texto);
        boolean elemento = limpio.startsWith("  ");
        String prefijo = elemento ? "#   " : "# ";
        String continuacion = elemento ? "#     " : "# ";
        StringBuilder renglon = new StringBuilder();
        for (String palabra : limpio.strip().split("\\s+")) {
            if (renglon.length() > 0 && prefijo.length() + renglon.length() + 1 + palabra.length() > 78) {
                sb.append(prefijo).append(renglon).append('\n');
                renglon.setLength(0);
                prefijo = continuacion;
            }
            if (renglon.length() > 0) {
                renglon.append(' ');
            }
            renglon.append(palabra);
        }
        if (renglon.length() > 0) {
            sb.append(prefijo).append(renglon).append('\n');
        }
    }

    /** Sustituye la tipografía que Latin-1 no representa y descarta el resto. */
    static String latin1(String texto) {
        String t = texto.replace('—', '-').replace('–', '-').replace("→", "->")
                .replace('‘', '\'').replace('’', '\'').replace('“', '"').replace('”', '"')
                .replace("…", "...").replace('\n', ' ').replace('\r', ' ');
        StringBuilder sb = new StringBuilder();
        for (char c : t.toCharArray()) {
            if (c <= 0xFF) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<?> lista(Object valor) {
        return valor instanceof List<?> l ? l : List.of();
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).strip();
    }

    private static List<String> textos(Object valor) {
        List<String> textos = new ArrayList<>();
        for (Object o : lista(valor)) {
            String t = texto(o);
            if (!t.isEmpty()) {
                textos.add(t);
            }
        }
        return textos;
    }

    private static Integer entero(Object valor) {
        if (valor instanceof Number n) {
            return (int) Math.round(n.doubleValue());
        }
        try {
            return valor == null ? null : Integer.valueOf(String.valueOf(valor).strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
