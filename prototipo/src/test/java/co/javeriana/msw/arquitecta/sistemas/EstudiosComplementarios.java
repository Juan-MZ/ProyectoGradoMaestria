package co.javeriana.msw.arquitecta.sistemas;

import co.javeriana.msw.arquitecta.comparacion.EquivalenteArchUnit;
import co.javeriana.msw.arquitecta.config.CargadorConfiguracion;
import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.ConstructorGrafo;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.grafo.Tarjan;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.ReglaCiclos;
import co.javeriana.msw.arquitecta.reglas.ReglaConcentracion;
import co.javeriana.msw.arquitecta.reglas.ReglaIntegridadCapas;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;
import co.javeriana.msw.arquitecta.servicio.Analisis;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Estudios que completan la evaluación de la sección 4.7 de la monografía.
 *
 * <ul>
 *   <li><b>umbral</b>: distribución del acoplamiento eferente en un conjunto de
 *       calibración (cuatro bibliotecas) y comprobación del umbral derivado en un
 *       conjunto de validación independiente (aplicaciones en capas y caso de
 *       estudio), al modo de los umbrales por percentiles de Alves et al. (2010).</li>
 *   <li><b>evolucion</b>: aplica el catálogo declarado para una versión de
 *       referencia a las versiones publicadas antes y después de ella. Lo que se
 *       observa es deriva real, no inyectada.</li>
 *   <li><b>oraculo</b>: contrasta el grafo extraído con el que obtiene una
 *       herramienta independiente, JDepend, sobre los mismos binarios.</li>
 *   <li><b>archunit</b>: las reglas del caso de estudio escritas con ArchUnit,
 *       para comparar esfuerzo y resultado ({@link EquivalenteArchUnit}).</li>
 * </ul>
 *
 * <p>Uso, desde {@code prototipo/} y tras compilar:</p>
 * <pre>
 *   arquitecta estudios [umbral|evolucion|oraculo|archunit|todos]
 * </pre>
 *
 * <p>Los {@code .jar} se descargan de Maven Central la primera vez y se verifican
 * contra el SHA-1 que el repositorio publica junto a cada artefacto.</p>
 */
public final class EstudiosComplementarios {

    private static final Locale L = Locale.ROOT;
    private static final Path DESCARGAS = Path.of("out", "sistemas", "versiones");
    private static final Path EVIDENCIA = Path.of("ejemplos", "estudios");
    private static final Path CATALOGOS = Path.of("ejemplos", "sistemas");
    private static final Path APLICACIONES = Path.of("out", "aplicaciones");

    /** Percentiles de riesgo moderado, alto y muy alto de Alves et al. (2010). */
    private static final double[] PERCENTILES = {0.50, 0.70, 0.80, 0.90};

    /** Un sistema analizable: ruta a sus binarios y catálogo que declara su arquitectura. */
    record Objetivo(String id, String nombre, String conjunto, Path ruta, Path catalogo) {
    }

    /** Una serie de versiones publicadas de un mismo artefacto. */
    private record Serie(String id, String nombre, String grupo, String artefacto, Path catalogo,
            String referencia, List<String> versiones) {
    }

    private static final List<Serie> SERIES = List.of(
            new Serie("jackson-databind", "Jackson Databind", "com.fasterxml.jackson.core", "jackson-databind",
                    CATALOGOS.resolve("jackson-databind/catalogo.properties"), "2.17.2",
                    List.of("2.4.0", "2.6.0", "2.8.0", "2.10.0", "2.12.0", "2.14.0", "2.16.0", "2.17.2",
                            "2.18.0", "2.19.0", "2.20.0", "2.21.0", "2.22.0")),
            new Serie("guava", "Guava", "com.google.guava", "guava",
                    CATALOGOS.resolve("guava/catalogo.properties"), "33.2.1-jre",
                    List.of("18.0", "21.0", "24.0-jre", "27.0-jre", "30.0-jre", "33.0.0-jre", "33.2.1-jre",
                            "33.3.0-jre", "33.4.0-jre", "33.5.0-jre", "33.6.0-jre", "33.7.0-jre")),
            new Serie("commons-lang3", "Apache Commons Lang", "org.apache.commons", "commons-lang3",
                    CATALOGOS.resolve("commons-lang3/catalogo.properties"), "3.14.0",
                    List.of("3.1", "3.4", "3.7", "3.10", "3.12.0", "3.14.0", "3.16.0", "3.18.0", "3.20.0",
                            "3.21.0")));

    private EstudiosComplementarios() {
    }

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "todos";
        Files.createDirectories(EVIDENCIA);
        switch (modo) {
            case "umbral" -> umbral();
            case "evolucion" -> evolucion();
            case "oraculo" -> oraculo();
            case "archunit" -> EquivalenteArchUnit.main(new String[0]);
            case "todos" -> {
                umbral();
                evolucion();
                oraculo();
                EquivalenteArchUnit.main(new String[0]);
            }
            default -> throw new IllegalArgumentException("Modo desconocido: " + modo
                    + " (use umbral, evolucion, oraculo, archunit o todos)");
        }
        System.out.println("Evidencia en " + EVIDENCIA.toAbsolutePath());
    }

    // ================================================================ objetivos

    /** Bibliotecas de la sección 4.7: el conjunto con que se calibra el umbral. */
    static List<Objetivo> calibracion() throws Exception {
        return List.of(
                new Objetivo("commons-lang3", "Apache Commons Lang 3.14.0", "calibracion",
                        descargar("org.apache.commons", "commons-lang3", "3.14.0"),
                        CATALOGOS.resolve("commons-lang3/catalogo.properties")),
                new Objetivo("jackson-databind", "Jackson Databind 2.17.2", "calibracion",
                        descargar("com.fasterxml.jackson.core", "jackson-databind", "2.17.2"),
                        CATALOGOS.resolve("jackson-databind/catalogo.properties")),
                new Objetivo("archunit", "ArchUnit 1.3.0", "calibracion", Path.of("lib", "archunit-1.3.0.jar"),
                        Path.of("ejemplos", "archunit", "rules-archunit.properties")),
                new Objetivo("guava", "Guava 33.2.1", "calibracion",
                        descargar("com.google.guava", "guava", "33.2.1-jre"),
                        CATALOGOS.resolve("guava/catalogo.properties")));
    }

    /**
     * Aplicaciones en capas y caso de estudio: un conjunto que no intervino en la
     * calibración. Las aplicaciones se compilan con {@code arquitecta aplicaciones};
     * si falta alguna, se omite y se avisa.
     */
    static List<Objetivo> validacion() {
        List<Objetivo> lista = new ArrayList<>();
        for (String[] app : ExperimentoSistemas.APLICACIONES) {
            Path clases = APLICACIONES.resolve(app[0]).resolve("target/classes");
            if (Files.isDirectory(clases)) {
                lista.add(new Objetivo(app[0], app[1], "validacion", clases,
                        CATALOGOS.resolve(app[0]).resolve("catalogo.properties")));
            } else {
                System.out.println("[aviso] " + app[1] + " no está compilada (" + clases + "); se omite.");
            }
        }
        lista.add(new Objetivo("caso-estudio-c0", "Caso de estudio (C0)", "validacion",
                Path.of("caso-estudio", "c0-base", "target", "classes"), Path.of("caso-estudio", "rules.properties")));
        return lista;
    }

    // ================================================================ umbral

    private static void umbral() throws Exception {
        List<Objetivo> objetivos = new ArrayList<>(calibracion());
        objetivos.addAll(validacion());

        List<String> distribucion = new ArrayList<>(List.of("sistema,conjunto,paquete,ce,ca"));
        Map<Objetivo, List<Integer>> valores = new LinkedHashMap<>();
        Map<Objetivo, ConfiguracionReglas> configs = new LinkedHashMap<>();
        Map<Objetivo, GrafoDependencias> grafos = new LinkedHashMap<>();
        for (Objetivo o : objetivos) {
            ConfiguracionReglas config = CargadorConfiguracion.cargar(o.catalogo());
            GrafoDependencias grafo = new ConstructorGrafo(config.raiz(), config.exclusiones()).desdeRuta(o.ruta());
            List<Integer> ce = new ArrayList<>();
            for (String p : new TreeSet<>(grafo.nodos())) {
                if (config.esCompartido(p)) {
                    continue; // FF3 no los verifica: tampoco cuentan para fijar su umbral.
                }
                ce.add(grafo.acoplamientoEferente(p));
                distribucion.add(String.join(",", o.id(), o.conjunto(), p, "" + grafo.acoplamientoEferente(p),
                        "" + grafo.acoplamientoAferente(p)));
            }
            Collections.sort(ce);
            valores.put(o, ce);
            configs.put(o, config);
            grafos.put(o, grafo);
        }

        // Umbral de calibración: percentil 90 de la distribución conjunta de las
        // bibliotecas, la franja de riesgo «muy alto» en el método de Alves et al.
        List<Integer> conjunta = new ArrayList<>();
        valores.forEach((o, v) -> {
            if (o.conjunto().equals("calibracion")) {
                conjunta.addAll(v);
            }
        });
        Collections.sort(conjunta);
        int p90 = percentil(conjunta, 0.90);

        List<String> resumen = new ArrayList<>(List.of("sistema,conjunto,paquetes,p50,p70,p80,p90,maximo,"
                + "umbral_catalogo,senalados_catalogo,umbral_p90_calibracion,senalados_p90_calibracion,"
                + "umbral_p90_propio,senalados_p90_propio"));
        StringBuilder md = new StringBuilder("# Calibración del umbral de concentración\n\n");
        md.append("Distribución del acoplamiento eferente (Ce) de los paquetes que FF3 verifica (sin los de rol ")
          .append("compartido). Percentiles por rango más cercano.\n\n")
          .append("| Sistema | Conjunto | Paquetes | p50 | p70 | p80 | p90 | Máx. | Umbral del catálogo | ")
          .append("Señalados | Señalados con p90 de calibración (Ce > ").append(p90).append(") |\n")
          .append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (Map.Entry<Objetivo, List<Integer>> e : valores.entrySet()) {
            Objetivo o = e.getKey();
            List<Integer> v = e.getValue();
            int umbralCatalogo = configs.get(o).umbralConcentracion();
            int propio = percentil(v, 0.90);
            long senCat = v.stream().filter(x -> x > umbralCatalogo).count();
            long senCal = v.stream().filter(x -> x > p90).count();
            long senPropio = v.stream().filter(x -> x > propio).count();
            resumen.add(String.join(",", o.id(), o.conjunto(), "" + v.size(),
                    "" + percentil(v, PERCENTILES[0]), "" + percentil(v, PERCENTILES[1]),
                    "" + percentil(v, PERCENTILES[2]), "" + percentil(v, PERCENTILES[3]), "" + v.get(v.size() - 1),
                    "" + umbralCatalogo, "" + senCat, "" + p90, "" + senCal, "" + propio, "" + senPropio));
            md.append(String.format(L, "| %s | %s | %d | %d | %d | %d | %d | %d | %d | %d | %d |%n", o.nombre(),
                    o.conjunto(), v.size(), percentil(v, 0.5), percentil(v, 0.7), percentil(v, 0.8), propio,
                    v.get(v.size() - 1), umbralCatalogo, senCat, senCal));
        }
        md.append(String.format(L, "%nConjunto de calibración: %d paquetes; p50 = %d, p70 = %d, p80 = %d, "
                + "**p90 = %d**.%n", conjunta.size(), percentil(conjunta, 0.5), percentil(conjunta, 0.7),
                percentil(conjunta, 0.8), p90));

        // Sensibilidad: cuántos paquetes señala FF3 en cada sistema según el umbral.
        List<String> sensibilidad = new ArrayList<>(List.of("sistema,conjunto,umbral,senalados,paquetes,porcentaje"));
        for (Map.Entry<Objetivo, List<Integer>> e : valores.entrySet()) {
            List<Integer> v = e.getValue();
            for (int u = 1; u <= Math.max(20, v.get(v.size() - 1)); u++) {
                final int umbral = u;
                long s = v.stream().filter(x -> x > umbral).count();
                sensibilidad.add(String.format(L, "%s,%s,%d,%d,%d,%.2f", e.getKey().id(), e.getKey().conjunto(), u, s,
                        v.size(), 100.0 * s / v.size()));
            }
        }

        escribir("distribucion-ce.csv", distribucion);
        escribir("umbral-ce.csv", resumen);
        escribir("sensibilidad-umbral.csv", sensibilidad);
        Files.writeString(EVIDENCIA.resolve("umbral.md"), md.toString(), StandardCharsets.UTF_8);
        System.out.println(md);
    }

    /** Percentil por rango más cercano: el menor valor que deja por debajo al menos la fracción q. */
    static int percentil(List<Integer> ordenados, double q) {
        if (ordenados.isEmpty()) {
            return 0;
        }
        int rango = (int) Math.ceil(q * ordenados.size());
        return ordenados.get(Math.max(0, Math.min(ordenados.size() - 1, rango - 1)));
    }

    // ================================================================ evolución

    private static void evolucion() throws Exception {
        List<String> filas = new ArrayList<>(List.of("serie,version,posicion,sha1,clases,paquetes,dependencias,"
                + "verificaciones,incumplimientos,inc_capas,inc_ciclos,inc_concentracion,cr,cr_por_regla,"
                + "estado_cr,estado_final,mayor_componente_ciclico,hallazgos_nuevos,hallazgos_resueltos"));
        StringBuilder md = new StringBuilder("# Evolución histórica: deriva real frente a un catálogo declarado\n\n")
                .append("Cada serie aplica, sin modificarlo, el catálogo declarado para la versión de referencia ")
                .append("(sección 4.7) a versiones publicadas antes y después de ella. Las posteriores responden a la ")
                .append("pregunta operativa del mecanismo: si el equipo hubiera declarado esa arquitectura, ¿qué ")
                .append("deriva habría aparecido después? Las anteriores muestran desde cuándo existía lo que hoy se ")
                .append("señala.\n\n");
        for (Serie s : SERIES) {
            md.append("## ").append(s.nombre()).append(" (referencia ").append(s.referencia()).append(")\n\n")
              .append("| Versión | Posición | Clases | Paquetes | CR | CR por regla | Capas | Ciclos (paq.) | ")
              .append("Mayor SCC | Concentración | Estado (CR+sev.) | Nuevos | Resueltos |\n")
              .append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
            StringBuilder cambios = new StringBuilder();
            Set<String> previas = null;
            boolean despues = false;
            for (String version : s.versiones()) {
                Path jar = descargar(s.grupo(), s.artefacto(), version);
                String posicion = version.equals(s.referencia()) ? "referencia" : despues ? "posterior" : "anterior";
                if (version.equals(s.referencia())) {
                    despues = true;
                }
                ResultadoAnalisis r = Analisis.ejecutar(new Analisis.Solicitud(jar, s.catalogo(), "cr+severidad",
                        s.id() + "-" + version, null));
                Resultado res = r.resultado();
                Set<String> claves = claves(res);
                Set<String> nuevos = new TreeSet<>(claves);
                Set<String> resueltos = new TreeSet<>();
                if (previas != null) {
                    nuevos.removeAll(previas);
                    resueltos.addAll(previas);
                    resueltos.removeAll(claves);
                } else {
                    nuevos.clear();
                }
                ConfiguracionReglas config = CargadorConfiguracion.cargar(s.catalogo());
                int scc = mayorComponente(new ConstructorGrafo(config.raiz(), config.exclusiones()).desdeRuta(jar),
                        config.compartidos());
                int clases = ConstructorGrafo.contarPaquetes(jar).entrySet().stream()
                        .filter(e -> e.getKey().equals(config.raiz()) || e.getKey().startsWith(config.raiz() + "."))
                        .filter(e -> !config.excluido(e.getKey()))
                        .mapToInt(Map.Entry::getValue).sum();
                filas.add(String.join(",", s.id(), version, posicion, sha1(jar), "" + clases, "" + r.paquetes(),
                        "" + r.dependencias(), "" + res.verificaciones(), "" + res.incumplimientos(),
                        "" + incumplimientos(res, ReglaIntegridadCapas.NOMBRE),
                        "" + incumplimientos(res, ReglaCiclos.NOMBRE),
                        "" + incumplimientos(res, ReglaConcentracion.NOMBRE),
                        String.format(L, "%.2f", res.conformidad()), String.format(L, "%.2f", res.conformidadPorRegla()),
                        res.estadoPorCR().etiqueta(), res.estado().etiqueta(), "" + scc, "" + nuevos.size(),
                        "" + resueltos.size()));
                md.append(String.format(L, "| %s | %s | %d | %d | %.2f %% | %.2f %% | %d | %d | %d | %d | %s | %d | %d |%n",
                        version, posicion, clases, r.paquetes(), res.conformidad(), res.conformidadPorRegla(),
                        incumplimientos(res, ReglaIntegridadCapas.NOMBRE), incumplimientos(res, ReglaCiclos.NOMBRE), scc,
                        incumplimientos(res, ReglaConcentracion.NOMBRE), res.estado().etiqueta(), nuevos.size(),
                        resueltos.size()));
                if (!nuevos.isEmpty() || !resueltos.isEmpty()) {
                    cambios.append("- **").append(version).append("**\n");
                    nuevos.forEach(k -> cambios.append("  - aparece: ").append(k).append('\n'));
                    resueltos.forEach(k -> cambios.append("  - desaparece: ").append(k).append('\n'));
                }
                System.out.printf(L, "%s %s (%s): CR %.2f %% · por regla %.2f %% · %d hallazgos · SCC %d · +%d −%d%n",
                        s.id(), version, posicion, res.conformidad(), res.conformidadPorRegla(), res.totalViolaciones(),
                        scc, nuevos.size(), resueltos.size());
                previas = claves;
            }
            md.append("\n### Cambios entre versiones consecutivas\n\n").append(cambios.length() == 0
                    ? "Ninguno.\n" : cambios).append('\n');
        }
        escribir("evolucion.csv", filas);
        Files.writeString(EVIDENCIA.resolve("evolucion.md"), md.toString(), StandardCharsets.UTF_8);
    }

    private static int incumplimientos(Resultado res, String regla) {
        return res.porRegla().stream().filter(r -> r.regla().equals(regla))
                .mapToInt(ResultadoRegla::incumplimientos).sum();
    }

    /** Tamaño del mayor componente fuertemente conexo, calculado como lo hace FF2. */
    private static int mayorComponente(GrafoDependencias grafo, List<String> compartidos) {
        GrafoDependencias evaluado = grafo.filtrar(p -> !compartidos.contains(p));
        return new Tarjan(evaluado).componentesFuertementeConexos().stream().mapToInt(List::size).max().orElse(0);
    }

    /** Identidad de cada hallazgo: regla y paquetes implicados (los de un ciclo, ordenados). */
    static Set<String> claves(Resultado r) {
        Set<String> claves = new TreeSet<>();
        for (Violacion v : r.violaciones()) {
            List<String> u = new ArrayList<>(v.ubicacion());
            if (v.regla().equals(ReglaCiclos.NOMBRE)) {
                u = new ArrayList<>(new TreeSet<>(u));
            }
            claves.add(v.regla() + " | " + String.join(" , ", u));
        }
        return claves;
    }

    // ================================================================ oráculo

    /**
     * Contrasta las dependencias entre paquetes que extrae el mecanismo con las
     * que extrae JDepend sobre los mismos binarios. JDepend se ejecuta como
     * proceso aparte y no se incorpora a la herramienta.
     */
    private static void oraculo() throws Exception {
        Path jdepend = descargar("guru.nidi", "jdepend", "2.9.5");
        List<Objetivo> objetivos = new ArrayList<>(calibracion());
        objetivos.addAll(validacion());

        List<String> filas = new ArrayList<>(List.of("sistema,conjunto,paquetes_mecanismo,paquetes_jdepend,"
                + "aristas_mecanismo,aristas_jdepend,aristas_comunes,solo_mecanismo,solo_jdepend,"
                + "paquetes_en_ciclo_mecanismo,paquetes_en_ciclo_jdepend,ciclos_coinciden,"
                + "paquetes_con_ce_igual,paquetes_comparados"));
        StringBuilder md = new StringBuilder("# Oráculo externo: el grafo del mecanismo frente al de JDepend\n\n")
                .append("JDepend 2.9.5 (guru.nidi) se ejecuta sobre los mismos binarios. Se comparan solo los paquetes ")
                .append("del sistema (bajo la raíz y fuera de las exclusiones del catálogo) y las dependencias entre ")
                .append("ellos. Los componentes cíclicos se calculan con el mismo algoritmo sobre ambos grafos, sin ")
                .append("las exenciones, para que la comparación aísle la extracción.\n\n")
                .append("| Sistema | Aristas mecanismo | Aristas JDepend | Comunes | Solo mecanismo | Solo JDepend | ")
                .append("Paquetes en ciclo (mec./JD) | Ce igual |\n| --- | --- | --- | --- | --- | --- | --- | --- |\n");
        StringBuilder diferencias = new StringBuilder();
        for (Objetivo o : objetivos) {
            ConfiguracionReglas config = CargadorConfiguracion.cargar(o.catalogo());
            GrafoDependencias propio = new ConstructorGrafo(config.raiz(), config.exclusiones()).desdeRuta(o.ruta());
            GrafoDependencias externo = jdepend(jdepend, o.ruta(), config);

            Set<String> ap = aristas(propio);
            Set<String> ae = aristas(externo);
            Set<String> comunes = new TreeSet<>(ap);
            comunes.retainAll(ae);
            Set<String> soloPropio = new TreeSet<>(ap);
            soloPropio.removeAll(ae);
            Set<String> soloExterno = new TreeSet<>(ae);
            soloExterno.removeAll(ap);

            Set<String> cicloPropio = enCiclo(propio);
            Set<String> cicloExterno = enCiclo(externo);
            Set<String> nodos = new TreeSet<>(propio.nodos());
            nodos.retainAll(externo.nodos());
            long ceIgual = nodos.stream()
                    .filter(p -> propio.acoplamientoEferente(p) == externo.acoplamientoEferente(p)).count();

            filas.add(String.join(",", o.id(), o.conjunto(), "" + propio.nodos().size(), "" + externo.nodos().size(),
                    "" + ap.size(), "" + ae.size(), "" + comunes.size(), "" + soloPropio.size(),
                    "" + soloExterno.size(), "" + cicloPropio.size(), "" + cicloExterno.size(),
                    "" + cicloPropio.equals(cicloExterno), "" + ceIgual, "" + nodos.size()));
            md.append(String.format(L, "| %s | %d | %d | %d | %d | %d | %d / %d | %d de %d |%n", o.nombre(), ap.size(),
                    ae.size(), comunes.size(), soloPropio.size(), soloExterno.size(), cicloPropio.size(),
                    cicloExterno.size(), ceIgual, nodos.size()));
            if (!soloPropio.isEmpty() || !soloExterno.isEmpty()) {
                diferencias.append("## ").append(o.nombre()).append("\n\n");
                for (String a : soloPropio) {
                    String[] par = a.split(" -> ");
                    List<String> ev = propio.evidenciaDe(par[0], par[1]);
                    diferencias.append("- solo mecanismo: `").append(a).append("` — ")
                            .append(ev.isEmpty() ? "" : ev.subList(0, Math.min(2, ev.size()))).append('\n');
                }
                soloExterno.forEach(a -> diferencias.append("- solo JDepend: `").append(a).append("`\n"));
                diferencias.append('\n');
            }
            System.out.printf(L, "%s: %d comunes · %d solo mecanismo · %d solo JDepend · ciclos %d/%d%n", o.id(),
                    comunes.size(), soloPropio.size(), soloExterno.size(), cicloPropio.size(), cicloExterno.size());
        }
        md.append("\n# Diferencias, arista por arista\n\n").append(diferencias.length() == 0 ? "Ninguna.\n" : diferencias);
        escribir("oraculo-jdepend.csv", filas);
        Files.writeString(EVIDENCIA.resolve("oraculo-jdepend.md"), md.toString(), StandardCharsets.UTF_8);
    }

    /** Ejecuta JDepend sobre la ruta y devuelve su grafo restringido a los paquetes del sistema. */
    private static GrafoDependencias jdepend(Path jdepend, Path ruta, ConfiguracionReglas config) throws Exception {
        Path xml = Files.createTempFile("jdepend", ".xml");
        Path entrada = Files.isDirectory(ruta) ? ruta : extraerClases(ruta);
        try {
            Process p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", jdepend.toString(), "jdepend.xmlui.JDepend", "-file", xml.toString(),
                    entrada.toAbsolutePath().toString())
                    .redirectErrorStream(true).start();
            String salida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(5, TimeUnit.MINUTES) || p.exitValue() != 0) {
                throw new IllegalStateException("JDepend falló sobre " + ruta + ":\n" + salida);
            }
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = f.newDocumentBuilder().parse(xml.toFile());
            GrafoDependencias grafo = new GrafoDependencias();
            Element paquetes = (Element) doc.getElementsByTagName("Packages").item(0);
            for (Node n = paquetes.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (!(n instanceof Element pkg) || !pkg.getTagName().equals("Package")) {
                    continue;
                }
                String nombre = pkg.getAttribute("name");
                if (!delSistema(nombre, config)) {
                    continue;
                }
                if (pkg.getElementsByTagName("Stats").getLength() == 0) {
                    continue; // paquete referenciado pero sin clases en la ruta analizada
                }
                grafo.agregarNodo(nombre);
                NodeList ef = pkg.getElementsByTagName("DependsUpon");
                if (ef.getLength() == 0) {
                    continue;
                }
                NodeList destinos = ((Element) ef.item(0)).getElementsByTagName("Package");
                for (int i = 0; i < destinos.getLength(); i++) {
                    String d = destinos.item(i).getTextContent().strip();
                    if (delSistema(d, config) && !d.equals(nombre)) {
                        grafo.agregarArista(nombre, d);
                    }
                }
            }
            // Un destino del sistema solo cuenta si tiene clases en la ruta analizada.
            return grafo.filtrar(grafo::existeNodo);
        } finally {
            Files.deleteIfExists(xml);
            if (!entrada.equals(ruta)) {
                borrar(entrada);
            }
        }
    }

    /**
     * Extrae las clases de un {@code .jar} a un directorio temporal, sin
     * {@code module-info.class} ni {@code META-INF/}. JDepend 2.9.5 aborta el
     * análisis de todo el archivo al encontrar la constante {@code CONSTANT_Module}
     * (Java 9) de un descriptor de módulo, y las variantes de
     * {@code META-INF/versions} duplicarían clases que el mecanismo no analiza.
     */
    private static Path extraerClases(Path jar) throws IOException {
        Path destino = Files.createTempDirectory("jdepend-clases");
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
            for (java.util.zip.ZipEntry e : Collections.list(zip.entries())) {
                String n = e.getName();
                if (e.isDirectory() || !n.endsWith(".class") || n.startsWith("META-INF/")
                        || n.endsWith("module-info.class")) {
                    continue;
                }
                Path f = destino.resolve(n);
                Files.createDirectories(f.getParent());
                try (InputStream in = zip.getInputStream(e)) {
                    Files.copy(in, f);
                }
            }
        }
        return destino;
    }

    private static void borrar(Path dir) throws IOException {
        try (java.util.stream.Stream<Path> s = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) s.sorted(java.util.Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static boolean delSistema(String paquete, ConfiguracionReglas config) {
        return (paquete.equals(config.raiz()) || paquete.startsWith(config.raiz() + ".")) && !config.excluido(paquete);
    }

    private static Set<String> aristas(GrafoDependencias g) {
        Set<String> s = new TreeSet<>();
        for (String[] a : g.aristas()) {
            if (!a[0].equals(a[1]) && g.existeNodo(a[1])) {
                s.add(a[0] + " -> " + a[1]);
            }
        }
        return s;
    }

    private static Set<String> enCiclo(GrafoDependencias g) {
        Set<String> s = new TreeSet<>();
        for (List<String> c : new Tarjan(g).componentesFuertementeConexos()) {
            if (c.size() > 1) {
                s.addAll(c);
            }
        }
        return s;
    }

    // ================================================================ utilidades

    /**
     * Descarga un artefacto de Maven Central si no está ya en disco y comprueba
     * su SHA-1 contra el publicado. Devuelve la ruta local.
     */
    static Path descargar(String grupo, String artefacto, String version) throws Exception {
        Files.createDirectories(DESCARGAS);
        String archivo = artefacto + "-" + version + ".jar";
        Path destino = DESCARGAS.resolve(archivo);
        String url = "https://repo1.maven.org/maven2/" + grupo.replace('.', '/') + "/" + artefacto + "/" + version
                + "/" + archivo;
        Path sha1Local = DESCARGAS.resolve(archivo + ".sha1");
        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        if (!Files.exists(sha1Local)) {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url + ".sha1")).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                throw new IOException("No se pudo obtener el SHA-1 de " + url + " (HTTP " + r.statusCode() + ")");
            }
            Files.writeString(sha1Local, r.body().strip().split("\\s+")[0]);
        }
        String esperado = Files.readString(sha1Local).strip();
        if (!Files.exists(destino)) {
            System.out.println("Descargando " + archivo + " desde Maven Central…");
            Path tmp = Files.createTempFile(DESCARGAS, archivo, ".part");
            HttpResponse<InputStream> r = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (r.statusCode() != 200) {
                throw new IOException("No se pudo descargar " + url + " (HTTP " + r.statusCode() + ")");
            }
            try (InputStream in = r.body()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!sha1(destino).equals(esperado)) {
            Files.delete(destino);
            throw new IOException("El SHA-1 de " + archivo + " no coincide con el publicado en Maven Central.");
        }
        return destino;
    }

    static String sha1(Path archivo) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return HexFormat.of().formatHex(md.digest(Files.readAllBytes(archivo)));
    }

    private static void escribir(String nombre, List<String> filas) throws IOException {
        Files.writeString(EVIDENCIA.resolve(nombre), String.join("\n", filas) + "\n", StandardCharsets.UTF_8);
    }
}
