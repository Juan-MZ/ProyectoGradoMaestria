package co.javeriana.msw.arquitecta.reglas;

import co.javeriana.msw.arquitecta.grafo.BuscadorCiclos;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.grafo.Tarjan;

import java.util.ArrayList;
import java.util.List;

/**
 * <em>Fitness Function</em> de ausencia de ciclos.
 *
 * <p>Aplica el algoritmo de Tarjan sobre el grafo de dependencias: todo
 * componente fuertemente conexo con más de un paquete evidencia un ciclo. Cada
 * paquete atrapado en un ciclo cuenta como un incumplimiento, de modo que la
 * penalización crece con la extensión del enredo y no solo con su existencia.</p>
 *
 * <p>Los paquetes exentos se retiran del grafo antes del análisis. La razón es
 * topológica: un núcleo de anotaciones al que todo el sistema referencia actúa
 * como puente y une en un mismo componente a paquetes que, entre sí, no se
 * alcanzan. Evaluar sin él no oculta los ciclos reales ---siguen apareciendo, y
 * más pequeños--- sino que deja de atribuir a otros módulos un enredo que
 * procede de un paquete cuyo cometido es, precisamente, ser universal.</p>
 */
public class ReglaCiclos implements Regla {

    /** Nombre de la regla, con el que se identifican sus hallazgos. */
    public static final String NOMBRE = "Ciclos de dependencia";

    private final Exenciones exentos;

    public ReglaCiclos() {
        this(Exenciones.NINGUNA);
    }

    public ReglaCiclos(Exenciones exentos) {
        this.exentos = exentos == null ? Exenciones.NINGUNA : exentos;
    }

    @Override
    public String nombre() {
        return NOMBRE;
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
        return "Cada paquete del grafo no exento.";
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
        GrafoDependencias evaluado = exentos.vacias()
                ? grafo
                : grafo.filtrar(paquete -> !exentos.cubre(paquete));

        List<Violacion> violaciones = new ArrayList<>();
        int incumplimientos = 0;

        for (List<String> componente : new Tarjan(evaluado).componentesFuertementeConexos()) {
            if (componente.size() <= 1) {
                continue;
            }
            incumplimientos += componente.size();

            List<String> evidencia = new ArrayList<>();
            List<String> ruta = BuscadorCiclos.rutaDe(evaluado, componente);
            if (!ruta.isEmpty()) {
                evidencia.add("Ruta del ciclo: " + String.join(" -> ", ruta));
            }
            evidencia.add("Paquetes del componente: " + String.join(", ", componente));
            for (String origen : componente) {
                for (String destino : evaluado.destinosDe(origen)) {
                    if (componente.contains(destino)) {
                        evidencia.addAll(evaluado.evidenciaDe(origen, destino));
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
        return new ResultadoRegla(nombre(), evaluado.nodos().size(), incumplimientos, violaciones);
    }
}
