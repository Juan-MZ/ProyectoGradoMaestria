package co.javeriana.msw.arquitecta.reglas;

import java.util.ArrayList;
import java.util.List;

import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;

/**
 * <em>Fitness Function</em> de integridad de capas.
 *
 * <p>Verifica cada dependencia que cruza la frontera entre dos capas declaradas
 * y comprueba que el par origen-destino esté autorizado por la matriz de
 * dependencias permitidas. Los paquetes no asignados a ninguna capa —por
 * ejemplo, un dominio compartido— quedan deliberadamente fuera de la regla: al
 * no pertenecer a la estratificación, no puede afirmarse que la vulneren.</p>
 */
public class ReglaIntegridadCapas implements Regla {

    private final ConfiguracionReglas config;

    public ReglaIntegridadCapas(ConfiguracionReglas config) {
        this.config = config;
    }

    @Override
    public String nombre() {
        return "Integridad de capas";
    }

    @Override
    public String propiedad() {
        return "La estratificación declarada se respeta: ninguna capa depende de otra "
                + "que la matriz de adyacencias no autorice.";
    }

    @Override
    public String racionalidad() {
        return "Una dependencia que salta o invierte capas anula la separación de "
                + "responsabilidades, propaga los cambios fuera de su ámbito previsto y "
                + "obliga a razonar sobre el sistema completo para modificar una parte.";
    }

    @Override
    public String unidadVerificacion() {
        return "Cada arista del grafo cuyo origen y destino pertenezcan a capas declaradas distintas.";
    }

    @Override
    public String criterioIncumplimiento() {
        return "El par (capa origen, capa destino) no figura en la matriz de dependencias permitidas.";
    }

    @Override
    public Violacion.Severidad severidad() {
        return Violacion.Severidad.MAYOR;
    }

    @Override
    public ResultadoRegla evaluar(GrafoDependencias grafo) {
        int verificaciones = 0;
        List<Violacion> violaciones = new ArrayList<>();

        for (String origen : grafo.nodos()) {
            String capaOrigen = config.capaDe(origen);
            if (capaOrigen == null) {
                continue;
            }
            for (String destino : grafo.destinosDe(origen)) {
                String capaDestino = config.capaDe(destino);
                if (capaDestino == null || capaDestino.equals(capaOrigen)) {
                    continue;
                }
                verificaciones++;
                if (config.permitido(capaOrigen, capaDestino)) {
                    continue;
                }
                List<String> evidencia = new ArrayList<>();
                evidencia.add("Arista: " + origen + " -> " + destino);
                evidencia.add("Capas : " + capaOrigen + " -> " + capaDestino
                        + " (permitidas desde '" + capaOrigen + "': " + config.destinosPermitidos(capaOrigen) + ")");
                evidencia.addAll(grafo.evidenciaDe(origen, destino));
                violaciones.add(new Violacion(
                        nombre(),
                        "Dependencia no permitida entre capas: " + capaOrigen + " -> " + capaDestino,
                        List.of(origen, destino),
                        evidencia,
                        severidad()));
            }
        }
        return new ResultadoRegla(nombre(), verificaciones, violaciones.size(), violaciones);
    }
}
