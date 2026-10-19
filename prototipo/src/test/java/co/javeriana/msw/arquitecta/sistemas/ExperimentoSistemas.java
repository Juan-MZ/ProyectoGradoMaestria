package co.javeriana.msw.arquitecta.sistemas;

import co.javeriana.msw.arquitecta.asistente.ClienteModelo;
import co.javeriana.msw.arquitecta.asistente.ConfiguracionModelo;
import co.javeriana.msw.arquitecta.asistente.Conversacion;
import co.javeriana.msw.arquitecta.asistente.Revision;
import co.javeriana.msw.arquitecta.config.Capa;
import co.javeriana.msw.arquitecta.config.CargadorConfiguracion;
import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.ConstructorGrafo;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.Violacion;
import co.javeriana.msw.arquitecta.servicio.Analisis;
import co.javeriana.msw.arquitecta.servicio.Asistencia;
import co.javeriana.msw.arquitecta.servicio.ErrorAnalisis;
import co.javeriana.msw.arquitecta.servicio.Perfil;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validación de aplicabilidad en entorno controlado sobre sistemas reales.
 *
 * <p>Complementa el experimento del caso de estudio sin sustituirlo. Sobre cada
 * sistema ---una biblioteca de código abierto distribuida como {@code .jar}---
 * se inyectan derivas conocidas, una por familia y después las tres juntas, del
 * mismo modo que C1 a C4 lo hacen sobre el caso de estudio. Como el sistema real
 * ya trae sus propios hallazgos, lo que se mide es la <em>diferencia</em> con la
 * versión original: si aparecen exactamente los hallazgos de las derivas
 * inyectadas, si aparece alguno que no se inyectó y cuánto varía la CR.</p>
 *
 * <p>Uso, desde {@code prototipo/}:</p>
 * <pre>
 *   ... ExperimentoSistemas proponer   # catálogos con el asistente (requiere clave)
 *   ... ExperimentoSistemas ejecutar   # experimento, sin red y determinista
 * </pre>
 */
public final class ExperimentoSistemas {

    private static final Locale L = Locale.ROOT;
    private static final Path SISTEMAS = Path.of("out", "sistemas");
    private static final Path EVIDENCIA = Path.of("ejemplos", "sistemas");

    /** Un sistema del experimento y su catálogo. */
    private record Sistema(String id, String nombre, Path jar, Path catalogo) {
    }

    private static final List<Sistema> BIBLIOTECAS = List.of(
            new Sistema("commons-lang3", "Apache Commons Lang 3.14.0", SISTEMAS.resolve("commons-lang3-3.14.0.jar"),
                    EVIDENCIA.resolve("commons-lang3/catalogo.properties")),
            new Sistema("archunit", "ArchUnit 1.3.0", Path.of("lib", "archunit-1.3.0.jar"),
                    Path.of("ejemplos", "archunit", "rules-archunit.properties")),
            new Sistema("jackson-databind", "Jackson Databind 2.17.2", SISTEMAS.resolve("jackson-databind-2.17.2.jar"),
                    EVIDENCIA.resolve("jackson-databind/catalogo.properties")),
            new Sistema("guava", "Guava 33.2.1", SISTEMAS.resolve("guava-33.2.1-jre.jar"),
                    EVIDENCIA.resolve("guava/catalogo.properties")));

    /**
     * Aplicaciones en capas de código abierto: identificador, nombre y versión
     * fijada. {@code arquitecta aplicaciones} las clona en esa versión y las
     * compila en {@code out/aplicaciones/<id>/target/classes}. Sus catálogos no
     * los propuso el asistente: transcriben la arquitectura que documenta cada
     * proyecto.
     */
    static final List<String[]> APLICACIONES = List.of(
            new String[] {"spring-framework-petclinic", "Spring Framework PetClinic"},
            new String[] {"jpetstore-6", "MyBatis JPetStore 6"},
            new String[] {"dddsample-core", "DDD Sample (Citerus)"});

    /** Bibliotecas y, si están compiladas, las aplicaciones en capas. */
    private static List<Sistema> lista() throws java.io.IOException {
        List<Sistema> lista = new ArrayList<>(BIBLIOTECAS);
        for (String[] app : APLICACIONES) {
            Path clases = Path.of("out", "aplicaciones", app[0], "target", "classes");
            if (!Files.isDirectory(clases)) {
                System.out.println("[aviso] " + app[1] + " no está compilada; se omite (arquitecta aplicaciones).");
                continue;
            }
            Path jar = SISTEMAS.resolve(app[0] + ".jar");
            InyectorDerivas.jarDeDirectorio(clases, jar);
            lista.add(new Sistema(app[0], app[1], jar, EVIDENCIA.resolve(app[0]).resolve("catalogo.properties")));
        }
        return lista;
    }

    /** Dónde se inyectó cada deriva. */
    private record Plan(String[] capas, String[] ciclo, boolean cicloViolaCapas, String paqueteConcentrador,
            List<String> destinosConcentracion) {
    }

    private ExperimentoSistemas() {
    }

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "ejecutar";
        if (modo.equals("proponer")) {
            proponer();
        } else {
            ejecutar();
        }
    }

    // ================================================================ catálogos

    /**
     * Pide al asistente el catálogo de cada sistema que no lo tenga y lo guarda
     * sin modificarlo: es la situación de una organización que adopta la
     * herramienta sin haber declarado nunca su arquitectura.
     */
    private static void proponer() throws Exception {
        ConfiguracionModelo config = ConfiguracionModelo.cargar();
        for (Sistema s : lista()) {
            if (Files.exists(s.catalogo())) {
                System.out.println(s.id() + ": ya tiene catálogo, se conserva.");
                continue;
            }
            Perfil perfil = Asistencia.perfil(s.jar());
            System.out.println(s.id() + ": pidiendo catálogo a " + config.modelo() + "…");
            Conversacion.Respuesta r = new Conversacion(new ClienteModelo(config), perfil.describir(),
                    Asistencia.descripcionReglas()).proponer("");
            if (r.propuesta() == null) {
                throw new IllegalStateException(s.id() + ": el asistente no devolvió un catálogo: " + r.errorFormato());
            }
            String catalogo = r.propuesta().aCatalogo(s.nombre(), config.modelo());
            Files.createDirectories(s.catalogo().getParent());
            Files.write(s.catalogo(), catalogo.getBytes(StandardCharsets.ISO_8859_1));
            StringBuilder md = new StringBuilder("# Propuesta del asistente para " + s.nombre() + "\n\n");
            md.append("Modelo: ").append(config.modelo()).append(". Sin contexto: solo el perfil estructural.\n\n");
            md.append(r.explicacion()).append("\n");
            List<String> avisos = Revision.avisos(r.propuesta(), perfil);
            if (!avisos.isEmpty()) {
                md.append("\n## Revisión de la herramienta\n\n");
                avisos.forEach(a -> md.append("- ").append(a).append('\n'));
            }
            Files.writeString(s.catalogo().resolveSibling("propuesta.md"), md.toString(), StandardCharsets.UTF_8);
            System.out.println("   " + (Asistencia.validar(catalogo).isEmpty() ? "válido" : "NO válido")
                    + (avisos.isEmpty() ? "" : " · " + avisos.size() + " aviso(s) de revisión"));
        }
    }

    // ================================================================ experimento

    private static void ejecutar() throws Exception {
        Path mutantes = SISTEMAS.resolve("mutantes");
        List<String> filas = new ArrayList<>();
        filas.add("sistema,clases,paquetes,dependencias,variante,verificaciones,incumplimientos,cr,delta_cr_pp,"
                + "estado_cr,estado_cr_severidad,derivas_inyectadas,derivas_detectadas,hallazgos_nuevos_no_inyectados,"
                + "hallazgos_desaparecidos,segundos");
        StringBuilder planes = new StringBuilder("# Derivas inyectadas por sistema\n\n");
        for (Sistema s : lista()) {
            ConfiguracionReglas config = CargadorConfiguracion.cargar(s.catalogo());
            GrafoDependencias grafo = new ConstructorGrafo(config.raiz(), config.exclusiones()).desdeRuta(s.jar());
            int clases = ConstructorGrafo.contarPaquetes(s.jar()).entrySet().stream()
                    .filter(e -> e.getKey().equals(config.raiz()) || e.getKey().startsWith(config.raiz() + "."))
                    .filter(e -> config.exclusiones().stream()
                            .noneMatch(x -> e.getKey().equals(x) || e.getKey().startsWith(x + ".")))
                    .mapToInt(Map.Entry::getValue).sum();
            Map<String, List<String>> nombres = ConstructorGrafo.nombresDeClases(s.jar());
            Plan plan = planificar(config, grafo, nombres);
            planes.append("## ").append(s.nombre()).append("\n\n")
                    .append(plan.capas() == null ? "- Capas: no aplica (el catálogo no declara capas)\n"
                            : "- Capas: `" + plan.capas()[0] + "` → `" + plan.capas()[1]
                                    + "` (dependencia que la matriz no permite y que no cierra ningún ciclo)\n")
                    .append("- Ciclo: `").append(plan.ciclo()[0]).append("` → `").append(plan.ciclo()[1])
                    .append(plan.cicloViolaCapas()
                            ? "` (cierra un ciclo; en esta estratificación estricta toda arista que cierra un ciclo"
                                    + " apunta hacia arriba, de modo que también viola las capas)\n"
                            : "` (cierra un ciclo sin violar las capas)\n")
                    .append("- Concentración: paquete nuevo `").append(plan.paqueteConcentrador()).append("` con ")
                    .append(plan.destinosConcentracion().size()).append(" dependencias salientes (umbral ")
                    .append(config.umbralConcentracion()).append(")\n\n");

            ResultadoAnalisis original = analizar(s.jar(), s.catalogo(), s.id() + "-original");
            Set<String> base = claves(original.resultado());
            filas.add(fila(s, clases, original, "original", original.resultado(), 0, 0, 0, 0, 0));
            System.out.printf(L, "%s · original: CR %.2f %%, %d hallazgos, %.1f s%n", s.id(),
                    original.resultado().conformidad(), original.resultado().totalViolaciones(),
                    original.milisegundos() / 1000.0);

            List<String> variantes = plan.capas() == null ? List.of("ciclo", "concentracion", "combinada")
                    : List.of("capas", "ciclo", "concentracion", "combinada");
            for (String variante : variantes) {
                Map<String, byte[]> clasesNuevas = new LinkedHashMap<>();
                List<String> esperadas = new ArrayList<>();
                if (plan.capas() != null && (variante.equals("capas") || variante.equals("combinada"))) {
                    agregar(clasesNuevas, plan.capas()[0], "DerivaCapas", plan.capas()[1], nombres);
                    esperadas.add("capas");
                }
                if (variante.equals("ciclo") || variante.equals("combinada")) {
                    agregar(clasesNuevas, plan.ciclo()[0], "DerivaCiclo", plan.ciclo()[1], nombres);
                    esperadas.add("ciclo");
                }
                if (variante.equals("concentracion") || variante.equals("combinada")) {
                    String[] tipos = plan.destinosConcentracion().stream().map(p -> claseDe(p, nombres)).toArray(String[]::new);
                    clasesNuevas.put(interno(plan.paqueteConcentrador(), "DerivaConcentracion"),
                            InyectorDerivas.clase(plan.paqueteConcentrador(), "DerivaConcentracion", tipos));
                    esperadas.add("concentracion");
                }
                Path jar = mutantes.resolve(s.id() + "-" + variante + ".jar");
                InyectorDerivas.jarConClases(s.jar(), jar, clasesNuevas);
                ResultadoAnalisis r = analizar(jar, s.catalogo(), s.id() + "-" + variante);
                Set<String> claves = claves(r.resultado());
                Set<String> nuevos = new TreeSet<>(claves);
                nuevos.removeAll(base);
                Set<String> desaparecidos = new TreeSet<>(base);
                desaparecidos.removeAll(claves);
                int detectadas = 0;
                for (String e : esperadas) {
                    String encontrado = buscar(nuevos, e, plan);
                    if (encontrado != null) {
                        detectadas++;
                        nuevos.removeIf(k -> explica(k, e, plan));
                        if (e.equals("ciclo") && plan.cicloViolaCapas()) {
                            nuevos.removeIf(k -> k.startsWith("Integridad")
                                    && k.endsWith("| " + plan.ciclo()[0] + " , " + plan.ciclo()[1]));
                        }
                    }
                }
                filas.add(fila(s, clases, r, variante, original.resultado(), esperadas.size(), detectadas,
                        nuevos.size(), desaparecidos.size(), r.milisegundos()));
                System.out.printf(L, "   %-13s CR %.2f %% (%+.2f pp) · detectadas %d/%d · no inyectados %d · desaparecidos %d · %s / %s%n",
                        variante, r.resultado().conformidad(), r.resultado().conformidad() - original.resultado().conformidad(),
                        detectadas, esperadas.size(), nuevos.size(), desaparecidos.size(),
                        r.resultado().estadoPorCR().etiqueta(), r.resultado().estado().etiqueta());
                if (!nuevos.isEmpty()) {
                    System.out.println("      no inyectados: " + nuevos);
                }
                if (!desaparecidos.isEmpty()) {
                    Set<String> aparecidos = new TreeSet<>(claves);
                    aparecidos.removeAll(base);
                    System.out.println("      desaparecidos: " + desaparecidos + "  ->  aparecidos: " + aparecidos);
                }
            }
        }
        Files.createDirectories(EVIDENCIA);
        Files.writeString(EVIDENCIA.resolve("resultados.csv"), String.join("\n", filas) + "\n", StandardCharsets.UTF_8);
        Files.writeString(EVIDENCIA.resolve("derivas-inyectadas.md"), planes.toString(), StandardCharsets.UTF_8);
        System.out.println("Evidencia en " + EVIDENCIA.toAbsolutePath());
    }

    private static ResultadoAnalisis analizar(Path jar, Path catalogo, String etiqueta) throws ErrorAnalisis {
        return Analisis.ejecutar(new Analisis.Solicitud(jar, catalogo, "cr+severidad", etiqueta, null));
    }

    // ================================================================ planificación

    /**
     * Elige, de forma determinista, dónde inyectar cada deriva para que active
     * solo la regla que le corresponde: la de capas no cierra ciclos ni excede el
     * umbral; la de ciclo no viola capas y une paquetes que no estaban en ningún
     * ciclo; la de concentración crea un paquete nuevo sin capa, del que nadie
     * depende.
     */
    private static Plan planificar(ConfiguracionReglas config, GrafoDependencias grafo,
            Map<String, List<String>> nombres) {
        List<String> paquetes = new ArrayList<>(new TreeSet<>(grafo.nodos()));
        // Los ciclos se buscan como los busca FF2: sin los paquetes exentos por su
        // papel, que la regla retira del grafo antes de calcularlos.
        List<String> exentos = config.compartidos();
        GrafoDependencias sinExentos = grafo.filtrar(p -> !exentos.contains(p));
        Map<String, Set<String>> alcance = new LinkedHashMap<>();
        for (String p : paquetes) {
            alcance.put(p, exentos.contains(p) ? Set.of() : alcanzables(sinExentos, p));
        }
        Set<String> enCiclo = new HashSet<>();
        for (String p : paquetes) {
            for (String q : grafo.destinosDe(p)) {
                if (alcance.containsKey(q) && alcance.get(q).contains(p)) {
                    enCiclo.add(p);
                    enCiclo.add(q);
                }
            }
        }
        int umbral = config.umbralConcentracion();
        List<String> compartidos = config.compartidos();

        String[] capas = null;
        String[] ciclo = null;
        for (int pasada = 0; pasada < 2; pasada++) {
            for (String a : paquetes) {
                if (compartidos.contains(a) || grafo.acoplamientoEferente(a) + 1 > umbral || claseDe(a, nombres) == null) {
                    continue;
                }
                for (String b : paquetes) {
                    if (a.equals(b) || grafo.existeArista(a, b) || claseDe(b, nombres) == null) {
                        continue;
                    }
                    Capa ca = capa(config, a);
                    Capa cb = capa(config, b);
                    boolean cierraCiclo = alcance.get(b).contains(a);
                    boolean violaCapas = ca != null && cb != null && !ca.nombre().equals(cb.nombre())
                            && !config.permitidos().getOrDefault(ca.nombre(), Set.of()).contains(cb.nombre());
                    // Si ambos paquetes ya pertenecen al mismo ciclo, la arista nueva no
                    // altera ningún componente: solo puede activar la regla de capas.
                    boolean mismoCiclo = cierraCiclo && alcance.get(a).contains(b);
                    if (pasada == 0 && capas == null && violaCapas && (!cierraCiclo || mismoCiclo)) {
                        capas = new String[] {a, b};
                    }
                    // El ciclo no comparte paquetes con la deriva de capas, para que en la
                    // variante combinada ninguna de las dos altere a la otra.
                    if (pasada == 1 && ciclo == null && cierraCiclo && !violaCapas && !enCiclo.contains(a)
                            && !enCiclo.contains(b) && !compartidos.contains(b)
                            && (capas == null || (!List.of(capas).contains(a) && !List.of(capas).contains(b)))) {
                        ciclo = new String[] {a, b};
                    }
                }
            }
        }
        // En un sistema estrictamente estratificado ---una capa por paquete, sin
        // dependencias dentro de la misma capa--- toda arista que cierra un ciclo
        // apunta hacia arriba y viola también las capas. Solo entonces se admite
        // que la deriva de ciclo active las dos reglas; el hallazgo de capas que
        // produce se cuenta como explicado por la inyección.
        boolean cicloViolaCapas = false;
        if (ciclo == null) {
            for (String a : paquetes) {
                if (ciclo != null) {
                    break;
                }
                if (compartidos.contains(a) || grafo.acoplamientoEferente(a) + 1 > umbral || claseDe(a, nombres) == null
                        || (capas != null && List.of(capas).contains(a))) {
                    continue;
                }
                for (String b : paquetes) {
                    if (!a.equals(b) && !grafo.existeArista(a, b) && claseDe(b, nombres) != null
                            && alcance.get(b).contains(a) && !enCiclo.contains(a) && !enCiclo.contains(b)
                            && !compartidos.contains(b) && (capas == null || !List.of(capas).contains(b))) {
                        ciclo = new String[] {a, b};
                        cicloViolaCapas = true;
                        break;
                    }
                }
            }
        }
        // Un sistema sin capas declaradas no admite la deriva de capas: se omite.
        if (ciclo == null || (capas == null && !config.capas().isEmpty())) {
            throw new IllegalStateException("No se encontró dónde inyectar " + (ciclo == null ? "el ciclo" : "la deriva de capas"));
        }
        String concentrador = config.raiz() + ".derivainyectada";
        Capa capaNueva = capa(config, concentrador);
        List<String> destinos = new ArrayList<>();
        for (String p : paquetes) {
            if (destinos.size() > umbral) {
                break;
            }
            Capa cp = capa(config, p);
            boolean admitido = capaNueva == null || cp == null || cp.nombre().equals(capaNueva.nombre())
                    || config.permitidos().getOrDefault(capaNueva.nombre(), Set.of()).contains(cp.nombre());
            if (admitido && claseDe(p, nombres) != null) {
                destinos.add(p);
            }
        }
        return new Plan(capas, ciclo, cicloViolaCapas, concentrador, destinos);
    }

    private static Capa capa(ConfiguracionReglas config, String paquete) {
        for (Capa c : config.capas()) {
            for (String prefijo : c.paquetes()) {
                if (paquete.equals(prefijo) || paquete.startsWith(prefijo + ".")) {
                    return c;
                }
            }
        }
        return null;
    }

    private static Set<String> alcanzables(GrafoDependencias grafo, String desde) {
        Set<String> vistos = new HashSet<>();
        Deque<String> pendientes = new ArrayDeque<>(grafo.destinosDe(desde));
        while (!pendientes.isEmpty()) {
            String p = pendientes.pop();
            if (vistos.add(p)) {
                pendientes.addAll(grafo.destinosDe(p));
            }
        }
        return vistos;
    }

    /** Una clase existente del paquete, para usarla como tipo del campo inyectado. */
    private static String claseDe(String paquete, Map<String, List<String>> nombres) {
        List<String> clases = nombres.getOrDefault(paquete, List.of());
        return clases.isEmpty() ? null : paquete + "." + clases.get(0);
    }

    private static void agregar(Map<String, byte[]> clases, String origen, String nombre, String destino,
            Map<String, List<String>> nombres) {
        String tipo = claseDe(destino, nombres);
        if (tipo == null) {
            throw new IllegalStateException("No hay una clase conocida en " + destino);
        }
        clases.put(interno(origen, nombre), InyectorDerivas.clase(origen, nombre, tipo));
    }

    private static String interno(String paquete, String nombre) {
        return paquete.replace('.', '/') + "/" + nombre + ".class";
    }

    // ================================================================ comparación

    /** Identidad de cada hallazgo: regla y paquetes implicados. */
    private static Set<String> claves(Resultado r) {
        Set<String> claves = new TreeSet<>();
        for (Violacion v : r.violaciones()) {
            List<String> u = new ArrayList<>(v.ubicacion());
            if (v.regla().startsWith("Ciclos")) {
                u = new ArrayList<>(new TreeSet<>(u));
            }
            claves.add(v.regla() + " | " + String.join(" , ", u));
        }
        return claves;
    }

    private static String buscar(Set<String> nuevos, String esperada, Plan plan) {
        for (String k : nuevos) {
            if (explica(k, esperada, plan)) {
                return k;
            }
        }
        return null;
    }

    /** Si un hallazgo nuevo es el que la deriva inyectada debía producir. */
    private static boolean explica(String clave, String esperada, Plan plan) {
        return switch (esperada) {
            case "capas" -> plan.capas() != null && clave.startsWith("Integridad")
                    && clave.endsWith("| " + plan.capas()[0] + " , " + plan.capas()[1]);
            case "ciclo" -> clave.startsWith("Ciclos") && contiene(clave, plan.ciclo()[0]) && contiene(clave, plan.ciclo()[1]);
            default -> clave.startsWith("Concentración") && contiene(clave, plan.paqueteConcentrador());
        };
    }

    private static boolean contiene(String clave, String paquete) {
        String lista = clave.substring(clave.indexOf('|') + 1);
        for (String p : lista.split(",")) {
            if (p.strip().equals(paquete)) {
                return true;
            }
        }
        return false;
    }

    private static String fila(Sistema s, int clases, ResultadoAnalisis r, String variante, Resultado original,
            int inyectadas, int detectadas, int nuevos, int desaparecidos, long ms) {
        Resultado res = r.resultado();
        return String.join(",", s.id(), "" + clases, "" + r.paquetes(), "" + r.dependencias(), variante,
                "" + res.verificaciones(), "" + res.incumplimientos(), String.format(L, "%.2f", res.conformidad()),
                String.format(L, "%.2f", res.conformidad() - original.conformidad()), res.estadoPorCR().etiqueta(),
                res.estado().etiqueta(), "" + inyectadas, "" + detectadas, "" + nuevos, "" + desaparecidos,
                String.format(L, "%.2f", (ms == 0 ? r.milisegundos() : ms) / 1000.0));
    }
}
