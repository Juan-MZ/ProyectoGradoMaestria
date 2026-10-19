package co.javeriana.msw.arquitecta;

import co.javeriana.msw.arquitecta.config.Capa;
import co.javeriana.msw.arquitecta.config.CargadorConfiguracion;
import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.config.ModoClasificacion;
import co.javeriana.msw.arquitecta.grafo.BuscadorCiclos;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.grafo.Tarjan;
import co.javeriana.msw.arquitecta.motor.EstadoArquitectonico;
import co.javeriana.msw.arquitecta.motor.MotorConformidad;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.Exenciones;
import co.javeriana.msw.arquitecta.reglas.Regla;
import co.javeriana.msw.arquitecta.reglas.ReglaCiclos;
import co.javeriana.msw.arquitecta.reglas.ReglaConcentracion;
import co.javeriana.msw.arquitecta.reglas.ReglaIntegridadCapas;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo;
import co.javeriana.msw.arquitecta.reporte.ExportadorCsv;
import co.javeriana.msw.arquitecta.reporte.ExportadorDot;
import co.javeriana.msw.arquitecta.reporte.ExportadorJson;
import co.javeriana.msw.arquitecta.reporte.ExportadorSvg;
import co.javeriana.msw.arquitecta.reporte.ExportadorTikz;
import co.javeriana.msw.arquitecta.reporte.Historico;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Bateria de pruebas del nucleo del mecanismo.
 *
 * <p>Verifica el motor sobre grafos construidos a mano, con independencia de la
 * extraccion por bytecode. Separar ambas etapas evita confundir un defecto propio
 * con un comportamiento de la biblioteca de analisis estatico, y permite
 * ejercitar casos limite —grafos vacios, umbrales en su frontera exacta,
 * configuraciones invalidas— que un caso de estudio real dificilmente ofrece.</p>
 */
public final class PruebasArquitecta {

    private static final double EPS = 0.001;

    public static void main(String[] args) throws Exception {
        Verificador v = new Verificador();

        pruebasGrafo(v);
        pruebasTarjan(v);
        pruebasConfiguracion(v);
        pruebasReglaCapas(v);
        pruebasReglaCiclos(v);
        pruebasReglaConcentracion(v);
        pruebasMotor(v);
        pruebasExenciones(v);
        pruebasEscenarios(v);
        pruebasExportadores(v);
        pruebasVisualizacion(v);
        pruebasHistorico(v);
        PruebasExtraccion.ejecutar(v);
        PruebasServicio.ejecutar(v);
        co.javeriana.msw.arquitecta.asistente.PruebasAsistente.ejecutar(v);

        System.exit(v.resumen());
    }

    // ------------------------------------------------------------------ grafo

    private static void pruebasGrafo(Verificador v) {
        v.grupo("Grafo de dependencias");

        GrafoDependencias g = new GrafoDependencias();
        g.agregarArista("a", "b");
        g.agregarArista("a", "b");
        v.igual("una arista repetida no se duplica", 1, g.cantidadAristas());

        g.agregarArista("a", "a");
        v.igual("el autobucle se descarta", 1, g.cantidadAristas());
        v.igual("el autobucle no altera el acoplamiento eferente", 1, g.acoplamientoEferente("a"));

        g.agregarArista("c", "b");
        v.igual("el acoplamiento aferente cuenta los dependientes", 2, g.acoplamientoAferente("b"));
        v.igual("un nodo sin dependientes tiene Ca cero", 0, g.acoplamientoAferente("a"));
        v.igual("los nodos se registran una sola vez", 3, g.nodos().size());

        g.agregarArista("a", "b", "Field <A.b> has type <B> in (A.java:12)");
        v.comprueba("la evidencia de la arista se conserva",
                g.evidenciaDe("a", "b").contains("Field <A.b> has type <B> in (A.java:12)"));
        v.comprueba("una arista inexistente no tiene evidencia", g.evidenciaDe("b", "a").isEmpty());
        v.comprueba("los destinos son de solo lectura",
                esInmutable(() -> g.destinosDe("a").add("z")));

        GrafoDependencias filtrado = g.filtrar(n -> !n.equals("c"));
        v.igual("el filtrado retira el nodo excluido", 2, filtrado.nodos().size());
        v.comprueba("el filtrado retira sus aristas", !filtrado.existeNodo("c"));
        v.igual("el filtrado conserva las aristas supervivientes", 1, filtrado.cantidadAristas());
        v.comprueba("el filtrado conserva la evidencia",
                filtrado.evidenciaDe("a", "b").contains("Field <A.b> has type <B> in (A.java:12)"));
        v.igual("el grafo original no se altera al filtrar", 3, g.nodos().size());
    }

    private static void pruebasTarjan(Verificador v) {
        v.grupo("Deteccion de componentes fuertemente conexos");

        GrafoDependencias acic = new GrafoDependencias();
        acic.agregarArista("a", "b");
        acic.agregarArista("b", "c");
        v.comprueba("un grafo aciclico no produce componentes de tamano mayor a uno",
                new Tarjan(acic).componentesFuertementeConexos().stream().noneMatch(c -> c.size() > 1));

        GrafoDependencias ciclico = new GrafoDependencias();
        ciclico.agregarArista("a", "b");
        ciclico.agregarArista("b", "c");
        ciclico.agregarArista("c", "a");
        ciclico.agregarArista("d", "a");
        List<List<String>> comps = new Tarjan(ciclico).componentesFuertementeConexos();
        List<String> ciclo = comps.stream().filter(c -> c.size() > 1).findFirst().orElse(List.of());
        v.igual("el ciclo de tres nodos se detecta completo", 3, ciclo.size());
        v.comprueba("el nodo externo al ciclo no se incluye", !ciclo.contains("d"));

        List<String> ruta = BuscadorCiclos.rutaDe(ciclico, ciclo);
        v.comprueba("la ruta del ciclo se cierra sobre si misma",
                !ruta.isEmpty() && ruta.get(0).equals(ruta.get(ruta.size() - 1)));
        v.igual("la ruta recorre los tres paquetes y regresa", 4, ruta.size());

        GrafoDependencias vacio = new GrafoDependencias();
        v.comprueba("un grafo vacio no produce componentes",
                new Tarjan(vacio).componentesFuertementeConexos().isEmpty());
    }

    // ---------------------------------------------------------- configuracion

    private static void pruebasConfiguracion(Verificador v) throws IOException {
        v.grupo("Carga del catalogo de reglas");

        ConfiguracionReglas config = cargar(
                "raiz=com.caso\n"
                + "capas=presentacion,negocio\n"
                + "capa.presentacion=com.caso.presentacion\n"
                + "capa.negocio=com.caso.negocio\n"
                + "permitido.presentacion=negocio\n"
                + "permitido.negocio=\n"
                + "umbral.concentracion=3\n");
        v.igual("la raiz se lee", "com.caso", config.raiz());
        v.igual("las capas se leen en orden", List.of("presentacion", "negocio"), config.nombresCapas());
        v.comprueba("la matriz autoriza presentacion hacia negocio", config.permitido("presentacion", "negocio"));
        v.comprueba("la matriz rechaza negocio hacia presentacion", !config.permitido("negocio", "presentacion"));
        v.igual("un paquete sin capa devuelve nulo", null, config.capaDe("com.caso.dominio"));
        v.igual("un subpaquete hereda la capa por prefijo",
                "negocio", config.capaDe("com.caso.negocio.calculo"));
        v.igual("los umbrales de la CR toman su valor por defecto",
                85.0, config.umbralAceptable(), EPS);
        v.igual("el modo por defecto combina CR y severidad",
                ModoClasificacion.CR_SEVERIDAD, config.modoClasificacion());

        ConfiguracionReglas explicita = cargar(
                "raiz=x\n"
                + "umbral.cr.aceptable=90\n"
                + "umbral.cr.observacion=75\n"
                + "clasificacion.modo=cr\n");
        v.igual("el umbral aceptable se sobrescribe", 90.0, explicita.umbralAceptable(), EPS);
        v.igual("el modo se sobrescribe", ModoClasificacion.CR, explicita.modoClasificacion());
        v.igual("el umbral de concentracion toma su valor por defecto",
                CargadorConfiguracion.UMBRAL_CONCENTRACION_POR_DEFECTO, explicita.umbralConcentracion());

        v.rechaza("una configuracion sin raiz se rechaza",
                IllegalArgumentException.class, () -> cargarSinIO("capas=a\n"));
        v.rechaza("una capa declarada sin paquetes se rechaza",
                IllegalArgumentException.class, () -> cargarSinIO("raiz=x\ncapas=a\n"));
        v.rechaza("una adyacencia hacia una capa inexistente se rechaza",
                IllegalArgumentException.class,
                () -> cargarSinIO("raiz=x\ncapas=a\ncapa.a=x.a\npermitido.a=fantasma\n"));
        v.rechaza("un umbral no numerico se rechaza",
                IllegalArgumentException.class, () -> cargarSinIO("raiz=x\numbral.concentracion=tres\n"));
        v.rechaza("un modo desconocido se rechaza",
                IllegalArgumentException.class, () -> cargarSinIO("raiz=x\nclasificacion.modo=inventado\n"));
        v.rechaza("umbrales invertidos se rechazan",
                IllegalArgumentException.class,
                () -> cargarSinIO("raiz=x\numbral.cr.aceptable=60\numbral.cr.observacion=80\n"));

        v.comprueba("una capa reconoce sus prefijos",
                new Capa("negocio", List.of("com.caso.negocio")).contiene("com.caso.negocio.sub"));
        v.comprueba("una capa no reclama paquetes ajenos",
                !new Capa("negocio", List.of("com.caso.negocio")).contiene("com.caso.dominio"));
        v.comprueba("una capa respeta la frontera del separador",
                !new Capa("negocio", List.of("com.caso.negocio")).contiene("com.caso.negociador"));
        v.comprueba("una capa reconoce el paquete exacto",
                new Capa("negocio", List.of("com.caso.negocio")).contiene("com.caso.negocio"));

        ConfiguracionReglas exenta = cargar(
                "raiz=com.caso\n"
                + "excluir=com.caso.generado,com.caso.vendor\n"
                + "rol.compartido=com.caso.anotaciones\n");
        v.igual("las exclusiones se leen en orden",
                List.of("com.caso.generado", "com.caso.vendor"), exenta.exclusiones());
        v.comprueba("un paquete excluido se reconoce", exenta.excluido("com.caso.vendor.lib"));
        v.comprueba("la exclusion respeta la frontera del separador",
                !exenta.excluido("com.caso.vendorizado"));
        v.comprueba("un nucleo compartido se reconoce", exenta.esCompartido("com.caso.anotaciones"));
        v.comprueba("un paquete corriente no es compartido", !exenta.esCompartido("com.caso.negocio"));
        v.comprueba("el rol compartido no se hereda a los subpaquetes",
                !exenta.esCompartido("com.caso.anotaciones.internas"));
        v.comprueba("se detecta que hay exenciones", exenta.tieneExenciones());
        v.comprueba("sin exenciones declaradas no hay exenciones", !configPorDefecto().tieneExenciones());

        v.rechaza("una exencion fuera de la raiz se rechaza",
                IllegalArgumentException.class,
                () -> cargarSinIO("raiz=com.caso\nexcluir=org.otro\n"));
        v.rechaza("un rol compartido fuera de la raiz se rechaza",
                IllegalArgumentException.class,
                () -> cargarSinIO("raiz=com.caso\nrol.compartido=org.otro\n"));
        v.rechaza("un paquete excluido y compartido a la vez se rechaza",
                IllegalArgumentException.class,
                () -> cargarSinIO("raiz=com.caso\nexcluir=com.caso.x\nrol.compartido=com.caso.x\n"));
    }

    // --------------------------------------------------------------- reglas

    private static void pruebasReglaCapas(Verificador v) {
        v.grupo("Fitness Function: integridad de capas");

        ConfiguracionReglas config = configPorDefecto();
        ReglaIntegridadCapas regla = new ReglaIntegridadCapas(config);

        ResultadoRegla base = regla.evaluar(grafoBase());
        v.igual("la linea base no incumple", 0, base.incumplimientos());
        v.igual("solo se verifican las aristas entre capas distintas", 2, base.verificaciones());

        GrafoDependencias salto = grafoBase();
        salto.agregarArista("com.caso.presentacion", "com.caso.persistencia", "salto de capa");
        ResultadoRegla conSalto = regla.evaluar(salto);
        v.igual("el salto de capa se detecta", 1, conSalto.incumplimientos());
        v.igual("la violacion reporta su ubicacion",
                List.of("com.caso.presentacion", "com.caso.persistencia"),
                conSalto.violaciones().get(0).ubicacion());
        v.comprueba("la violacion arrastra la evidencia de la arista",
                conSalto.violaciones().get(0).evidencia().contains("salto de capa"));
        v.igual("la severidad es mayor",
                Violacion.Severidad.MAYOR, conSalto.violaciones().get(0).severidad());

        GrafoDependencias haciaDominio = grafoBase();
        haciaDominio.agregarArista("com.caso.persistencia", "com.caso.dominio.extra");
        v.igual("las dependencias hacia paquetes sin capa no se verifican",
                2, regla.evaluar(haciaDominio).verificaciones());

        GrafoDependencias intraCapa = grafoBase();
        intraCapa.agregarArista("com.caso.negocio", "com.caso.negocio.calculo");
        v.igual("las dependencias dentro de una misma capa no se verifican",
                2, regla.evaluar(intraCapa).verificaciones());

        v.igual("los conteos y los hallazgos coinciden",
                conSalto.incumplimientos(), conSalto.violaciones().size());
    }

    private static void pruebasReglaCiclos(Verificador v) {
        v.grupo("Fitness Function: ausencia de ciclos");

        ReglaCiclos regla = new ReglaCiclos();
        ResultadoRegla base = regla.evaluar(grafoBase());
        v.igual("la linea base no incumple", 0, base.incumplimientos());
        v.igual("se verifica cada paquete", 4, base.verificaciones());

        GrafoDependencias conCiclo = grafoBase();
        conCiclo.agregarArista("com.caso.persistencia", "com.caso.negocio", "referencia inversa");
        ResultadoRegla resultado = regla.evaluar(conCiclo);
        v.igual("cada paquete del ciclo cuenta como incumplimiento", 2, resultado.incumplimientos());
        v.igual("el ciclo se reporta como un unico hallazgo", 1, resultado.violaciones().size());
        v.igual("la severidad es critica",
                Violacion.Severidad.CRITICA, resultado.violaciones().get(0).severidad());
        v.comprueba("la evidencia incluye la ruta del ciclo",
                resultado.violaciones().get(0).evidencia().stream()
                        .anyMatch(e -> e.startsWith("Ruta del ciclo:")));
    }

    private static void pruebasReglaConcentracion(Verificador v) {
        v.grupo("Fitness Function: concentracion de dependencias salientes");

        ReglaConcentracion regla = new ReglaConcentracion(3);
        v.igual("la linea base no incumple", 0, regla.evaluar(grafoBase()).incumplimientos());

        GrafoDependencias g = grafoBase();
        g.agregarArista("com.caso.util", "com.caso.dominio");
        g.agregarArista("com.caso.util", "com.caso.negocio");
        g.agregarArista("com.caso.util", "com.caso.persistencia");
        v.igual("un Ce igual al umbral no incumple", 0, regla.evaluar(g).incumplimientos());

        g.agregarArista("com.caso.util", "com.caso.presentacion");
        ResultadoRegla excedido = regla.evaluar(g);
        v.igual("un Ce por encima del umbral incumple", 1, excedido.incumplimientos());
        v.igual("el hallazgo ubica el paquete responsable",
                List.of("com.caso.util"), excedido.violaciones().get(0).ubicacion());
        v.comprueba("la evidencia declara Ce y Ca",
                excedido.violaciones().get(0).evidencia().get(0).contains("Ce = 4"));

        v.rechaza("un umbral negativo se rechaza",
                IllegalArgumentException.class, () -> new ReglaConcentracion(-1));
    }

    // ---------------------------------------------------------------- motor

    private static void pruebasMotor(Verificador v) {
        v.grupo("Motor de conformidad");

        MotorConformidad motor = new MotorConformidad(85.0, 70.0, ModoClasificacion.CR);
        v.igual("una CR en el umbral exacto es aceptable",
                EstadoArquitectonico.ACEPTABLE, motor.clasificarPorCR(85.0));
        v.igual("una CR justo por debajo pasa a observacion",
                EstadoArquitectonico.EN_OBSERVACION, motor.clasificarPorCR(84.99));
        v.igual("una CR en el umbral inferior sigue en observacion",
                EstadoArquitectonico.EN_OBSERVACION, motor.clasificarPorCR(70.0));
        v.igual("una CR por debajo del umbral inferior es no conforme",
                EstadoArquitectonico.NO_CONFORME, motor.clasificarPorCR(69.99));

        v.igual("un hallazgo critico exige el estado no conforme",
                EstadoArquitectonico.NO_CONFORME, motor.clasificarPorSeveridad(Violacion.Severidad.CRITICA));
        v.igual("un hallazgo mayor exige observacion",
                EstadoArquitectonico.EN_OBSERVACION, motor.clasificarPorSeveridad(Violacion.Severidad.MAYOR));
        v.igual("la ausencia de hallazgos es aceptable",
                EstadoArquitectonico.ACEPTABLE, motor.clasificarPorSeveridad(null));

        Resultado vacio = motor.evaluar(new GrafoDependencias(), reglas(configPorDefecto()));
        v.igual("un grafo vacio no produce division por cero", 100.0, vacio.conformidad(), EPS);
        v.igual("un grafo vacio tampoco divide por cero en la CR por regla", 100.0,
                vacio.conformidadPorRegla(), EPS);
        v.igual("un grafo vacio se clasifica como aceptable",
                EstadoArquitectonico.ACEPTABLE, vacio.estado());

        v.rechaza("un motor con umbrales invertidos se rechaza",
                IllegalArgumentException.class,
                () -> new MotorConformidad(60.0, 80.0, ModoClasificacion.CR));
        v.rechaza("un resultado de regla incoherente se rechaza",
                IllegalArgumentException.class,
                () -> new ResultadoRegla("r", 1, 5, List.of()));
    }

    // ------------------------------------------------------------ exenciones

    private static void pruebasExenciones(Verificador v) {
        v.grupo("Exenciones por papel arquitectonico");

        // Un nucleo de anotaciones al que todo referencia y que referencia de vuelta:
        // une en un mismo componente a paquetes que entre si no se alcanzan.
        GrafoDependencias g = new GrafoDependencias();
        g.agregarArista("com.caso.a", "com.caso.nucleo");
        g.agregarArista("com.caso.b", "com.caso.nucleo");
        g.agregarArista("com.caso.nucleo", "com.caso.a");
        g.agregarArista("com.caso.nucleo", "com.caso.b");

        ResultadoRegla sinExencion = new ReglaCiclos().evaluar(g);
        v.igual("sin exencion, el nucleo arrastra a todos al mismo ciclo",
                3, sinExencion.incumplimientos());

        Exenciones nucleo = new Exenciones(List.of("com.caso.nucleo"));
        ResultadoRegla conExencion = new ReglaCiclos(nucleo).evaluar(g);
        v.igual("al eximir el nucleo desaparece el ciclo artificial",
                0, conExencion.incumplimientos());
        v.igual("las verificaciones excluyen el paquete exento",
                2, conExencion.verificaciones());

        // Un ciclo real entre dos paquetes debe seguir detectandose pese a la exencion.
        g.agregarArista("com.caso.a", "com.caso.b");
        g.agregarArista("com.caso.b", "com.caso.a");
        v.igual("un ciclo real sobrevive a la exencion",
                2, new ReglaCiclos(nucleo).evaluar(g).incumplimientos());

        // Concentracion: la raiz de composicion depende de todo por diseno.
        GrafoDependencias h = new GrafoDependencias();
        for (String destino : List.of("uno", "dos", "tres", "cuatro")) {
            h.agregarArista("com.caso.raiz", "com.caso." + destino);
        }
        v.igual("sin exencion, la raiz de composicion se marca",
                1, new ReglaConcentracion(3).evaluar(h).incumplimientos());

        Exenciones raiz = new Exenciones(List.of("com.caso.raiz"));
        ResultadoRegla exenta = new ReglaConcentracion(3, raiz).evaluar(h);
        v.igual("al eximirla deja de marcarse", 0, exenta.incumplimientos());
        v.igual("y deja de contarse como verificacion", 4, exenta.verificaciones());

        v.comprueba("una exencion vacia no altera el resultado",
                new ReglaConcentracion(3, Exenciones.NINGUNA).evaluar(h).incumplimientos() == 1);

        Exenciones util = new Exenciones(List.of("com.caso.util"));
        v.comprueba("la exencion cubre el paquete exacto", util.cubre("com.caso.util"));
        v.comprueba("la exencion no absorbe paquetes de nombre parecido",
                !util.cubre("com.caso.utilidades"));
        v.comprueba("la exencion NO se hereda a los subpaquetes",
                !util.cubre("com.caso.util.io"));
        v.comprueba("eximir la raiz no eximiria al sistema entero",
                !new Exenciones(List.of("com.caso")).cubre("com.caso.negocio"));
    }

    // ------------------------------------------------------------ escenarios

    private static void pruebasEscenarios(Verificador v) {
        v.grupo("Escenarios del experimento controlado");

        ConfiguracionReglas config = configPorDefecto();
        List<Regla> reglas = reglas(config);
        MotorConformidad soloCR = new MotorConformidad(85.0, 70.0, ModoClasificacion.CR);
        MotorConformidad conSeveridad = new MotorConformidad(85.0, 70.0, ModoClasificacion.CR_SEVERIDAD);

        Resultado base = soloCR.evaluar(grafoBase(), reglas);
        v.igual("C0 - la linea base alcanza conformidad plena", 100.0, base.conformidad(), EPS);
        v.igual("C0 - la linea base es aceptable", EstadoArquitectonico.ACEPTABLE, base.estado());
        v.igual("C0 - la linea base no arroja falsos positivos", 0, base.totalViolaciones());

        Resultado c1 = soloCR.evaluar(escenarioCapas(), reglas);
        v.igual("C1 - una ruptura de capas aislada detecta un hallazgo", 1, c1.totalViolaciones());
        v.igual("C1 - la CR apenas desciende", 90.909, c1.conformidad(), 0.01);
        v.igual("C1 - la CR por si sola clasifica como aceptable",
                EstadoArquitectonico.ACEPTABLE, c1.estadoPorCR());
        v.igual("C1 - al incorporar la severidad el estado se corrige",
                EstadoArquitectonico.EN_OBSERVACION,
                conSeveridad.evaluar(escenarioCapas(), reglas).estado());

        Resultado c2 = soloCR.evaluar(escenarioCiclo(), reglas);
        v.igual("C2 - un ciclo intracapa se detecta", 1, c2.totalViolaciones());
        v.igual("C2 - la CR desciende a observacion", 83.333, c2.conformidad(), 0.01);
        v.igual("C2 - al incorporar la severidad el estado es no conforme",
                EstadoArquitectonico.NO_CONFORME,
                conSeveridad.evaluar(escenarioCiclo(), reglas).estado());

        Resultado c3 = soloCR.evaluar(escenarioConcentracion(), reglas);
        v.igual("C3 - la concentracion aislada se detecta", 1, c3.totalViolaciones());
        v.igual("C3 - la CR apenas desciende", 91.666, c3.conformidad(), 0.01);
        v.igual("C3 - la CR por si sola clasifica como aceptable",
                EstadoArquitectonico.ACEPTABLE, c3.estadoPorCR());

        Resultado c4 = soloCR.evaluar(escenarioCombinado(), reglas);
        v.igual("C4 - las tres derivas se detectan", 4, c4.totalViolaciones());
        v.igual("C4 - la CR cae al valor esperado", 57.142, c4.conformidad(), 0.01);
        v.igual("C4 - el estado es no conforme", EstadoArquitectonico.NO_CONFORME, c4.estado());
        v.comprueba("C4 - las tres reglas reportan hallazgos",
                c4.porRegla().stream().allMatch(r -> !r.violaciones().isEmpty()));

        // CR por regla (formulacion del anteproyecto): con tres reglas solo admite
        // cuatro valores, y cualquier deriva aislada la deja en 66,67 %.
        v.igual("CR por regla - C0 cumple las tres reglas", 100.0, base.conformidadPorRegla(), EPS);
        v.igual("CR por regla - C1 incumple una de tres", 66.667, c1.conformidadPorRegla(), 0.01);
        v.igual("CR por regla - C2 incumple una de tres", 66.667, c2.conformidadPorRegla(), 0.01);
        v.igual("CR por regla - C3 incumple una de tres", 66.667, c3.conformidadPorRegla(), 0.01);
        v.igual("CR por regla - C4 incumple las tres", 0.0, c4.conformidadPorRegla(), EPS);
        v.igual("CR por regla - C4 evalua tres reglas", 3, c4.reglasEvaluadas());

        v.comprueba("la severidad solo puede endurecer el veredicto, nunca relajarlo",
                conSeveridad.evaluar(escenarioCapas(), reglas).estado().ordinal()
                        >= c1.estadoPorCR().ordinal());
    }

    private static void pruebasExportadores(Verificador v) {
        v.grupo("Exportacion de evidencia");

        ConfiguracionReglas config = configPorDefecto();
        GrafoDependencias grafo = escenarioCombinado();
        Resultado resultado = new MotorConformidad().evaluar(grafo, reglas(config));

        String json = ExportadorJson.exportar(resultado, grafo, config, "c4");
        v.comprueba("el JSON abre y cierra correctamente",
                json.trim().startsWith("{") && json.trim().endsWith("}"));
        v.igual("las llaves del JSON estan balanceadas",
                contar(json, '{'), contar(json, '}'));
        v.igual("los corchetes del JSON estan balanceados",
                contar(json, '['), contar(json, ']'));
        v.comprueba("el JSON declara el estado final", json.contains("\"estado\""));

        String fila = ExportadorCsv.filaResumen(resultado, grafo, "c4");
        v.igual("la fila de resumen tiene tantas columnas como la cabecera",
                ExportadorCsv.CABECERA_RESUMEN.split(",").length, columnas(fila));

        String hallazgos = ExportadorCsv.filasHallazgos(resultado, "c4");
        v.igual("se exporta una fila por hallazgo",
                resultado.totalViolaciones(), hallazgos.strip().split("\n").length);
        for (String linea : hallazgos.strip().split("\n")) {
            v.igual("cada fila de hallazgo respeta la cabecera",
                    ExportadorCsv.CABECERA_HALLAZGOS.split(",").length, columnas(linea));
        }

        // Un texto con coma debe entrecomillarse para no desplazar las columnas.
        Resultado conComa = resultadoSintetico("Dependencia indebida: a, b y c");
        String filaConComa = ExportadorCsv.filasHallazgos(conComa, "sintetico").strip();
        v.comprueba("una descripcion con coma se entrecomilla", filaConComa.contains("\"Dependencia"));
        v.igual("una descripcion con coma no desplaza las columnas",
                ExportadorCsv.CABECERA_HALLAZGOS.split(",").length, columnas(filaConComa));
    }

    /** Resultado mínimo con un único hallazgo, para ejercitar el escapado del CSV. */
    private static Resultado resultadoSintetico(String descripcion) {
        Violacion violacion = new Violacion("regla", descripcion,
                List.of("com.caso.a"), List.of("evidencia"), Violacion.Severidad.MAYOR);
        ResultadoRegla regla = new ResultadoRegla("regla", 1, 1, List.of(violacion));
        return new Resultado(0.0, EstadoArquitectonico.NO_CONFORME, Violacion.Severidad.MAYOR,
                EstadoArquitectonico.EN_OBSERVACION, EstadoArquitectonico.NO_CONFORME,
                ModoClasificacion.CR_SEVERIDAD, List.of(regla), 1, 1, 85.0, 70.0);
    }

    // ---------------------------------------------------------- visualizacion

    private static void pruebasVisualizacion(Verificador v) throws Exception {
        v.grupo("Visualizacion del grafo");

        ConfiguracionReglas config = configPorDefecto();
        GrafoDependencias c4 = escenarioCombinado();
        Resultado resultado = new MotorConformidad().evaluar(c4, reglas(config));
        DisposicionGrafo d = DisposicionGrafo.calcular(c4, config, resultado);

        v.igual("todos los paquetes quedan colocados", c4.nodos().size(), d.nodos().size());
        v.igual("todas las dependencias quedan dibujadas", c4.cantidadAristas(), d.aristas().size());

        double yPres = nodo(d, "com.caso.presentacion").y();
        double yNeg = nodo(d, "com.caso.negocio").y();
        double yPer = nodo(d, "com.caso.persistencia").y();
        v.comprueba("la capa superior se dibuja arriba", yPres < yNeg && yNeg < yPer);

        ConfiguracionReglas invertida = cargarSinIO(
                "raiz=com.caso\n"
                + "capas=persistencia,negocio,presentacion\n"
                + "capa.presentacion=com.caso.presentacion\n"
                + "capa.negocio=com.caso.negocio\n"
                + "capa.persistencia=com.caso.persistencia\n"
                + "permitido.presentacion=negocio\n"
                + "permitido.negocio=persistencia\n"
                + "permitido.persistencia=\n");
        DisposicionGrafo di = DisposicionGrafo.calcular(c4, invertida,
                new MotorConformidad().evaluar(c4, reglas(invertida)));
        v.comprueba("el orden se deduce de la matriz y no del orden de declaracion",
                nodo(di, "com.caso.presentacion").y() < nodo(di, "com.caso.persistencia").y());

        DisposicionGrafo.Nodo util = nodo(d, "com.caso.util");
        DisposicionGrafo.Nodo dominio = nodo(d, "com.caso.dominio");
        v.comprueba("los paquetes sin capa van a la columna lateral", util.lateral() && dominio.lateral());
        double bordeCapas = d.nodos().stream().filter(n -> !n.lateral())
                .mapToDouble(DisposicionGrafo.Nodo::derecha).max().orElse(0);
        v.comprueba("la columna lateral queda a la derecha de las capas",
                util.x() > bordeCapas && dominio.x() > bordeCapas);
        v.comprueba("la columna lateral no superpone sus paquetes",
                Math.abs(util.y() - dominio.y()) >= DisposicionGrafo.ALTO_NODO);

        v.comprueba("ningun par de paquetes de una fila se superpone", sinSolapamientos(d));

        v.igual("las dos aristas que violan capas se clasifican como tales", 2L,
                d.aristas().stream().filter(a -> a.tipo() == DisposicionGrafo.TipoArista.VIOLA_CAPAS).count());
        v.igual("el paquete concentrador se marca", DisposicionGrafo.EstadoNodo.CONCENTRACION, util.estado());
        v.igual("los paquetes del ciclo se marcan", DisposicionGrafo.EstadoNodo.CICLO,
                nodo(d, "com.caso.negocio").estado());
        v.comprueba("una dependencia que remonta las capas se reconoce como ascendente",
                d.aristas().stream().anyMatch(a -> a.ascendente()
                        && a.origen().paquete().equals("com.caso.persistencia")
                        && a.destino().paquete().equals("com.caso.presentacion")));

        v.comprueba("las flechas que llegan a un mismo lado no comparten punto de llegada",
                puertosDistintos(d, "com.caso.dominio"));

        DisposicionGrafo c2 = DisposicionGrafo.calcular(escenarioCiclo(), config,
                new MotorConformidad().evaluar(escenarioCiclo(), reglas(config)));
        v.igual("los paquetes de un ciclo dentro de una capa comparten fila",
                nodo(c2, "com.caso.negocio").fila(), nodo(c2, "com.caso.negocio.calculo").fila());

        ConfiguracionReglas sinCapas = cargarSinIO("raiz=com.caso\n");
        DisposicionGrafo plano = DisposicionGrafo.calcular(c4, sinCapas,
                new MotorConformidad().evaluar(c4, reglas(sinCapas)));
        v.comprueba("sin capas declaradas no hay columna lateral", !plano.tieneColumnaLateral());
        v.comprueba("sin capas declaradas quien depende queda por encima",
                nodo(plano, "com.caso.util").y() < nodo(plano, "com.caso.dominio").y());

        GrafoDependencias hub = new GrafoDependencias();
        for (int i = 0; i < 20; i++) {
            hub.agregarArista("com.caso.m" + i, "com.caso.base");
        }
        DisposicionGrafo dh = DisposicionGrafo.calcular(hub, sinCapas,
                new MotorConformidad().evaluar(hub, reglas(sinCapas)));
        v.comprueba("un paquete muy referenciado se ensancha para repartir sus flechas",
                nodo(dh, "com.caso.base").ancho() > nodo(dh, "com.caso.m1").ancho());

        String svg = ExportadorSvg.exportar(d, resultado, "C4");
        String svgOtra = ExportadorSvg.exportar(DisposicionGrafo.calcular(c4, config, resultado), resultado, "C4");
        v.comprueba("el dibujo es determinista", svg.equals(svgOtra));
        v.comprueba("el SVG es XML bien formado", xmlValido(svg));
        v.comprueba("el SVG rotula cada paquete",
                svg.contains(">presentacion<") && svg.contains(">util<") && svg.contains(">dominio<"));
        v.comprueba("el SVG incluye la leyenda", svg.contains("Viola la integridad de capas"));

        String tikz = ExportadorTikz.exportar(d, "C4");
        v.comprueba("el TikZ abre y cierra su entorno",
                tikz.contains("\\begin{tikzpicture}") && tikz.contains("\\end{tikzpicture}"));
        v.igual("las llaves del TikZ estan balanceadas", contar(tikz, '{'), contar(tikz, '}'));
        v.igual("los guiones bajos se escapan para LaTeX", "cycle\\_detection",
                ExportadorTikz.latex("cycle_detection"));

        String dot = ExportadorDot.exportar(d, "C4");
        v.comprueba("el DOT fija las filas", dot.contains("rank=same"));
        v.comprueba("el DOT impide que una violacion invierta las capas", dot.contains("constraint=false"));
    }

    private static DisposicionGrafo.Nodo nodo(DisposicionGrafo d, String paquete) {
        return d.nodos().stream().filter(n -> n.paquete().equals(paquete)).findFirst().orElseThrow();
    }

    private static boolean sinSolapamientos(DisposicionGrafo d) {
        List<DisposicionGrafo.Nodo> nodos = d.nodos();
        for (int i = 0; i < nodos.size(); i++) {
            for (int j = i + 1; j < nodos.size(); j++) {
                DisposicionGrafo.Nodo a = nodos.get(i);
                DisposicionGrafo.Nodo b = nodos.get(j);
                boolean enX = a.x() < b.derecha() && b.x() < a.derecha();
                boolean enY = a.y() < b.abajo() && b.y() < a.abajo();
                if (enX && enY) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean puertosDistintos(DisposicionGrafo d, String paquete) {
        java.util.Set<String> puntos = new java.util.HashSet<>();
        for (DisposicionGrafo.Arista a : d.aristas()) {
            if (a.destino().paquete().equals(paquete)) {
                String punto = String.format(java.util.Locale.ROOT, "%.1f,%.1f", a.trayecto().x1(), a.trayecto().y1());
                if (!puntos.add(punto)) {
                    return false;
                }
            }
        }
        return puntos.size() > 1;
    }

    private static boolean xmlValido(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory fabrica = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(
                    xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------- historico

    private static void pruebasHistorico(Verificador v) throws IOException {
        v.grupo("Tendencia entre corridas");

        Path csv = Files.createTempFile("resumen", ".csv");
        try {
            Files.writeString(csv, ExportadorCsv.CABECERA_RESUMEN + "\n"
                    + "C0-base,4,5,10,0,100.00,Aceptable,,Aceptable,Aceptable\n"
                    + "C4,5,11,14,6,57.14,No conforme,Crítica,No conforme,No conforme\n"
                    + "C4,5,11,14,4,71.43,En observación,Mayor,En observación,En observación\n");

            v.igual("se recupera la ultima CR del escenario",
                    71.43, Historico.crAnterior(csv, "C4").getAsDouble(), 0.001);
            v.igual("cada escenario tiene su propia historia",
                    100.00, Historico.crAnterior(csv, "C0-base").getAsDouble(), 0.001);
            v.comprueba("un escenario sin historia no devuelve valor",
                    Historico.crAnterior(csv, "inexistente").isEmpty());
            v.comprueba("un archivo inexistente no devuelve valor",
                    Historico.crAnterior(Path.of("no-existe.csv"), "C4").isEmpty());
            v.comprueba("un archivo nulo no rompe",
                    Historico.crAnterior(null, "C4").isEmpty());

            v.comprueba("una mejora se describe como tal",
                    Historico.describirTendencia(88.35, 80.67).contains("mejora"));
            v.comprueba("un deterioro se describe como tal",
                    Historico.describirTendencia(70.0, 90.0).contains("deterioro"));
            v.comprueba("la ausencia de cambio se describe como tal",
                    Historico.describirTendencia(85.0, 85.0).contains("sin cambio"));
            v.comprueba("la mejora lleva signo explicito",
                    Historico.describirTendencia(88.35, 80.67).contains("+7.68 pp"));
        } finally {
            Files.deleteIfExists(csv);
        }

        // Una fila con coma dentro del nombre no debe desplazar las columnas.
        Path conComa = Files.createTempFile("resumen", ".csv");
        try {
            Files.writeString(conComa, ExportadorCsv.CABECERA_RESUMEN + "\n"
                    + "\"C1, capas\",4,6,11,1,90.91,Aceptable,Mayor,En observación,En observación\n");
            v.igual("un escenario entrecomillado se lee correctamente",
                    90.91, Historico.crAnterior(conComa, "C1, capas").getAsDouble(), 0.001);
        } finally {
            Files.deleteIfExists(conComa);
        }
    }

    // ------------------------------------------------------------ auxiliares

    private static GrafoDependencias grafoBase() {
        GrafoDependencias g = new GrafoDependencias();
        g.agregarArista("com.caso.presentacion", "com.caso.negocio");
        g.agregarArista("com.caso.presentacion", "com.caso.dominio");
        g.agregarArista("com.caso.negocio", "com.caso.persistencia");
        g.agregarArista("com.caso.negocio", "com.caso.dominio");
        g.agregarArista("com.caso.persistencia", "com.caso.dominio");
        return g;
    }

    /** C1: salto de capa presentacion hacia persistencia, sin ciclo ni concentracion. */
    private static GrafoDependencias escenarioCapas() {
        GrafoDependencias g = grafoBase();
        g.agregarArista("com.caso.presentacion", "com.caso.persistencia");
        return g;
    }

    /** C2: ciclo entre dos subpaquetes de la misma capa, sin violar la estratificacion. */
    private static GrafoDependencias escenarioCiclo() {
        GrafoDependencias g = grafoBase();
        g.agregarArista("com.caso.negocio", "com.caso.negocio.calculo");
        g.agregarArista("com.caso.negocio.calculo", "com.caso.negocio");
        return g;
    }

    /** C3: paquete de utilidades con Ce = 4, sin capa asignada y sin cerrar ciclos. */
    private static GrafoDependencias escenarioConcentracion() {
        GrafoDependencias g = grafoBase();
        g.agregarArista("com.caso.util", "com.caso.dominio");
        g.agregarArista("com.caso.util", "com.caso.negocio");
        g.agregarArista("com.caso.util", "com.caso.persistencia");
        g.agregarArista("com.caso.util", "com.caso.presentacion");
        return g;
    }

    /** C4: las tres derivas simultaneas. */
    private static GrafoDependencias escenarioCombinado() {
        GrafoDependencias g = grafoBase();
        g.agregarArista("com.caso.persistencia", "com.caso.presentacion");
        g.agregarArista("com.caso.persistencia", "com.caso.negocio");
        g.agregarArista("com.caso.util", "com.caso.dominio");
        g.agregarArista("com.caso.util", "com.caso.negocio");
        g.agregarArista("com.caso.util", "com.caso.presentacion");
        g.agregarArista("com.caso.util", "com.caso.persistencia");
        return g;
    }

    private static ConfiguracionReglas configPorDefecto() {
        return cargarSinIO(
                "raiz=com.caso\n"
                + "capas=presentacion,negocio,persistencia\n"
                + "capa.presentacion=com.caso.presentacion\n"
                + "capa.negocio=com.caso.negocio\n"
                + "capa.persistencia=com.caso.persistencia\n"
                + "permitido.presentacion=negocio\n"
                + "permitido.negocio=persistencia\n"
                + "permitido.persistencia=\n"
                + "umbral.concentracion=3\n");
    }

    private static List<Regla> reglas(ConfiguracionReglas config) {
        return List.of(
                new ReglaIntegridadCapas(config),
                new ReglaCiclos(),
                new ReglaConcentracion(config.umbralConcentracion()));
    }

    private static ConfiguracionReglas cargar(String contenido) throws IOException {
        Path temporal = Files.createTempFile("reglas", ".properties");
        try {
            Files.writeString(temporal, contenido);
            return CargadorConfiguracion.cargar(temporal);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static ConfiguracionReglas cargarSinIO(String contenido) {
        try {
            return cargar(contenido);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean esInmutable(Runnable bloque) {
        try {
            bloque.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    private static int contar(String texto, char caracter) {
        int total = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == caracter) {
                total++;
            }
        }
        return total;
    }

    private static int columnas(String fila) {
        boolean entreComillas = false;
        int columnas = 1;
        for (int i = 0; i < fila.length(); i++) {
            char c = fila.charAt(i);
            if (c == '"') {
                entreComillas = !entreComillas;
            } else if (c == ',' && !entreComillas) {
                columnas++;
            }
        }
        return columnas;
    }
}
