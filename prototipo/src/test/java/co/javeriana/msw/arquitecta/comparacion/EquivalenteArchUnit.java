package co.javeriana.msw.arquitecta.comparacion;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import co.javeriana.msw.arquitecta.servicio.Analisis;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Las tres reglas del caso de estudio escritas directamente con ArchUnit, la
 * forma en que hoy se expresan las <em>Fitness Functions</em> estructurales en
 * Java. Sirve para contrastar con datos qué aporta el mecanismo frente a la
 * herramienta sobre la que se apoya: se comparan el esfuerzo de especificación
 * (líneas de cada forma), lo que cada una detecta en C0 a C4 y lo que cada una
 * entrega como resultado.
 *
 * <p>El bloque entre las marcas {@code ESPECIFICACION} es la especificación que
 * se cuenta; el resto es la infraestructura del contraste. Uso, desde
 * {@code prototipo/}: {@code arquitecta estudios archunit}.</p>
 */
public final class EquivalenteArchUnit {

    private static final String[] ESCENARIOS = {"c0-base", "c1-capas", "c2-ciclo", "c3-concentracion", "c4-combinado"};
    private static final Path SALIDA = Path.of("ejemplos", "archunit", "equivalente-archunit.md");

    private EquivalenteArchUnit() {
    }

    // ESPECIFICACION-INICIO
    static ArchRule capas() {
        return layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("presentacion").definedBy("com.caso.presentacion..")
                .layer("negocio").definedBy("com.caso.negocio..")
                .layer("persistencia").definedBy("com.caso.persistencia..")
                .whereLayer("presentacion").mayNotBeAccessedByAnyLayer()
                .whereLayer("negocio").mayOnlyBeAccessedByLayers("presentacion")
                .whereLayer("persistencia").mayOnlyBeAccessedByLayers("negocio");
    }

    static ArchRule ciclos() {
        return slices().matching("com.caso.(**)").should().beFreeOfCycles();
    }

    /** ArchUnit no ofrece una regla de acoplamiento eferente: se programa. */
    static List<String> concentracion(JavaClasses clases, int umbral) {
        List<String> infractores = new ArrayList<>();
        for (JavaPackage p : clases.getPackage("com.caso").getSubpackagesInTree()) {
            Set<String> destinos = new TreeSet<>();
            for (JavaClass c : p.getClasses()) {
                c.getDirectDependenciesFromSelf().forEach(d -> {
                    String destino = d.getTargetClass().getBaseComponentType().getPackageName();
                    if (destino.startsWith("com.caso") && !destino.equals(p.getName())) {
                        destinos.add(destino);
                    }
                });
            }
            if (destinos.size() > umbral) {
                infractores.add(p.getName() + " (Ce = " + destinos.size() + ")");
            }
        }
        return infractores;
    }
    // ESPECIFICACION-FIN

    public static void main(String[] args) throws Exception {
        StringBuilder md = new StringBuilder("# Las reglas del caso de estudio escritas con ArchUnit\n\n")
                .append("Generado por `EquivalenteArchUnit` (`arquitecta estudios archunit`). Compara, sobre los cinco ")
                .append("escenarios, lo que detectan las tres reglas escritas con ArchUnit y lo que detecta el ")
                .append("mecanismo con `caso-estudio/rules.properties`.\n\n")
                .append("| Escenario | ArchUnit: capas | ArchUnit: ciclos | ArchUnit: concentración | ")
                .append("Mecanismo: hallazgos | Mecanismo: CR | Mecanismo: estado |\n")
                .append("| --- | --- | --- | --- | --- | --- | --- |\n");
        boolean coinciden = true;
        for (String e : ESCENARIOS) {
            Path ruta = Path.of("caso-estudio", e, "target", "classes");
            JavaClasses clases = new ClassFileImporter().importPath(ruta);
            EvaluationResult rc = capas().evaluate(clases);
            EvaluationResult ry = ciclos().evaluate(clases);
            List<String> conc = concentracion(clases, 3);
            ResultadoAnalisis r = Analisis.ejecutar(new Analisis.Solicitud(ruta,
                    Path.of("caso-estudio", "rules.properties"), "cr+severidad", e, null));
            long capasMec = r.resultado().violaciones().stream().filter(v -> v.regla().startsWith("Integridad")).count();
            long ciclosMec = r.resultado().violaciones().stream().filter(v -> v.regla().startsWith("Ciclos")).count();
            long concMec = r.resultado().violaciones().stream().filter(v -> v.regla().startsWith("Concentración")).count();
            // ArchUnit reporta violaciones por clase y llamada; lo comparable es si cada regla falla.
            coinciden &= rc.hasViolation() == (capasMec > 0) && ry.hasViolation() == (ciclosMec > 0)
                    && !conc.isEmpty() == (concMec > 0);
            md.append(String.format(Locale.ROOT, "| %s | %s | %s | %s | %d | %.2f %% | %s |%n", e,
                    rc.hasViolation() ? "falla (" + rc.getFailureReport().getDetails().size() + " líneas)" : "pasa",
                    ry.hasViolation() ? "falla (" + ry.getFailureReport().getDetails().size() + " líneas)" : "pasa",
                    conc.isEmpty() ? "pasa" : "falla: " + String.join(", ", conc),
                    r.resultado().totalViolaciones(), r.resultado().conformidad(), r.resultado().estado().etiqueta()));
        }
        Path fuente = Path.of("src", "test", "java", "co", "javeriana", "msw", "arquitecta", "comparacion",
                "EquivalenteArchUnit.java");
        int lineasArchUnit = lineasDeEspecificacion(Files.readAllLines(fuente, StandardCharsets.UTF_8), true);
        int lineasCatalogo = lineasDeEspecificacion(
                Files.readAllLines(Path.of("caso-estudio", "rules.properties"), StandardCharsets.UTF_8), false);
        md.append(String.format(Locale.ROOT, "%nDetección por regla coincidente en los cinco escenarios: **%s**.%n%n",
                coinciden ? "sí" : "no"))
          .append("| Forma | Líneas de especificación | Lenguaje | Resultado que entrega |\n| --- | --- | --- | --- |\n")
          .append(String.format(Locale.ROOT, "| ArchUnit | %d | Java (API fluida y código propio para Ce) | ", lineasArchUnit))
          .append("pasa/falla por regla, con la lista de accesos infractores |\n")
          .append(String.format(Locale.ROOT, "| Catálogo del mecanismo | %d | propiedades clave=valor | ", lineasCatalogo))
          .append("CR, estado con severidad, evidencia por hallazgo, alcance del diagnóstico, tendencia |\n\n")
          .append("Se cuentan las líneas no vacías y sin comentarios: en ArchUnit, las del bloque de ")
          .append("especificación de `EquivalenteArchUnit.java`; en el catálogo, las de `rules.properties`.\n");
        Files.writeString(SALIDA, md.toString(), StandardCharsets.UTF_8);
        System.out.println(md);
    }

    private static int lineasDeEspecificacion(List<String> lineas, boolean java) {
        int n = 0;
        boolean dentro = !java;
        for (String l : lineas) {
            String t = l.strip();
            if (java && t.contains("ESPECIFICACION-INICIO")) {
                dentro = true;
                continue;
            }
            if (java && t.contains("ESPECIFICACION-FIN")) {
                dentro = false;
            }
            if (!dentro || t.isEmpty() || t.startsWith("#") || t.startsWith("//") || t.startsWith("/**")
                    || t.startsWith("*")) {
                continue;
            }
            n++;
        }
        return n;
    }
}
