package co.javeriana.msw.arquitecta.reglas;

import java.util.ArrayList;
import java.util.List;

import co.javeriana.msw.arquitecta.grafo.BuscadorCiclos;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.grafo.Tarjan;

/**
 * <em>Fitness Function</em> de ausencia de ciclos.
 *
 * <p>Aplica el algoritmo de Tarjan sobre el grafo de dependencias: todo
 * componente fuertemente conexo con más de un paquete evidencia un ciclo. Cada
 * paquete atrapado en un ciclo cuenta como un incumplimiento, de modo que la
 * penalización crece con la extensión del enredo y no solo con su existencia.</p>
 */
public class ReglaCiclos implements Regla {

    @Override
    public String nombre() {
        return "Ciclos de dependencia";
    }

    @Override
    public String propiedad() {
        return "El grafo de dependencias entre paquetes es acíclico.";
    }

    @Override
    public String racionalidad() {
        return "Los paquetes de un ciclo no pueden compilarse, probarse, versionarse ni "
                + "comprenderse por separado: forman una unidad de cambio implícita que "
                + "ninguna decisión de diseño declaró.";
    }

    @Override
    public String unidadVerificacion() {
        return "Cada paquete del grafo.";
    }

    @Override
    public String criterioIncumplimiento() {
        return "El paquete pertenece a un componente fuertemente conexo de tamaño mayor que uno.";
    }

    @Override
    public Violacion.Severidad severidad() {
        return Violacion.Severidad.CRITICA;
    }

    @Override
    public ResultadoRegla evaluar(GrafoDependencias grafo) {
        List<Violacion> violaciones = new ArrayList<>();
        int incumplimientos = 0;

        for (List<String> componente : new Tarjan(grafo).componentesFuertementeConexos()) {
            if (componente.size() <= 1) {
                continue;
            }
            incumplimientos += componente.size();

            List<String> evidencia = new ArrayList<>();
            List<String> ruta = BuscadorCiclos.rutaDe(grafo, componente);
            if (!ruta.isEmpty()) {
                evidencia.add("Ruta del ciclo: " + String.join(" -> ", ruta));
            }
            evidencia.add("Paquetes del componente: " + String.join(", ", componente));
            for (String origen : componente) {
                for (String destino : grafo.destinosDe(origen)) {
                    if (componente.contains(destino)) {
                        evidencia.addAll(grafo.evidenciaDe(origen, destino));
                    }
                }
            }

            violaciones.add(new Violacion(
                    nombre(),
                    "Ciclo de dependencia entre " + componente.size() + " paquetes.",
                    List.copyOf(componente),
                    evidencia,
                    severidad()));
        }
        return new ResultadoRegla(nombre(), grafo.nodos().size(), incumplimientos, violaciones);
    }
}
