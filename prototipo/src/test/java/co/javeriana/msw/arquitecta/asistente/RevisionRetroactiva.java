package co.javeriana.msw.arquitecta.asistente;

import co.javeriana.msw.arquitecta.servicio.Asistencia;
import co.javeriana.msw.arquitecta.servicio.Perfil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aplica la revisión determinista vigente a las propuestas ya registradas por
 * {@link EvaluacionAsistente}, sin volver a consultar ningún modelo. La revisión
 * solo usa el umbral y las exenciones de la propuesta, que la tabla de
 * resultados conserva, de modo que su efecto puede medirse a posteriori.
 *
 * <p>Uso: {@code ... RevisionRetroactiva <carpeta> [<carpeta>...]}, con carpetas
 * de {@code ejemplos/asistente/}. Escribe {@code revision.csv} en cada una.</p>
 */
public final class RevisionRetroactiva {

    private static final Map<String, String> CARPETAS = Map.of("C0", "c0-base", "C1", "c1-capas", "C2", "c2-ciclo",
            "C3", "c3-concentracion", "C4", "c4-combinado");

    private RevisionRetroactiva() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, Perfil> perfiles = new HashMap<>();
        for (String carpeta : args) {
            Path dir = Path.of("ejemplos", "asistente", carpeta);
            List<String> lineas = Files.readAllLines(dir.resolve("resultados.csv"), StandardCharsets.UTF_8);
            List<String> salida = new ArrayList<>(List.of("escenario,repeticion,aviso_umbral,aviso_exencion,derivas_ocultas"));
            int umbral = 0;
            int exencion = 0;
            int validas = 0;
            int ocultasAvisadas = 0;
            int conOcultas = 0;
            for (String l : lineas.subList(1, lineas.size())) {
                List<String> c = celdas(l);
                if (!c.get(3).equals("si")) {
                    continue;
                }
                validas++;
                if (!perfiles.containsKey(c.get(0))) {
                    perfiles.put(c.get(0), Asistencia.perfil(Path.of("caso-estudio", CARPETAS.get(c.get(0)))));
                }
                Perfil perfil = perfiles.get(c.get(0));
                List<Propuesta.Exencion> ex = new ArrayList<>();
                if (!c.get(8).isBlank()) {
                    for (String x : c.get(8).split("\\|")) {
                        String[] par = x.split(":", 2);
                        ex.add(new Propuesta.Exencion("com.caso." + par[1], par[0], ""));
                    }
                }
                Propuesta p = new Propuesta("com.caso", List.of(), Map.of(), Integer.valueOf(c.get(7)), "", ex,
                        List.of(), List.of());
                List<String> avisos = Revision.avisos(p, perfil);
                boolean au = avisos.stream().anyMatch(a -> a.startsWith("Con el umbral"));
                boolean ae = avisos.stream().anyMatch(a -> a.startsWith("La propuesta exime"));
                umbral += au ? 1 : 0;
                exencion += ae ? 1 : 0;
                String ocultas = c.get(11);
                if (!ocultas.isBlank()) {
                    conOcultas++;
                    ocultasAvisadas += (au || ae) ? 1 : 0;
                }
                salida.add(String.join(",", c.get(0), c.get(1), au ? "si" : "no", ae ? "si" : "no",
                        "\"" + ocultas + "\""));
            }
            Files.writeString(dir.resolve("revision.csv"), String.join("\n", salida) + "\n", StandardCharsets.UTF_8);
            System.out.printf("%s: %d válidas · aviso de umbral %d · aviso de exención %d · corridas con derivas"
                    + " ocultas %d, avisadas %d%n", carpeta, validas, umbral, exencion, conOcultas, ocultasAvisadas);
        }
    }

    /** Separa una línea CSV respetando los campos entrecomillados. */
    private static List<String> celdas(String linea) {
        List<String> r = new ArrayList<>();
        StringBuilder a = new StringBuilder();
        boolean comillas = false;
        for (char ch : linea.toCharArray()) {
            if (ch == '"') {
                comillas = !comillas;
            } else if (ch == ',' && !comillas) {
                r.add(a.toString());
                a.setLength(0);
            } else {
                a.append(ch);
            }
        }
        r.add(a.toString());
        while (r.size() < 18) {
            r.add("");
        }
        return r;
    }
}
