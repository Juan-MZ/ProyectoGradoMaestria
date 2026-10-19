package co.javeriana.msw.arquitecta.asistente;

import co.javeriana.msw.arquitecta.reglas.Violacion;
import co.javeriana.msw.arquitecta.servicio.Asistencia;
import co.javeriana.msw.arquitecta.servicio.Perfil;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Evaluación exploratoria del asistente de catálogo con un modelo de lenguaje
 * real. No forma parte de la batería de pruebas: consulta un servicio externo,
 * requiere una clave y su resultado varía entre ejecuciones.
 *
 * <p>Para cada escenario del caso de estudio, el asistente propone un catálogo
 * <em>sin contexto alguno</em> ---solo con el perfil estructural del código, que
 * en C1 a C4 ya contiene las derivas inducidas---. La pregunta que se mide no es
 * si su catálogo coincide con el de referencia, sino si es seguro adoptarlo: si
 * con él la herramienta sigue detectando las derivas que se introdujeron, o si
 * el modelo, al describir el código que ve, las legitimó.</p>
 *
 * <p>Uso: {@code java -cp "out/arquitecta-core;out/arquitecta-test;lib/*"
 * co.javeriana.msw.arquitecta.asistente.EvaluacionAsistente <repeticiones> <etiqueta> [modelo]}.
 * Usa la configuración de {@code ~/.arquitecta/asistente.properties}; la clave
 * nunca se escribe en la evidencia.</p>
 */
public final class EvaluacionAsistente {

    private static final Locale ES = Locale.ROOT;
    private static final String P = "com.caso.presentacion";
    private static final String N = "com.caso.negocio";
    private static final String D = "com.caso.persistencia";
    private static final String U = "com.caso.util";

    /** Deriva inducida esperable: regla y, para FF1/FF3, los paquetes implicados. */
    private record Esperada(String nombre, String regla, String origen, String destino) {
    }

    private record Escenario(String carpeta, String etiqueta, double crExperimento, List<Esperada> derivas) {
    }

    private static final List<Escenario> ESCENARIOS = List.of(
            new Escenario("c0-base", "C0", 100.0, List.of()),
            new Escenario("c1-capas", "C1", 90.91, List.of(new Esperada("capas presentacion->persistencia", "Integridad", P, D))),
            new Escenario("c2-ciclo", "C2", 83.33, List.of(new Esperada("ciclo en negocio", "Ciclos", null, null))),
            new Escenario("c3-concentracion", "C3", 91.67, List.of(new Esperada("concentracion en util", "Concentración", U, null))),
            new Escenario("c4-combinado", "C4", 57.14, List.of(
                    new Esperada("capas persistencia->negocio", "Integridad", D, N),
                    new Esperada("capas persistencia->presentacion", "Integridad", D, P),
                    new Esperada("ciclo entre capas", "Ciclos", null, null),
                    new Esperada("concentracion en util", "Concentración", U, null))));

    private EvaluacionAsistente() {
    }

    public static void main(String[] args) throws Exception {
        int repeticiones = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        String etiqueta = args.length > 1 ? args[1] : "evaluacion";
        ConfiguracionModelo base = ConfiguracionModelo.cargar();
        ConfiguracionModelo config = args.length > 2
                ? new ConfiguracionModelo(base.proveedor(), base.url(), args[2], base.clave()) : base;
        Path salida = Path.of("ejemplos", "asistente", etiqueta);
        Files.createDirectories(salida);
        String reglas = Asistencia.descripcionReglas();

        List<String> filas = new ArrayList<>();
        filas.add("escenario,repeticion,modelo,valido,segundos,capas,estratificacion_correcta,umbral,exenciones,"
                + "derivas_inducidas,derivas_detectadas,derivas_ocultas,hallazgos_adicionales,derivas_senaladas,"
                + "preguntas,cr_propuesta,cr_experimento,aviso_umbral");
        for (Escenario e : ESCENARIOS) {
            Path proyecto = Path.of("caso-estudio", e.carpeta());
            Perfil perfil = Asistencia.perfil(proyecto);
            for (int r = 1; r <= repeticiones; r++) {
                String base0 = e.etiqueta() + "-" + r;
                System.out.printf(ES, "%s · repetición %d/%d …%n", e.etiqueta(), r, repeticiones);
                long inicio = System.nanoTime();
                Conversacion.Respuesta respuesta;
                try {
                    respuesta = new Conversacion(new ClienteModelo(config), perfil.describir(), reglas).proponer("");
                } catch (ClienteModelo.ErrorModelo ex) {
                    System.out.println("   fallo: " + ex.getMessage());
                    filas.add(String.join(",", e.etiqueta(), "" + r, config.modelo(), "error", "", "", "", "", "",
                            "" + e.derivas().size(), "", "", "", "", "", "", "" + e.crExperimento()));
                    continue;
                }
                double segundos = (System.nanoTime() - inicio) / 1e9;
                Propuesta p = respuesta.propuesta();
                Files.writeString(salida.resolve(base0 + "-respuesta.md"),
                        "# " + e.etiqueta() + " · repetición " + r + " · " + config.modelo() + "\n\n"
                                + respuesta.explicacion() + "\n", StandardCharsets.UTF_8);
                if (p == null) {
                    filas.add(String.join(",", e.etiqueta(), "" + r, config.modelo(), "sin-catalogo",
                            fmt(segundos), "", "", "", "", "" + e.derivas().size(), "", "", "", "", "", "",
                            "" + e.crExperimento()));
                    continue;
                }
                String catalogo = p.aCatalogo(e.carpeta(), config.modelo());
                Files.write(salida.resolve(base0 + ".properties"), catalogo.getBytes(StandardCharsets.ISO_8859_1));
                boolean valido = Asistencia.validar(catalogo).isEmpty();
                String capas = String.join("|", p.capas().stream().map(Propuesta.Capa::nombre).toList());
                String exenciones = String.join("|", p.exenciones().stream()
                        .map(x -> x.tipo() + ":" + x.paquete().replace("com.caso.", "")).toList());
                if (!valido) {
                    filas.add(String.join(",", e.etiqueta(), "" + r, config.modelo(), "no", fmt(segundos), capas,
                            "", "" + p.umbralConcentracion(), exenciones, "" + e.derivas().size(), "", "", "",
                            "" + p.derivasProbables().size(), "" + p.preguntas().size(), "", "" + e.crExperimento()));
                    continue;
                }
                ResultadoAnalisis vista = Asistencia.previsualizar(proyecto, catalogo);
                List<Violacion> hallazgos = new ArrayList<>(vista.resultado().violaciones());
                int detectadas = 0;
                List<String> ocultas = new ArrayList<>();
                for (Esperada esperada : e.derivas()) {
                    Violacion v = buscar(hallazgos, esperada);
                    if (v != null) {
                        detectadas++;
                        hallazgos.remove(v);
                    } else {
                        ocultas.add(esperada.nombre());
                    }
                }
                List<String> adicionales = hallazgos.stream().map(EvaluacionAsistente::resumir).toList();
                filas.add(String.join(",", e.etiqueta(), "" + r, config.modelo(), "si", fmt(segundos), capas,
                        estratificacion(p) ? "si" : "no", "" + p.umbralConcentracion(), exenciones,
                        "" + e.derivas().size(), "" + detectadas, comillas(String.join("; ", ocultas)),
                        comillas(String.join("; ", adicionales)), "" + p.derivasProbables().size(),
                        "" + p.preguntas().size(), fmt(vista.resultado().conformidad()), "" + e.crExperimento(),
                        Revision.avisos(p, perfil).isEmpty() ? "no" : "si"));
                System.out.printf(ES, "   %.0f s · derivas detectadas %d/%d · ocultas: %s · adicionales: %s%n",
                        segundos, detectadas, e.derivas().size(), ocultas, adicionales);
            }
        }
        Files.write(salida.resolve("resultados.csv"), String.join("\n", filas).concat("\n")
                .getBytes(StandardCharsets.UTF_8));
        System.out.println("Evidencia en " + salida.toAbsolutePath());
    }

    /** El hallazgo de la previsualización que corresponde a una deriva inducida, si lo hay. */
    private static Violacion buscar(List<Violacion> hallazgos, Esperada esperada) {
        for (Violacion v : hallazgos) {
            if (!v.regla().startsWith(esperada.regla())) {
                continue;
            }
            List<String> u = v.ubicacion();
            if (esperada.origen() == null) {
                return v;
            }
            if (esperada.destino() == null ? u.contains(esperada.origen())
                    : u.size() >= 2 && u.get(0).equals(esperada.origen()) && u.get(1).equals(esperada.destino())) {
                return v;
            }
        }
        return null;
    }

    /**
     * Si la propuesta declara la estratificación prevista del caso: presentación,
     * negocio y persistencia en capas distintas, con las dependencias
     * descendentes permitidas y las ascendentes prohibidas.
     */
    private static boolean estratificacion(Propuesta p) {
        Boolean pn = permite(p, P, N);
        Boolean nd = permite(p, N, D);
        Boolean np = permite(p, N, P);
        Boolean dn = permite(p, D, N);
        Boolean dp = permite(p, D, P);
        String cp = capa(p, P);
        String cn = capa(p, N);
        String cd = capa(p, D);
        return cp != null && cn != null && cd != null && !cp.equals(cn) && !cn.equals(cd) && !cp.equals(cd)
                && Boolean.TRUE.equals(pn) && Boolean.TRUE.equals(nd)
                && Boolean.FALSE.equals(np) && Boolean.FALSE.equals(dn) && Boolean.FALSE.equals(dp);
    }

    private static Boolean permite(Propuesta p, String origen, String destino) {
        String a = capa(p, origen);
        String b = capa(p, destino);
        if (a == null || b == null) {
            return null;
        }
        return a.equals(b) || p.permitidos().getOrDefault(a, List.of()).contains(b);
    }

    private static String capa(Propuesta p, String paquete) {
        for (Propuesta.Capa c : p.capas()) {
            for (String prefijo : c.paquetes()) {
                if (paquete.equals(prefijo) || paquete.startsWith(prefijo + ".")) {
                    return c.nombre();
                }
            }
        }
        return null;
    }

    private static String resumir(Violacion v) {
        return v.regla().split(" ")[0] + " " + String.join("->", v.ubicacion()).replace("com.caso.", "");
    }

    private static String fmt(double x) {
        return String.format(ES, "%.2f", x);
    }

    private static String comillas(String s) {
        return "\"" + s.replace("\"", "'") + "\"";
    }
}
