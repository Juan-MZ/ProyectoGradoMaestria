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
import co.javeriana.msw.arquitecta.reglas.Regla;
import co.javeriana.msw.arquitecta.reglas.ReglaCiclos;
import co.javeriana.msw.arquitecta.reglas.ReglaConcentracion;
import co.javeriana.msw.arquitecta.reglas.ReglaIntegridadCapas;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;
import co.javeriana.msw.arquitecta.reporte.ExportadorCsv;
import co.javeriana.msw.arquitecta.reporte.ExportadorJson;

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
        pruebasEscenarios(v);
        pruebasExportadores(v);

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
        v.igual("un grafo vacio se clasifica como aceptable",
                EstadoArquitectonico.ACEPTABLE, vacio.estado());

        v.rechaza("un motor con umbrales invertidos se rechaza",
                IllegalArgumentException.class,
                () -> new MotorConformidad(60.0, 80.0, ModoClasificacion.CR));
        v.rechaza("un resultado de regla incoherente se rechaza",
                IllegalArgumentException.class,
                () -> new ResultadoRegla("r", 1, 5, List.of()));
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
