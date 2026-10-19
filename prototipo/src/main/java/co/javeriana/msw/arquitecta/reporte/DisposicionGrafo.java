package co.javeriana.msw.arquitecta.reporte;

import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.grafo.Tarjan;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.ReglaCiclos;
import co.javeriana.msw.arquitecta.reglas.ReglaConcentracion;
import co.javeriana.msw.arquitecta.reglas.ReglaIntegridadCapas;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Calcula la disposición gráfica del grafo de dependencias.
 *
 * <p>Un grafo de arquitectura solo es legible si su dibujo refleja la
 * estratificación que se pretende verificar: las capas superiores arriba, las
 * inferiores abajo, y las dependencias fluyendo en el sentido previsto. Por eso
 * la disposición no se delega en un algoritmo genérico, que ordenaría los nodos
 * según el sentido de las aristas y dejaría que precisamente las violaciones
 * ---que apuntan hacia arriba--- invirtieran el orden de las capas. Aquí el orden
 * vertical lo dicta la arquitectura declarada, y cualquier arista que lo
 * contradiga se dibuja como lo que es: una dependencia que remonta la
 * estratificación.</p>
 *
 * <p>El procedimiento sigue el esquema clásico del dibujo de grafos jerárquicos
 * (Sugiyama, Tagawa y Toda, 1981), adaptado al dominio:</p>
 * <ol>
 *   <li><b>Asignación de bandas.</b> Cada capa declarada ocupa una banda
 *       horizontal. Su posición se deduce de la matriz de dependencias
 *       permitidas: una capa que puede depender de otras se sitúa por encima de
 *       ellas, con independencia del orden en que se declararon.</li>
 *   <li><b>Asignación de filas.</b> Dentro de cada banda, los paquetes se
 *       distribuyen en filas según el camino más largo sobre la condensación del
 *       subgrafo de la banda, de modo que también las dependencias internas
 *       apunten hacia abajo. Los paquetes de un mismo ciclo comparten fila. Las
 *       filas demasiado anchas se parten para conservar la legibilidad.</li>
 *   <li><b>Reducción de cruces.</b> Se reordena cada fila según el baricentro
 *       horizontal de sus vecinos, alternando barridos descendentes y
 *       ascendentes.</li>
 *   <li><b>Columna lateral.</b> Los paquetes sin capa ---un dominio compartido,
 *       un núcleo de anotaciones--- no pertenecen a la estratificación y se
 *       colocan en una columna aparte, a la altura media de sus vecinos.</li>
 * </ol>
 *
 * <p>La disposición es determinista: la misma entrada produce siempre el mismo
 * dibujo, de modo que dos figuras pueden compararse sin ruido.</p>
 */
public final class DisposicionGrafo {

    // Unidades abstractas de dibujo: una unidad equivale a un píxel del SVG.
    /** Anchura media de un carácter de la fuente monoespaciada a 12 unidades. */
    public static final double ANCHO_CARACTER = 7.2;
    public static final double ALTO_NODO = 30;

    private static final double RELLENO_NODO = 14;
    private static final double ANCHO_MINIMO_NODO = 72;
    private static final double SEPARACION_HORIZONTAL = 28;
    private static final double SEPARACION_FILAS = 58;
    private static final double RELLENO_BANDA_SUPERIOR = 30;
    private static final double RELLENO_BANDA_INFERIOR = 16;
    private static final double SEPARACION_BANDAS = 12;
    private static final double MARGEN_LATERAL = 92;
    private static final double MARGEN_SUPERIOR = 76;
    private static final double MARGEN_INFERIOR = 66;
    private static final double SEPARACION_COLUMNA = 118;
    private static final double SEPARACION_LATERAL = 16;
    private static final double CURVATURA_MAXIMA = 72;
    private static final double SEPARACION_PUERTOS = 7;
    private static final double ANCHO_MAXIMO_POR_CONEXIONES = 200;
    private static final int MAXIMO_POR_FILA = 5;
    private static final int BARRIDOS = 8;

    /** Estado visual de un paquete, derivado de los hallazgos de las reglas. */
    public enum EstadoNodo { NORMAL, CICLO, CONCENTRACION, CICLO_Y_CONCENTRACION, COMPARTIDO }

    /** Naturaleza de una dependencia a efectos de su representación. */
    public enum TipoArista { NUCLEO, NORMAL, CICLO, VIOLA_CAPAS }

    /** Paquete colocado en el dibujo. Las coordenadas corresponden a su esquina superior izquierda. */
    public record Nodo(String paquete, String etiqueta, String capa, EstadoNodo estado,
            double x, double y, double ancho, boolean lateral, int fila, int ce, int ca) {

        public double cx() {
            return x + ancho / 2;
        }

        public double cy() {
            return y + ALTO_NODO / 2;
        }

        public double abajo() {
            return y + ALTO_NODO;
        }

        public double derecha() {
            return x + ancho;
        }
    }

    /** Curva de Bézier cúbica: punto inicial, dos puntos de control y punto final. */
    public record Trayecto(double x0, double y0, double cx1, double cy1,
            double cx2, double cy2, double x1, double y1) {
    }

    /** Dependencia dibujada, con su clasificación y su trazado. */
    public record Arista(Nodo origen, Nodo destino, TipoArista tipo, Trayecto trayecto, boolean ascendente) {
    }

    /** Banda horizontal que representa una capa declarada. */
    public record Banda(String capa, double y, double alto) {
    }

    /** Fila del dibujo, con los paquetes que contiene en su orden final. */
    public record Fila(String capa, List<String> paquetes) {
    }

    private final List<Nodo> nodos;
    private final List<Arista> aristas;
    private final List<Banda> bandas;
    private final List<Fila> filas;
    private final double xBandas;
    private final double anchoBandas;
    private final double xColumna;
    private final double anchoColumna;
    private final double ancho;
    private final double alto;
    private final Set<EstadoNodo> estadosPresentes;
    private final Set<TipoArista> tiposPresentes;

    private DisposicionGrafo(List<Nodo> nodos, List<Arista> aristas, List<Banda> bandas, List<Fila> filas,
            double xBandas, double anchoBandas, double xColumna, double anchoColumna,
            double ancho, double alto) {
        this.nodos = List.copyOf(nodos);
        this.aristas = List.copyOf(aristas);
        this.bandas = List.copyOf(bandas);
        this.filas = List.copyOf(filas);
        this.xBandas = xBandas;
        this.anchoBandas = anchoBandas;
        this.xColumna = xColumna;
        this.anchoColumna = anchoColumna;
        this.ancho = ancho;
        this.alto = alto;
        this.estadosPresentes = EnumSet.noneOf(EstadoNodo.class);
        nodos.forEach(n -> estadosPresentes.add(n.estado()));
        this.tiposPresentes = EnumSet.noneOf(TipoArista.class);
        aristas.forEach(a -> tiposPresentes.add(a.tipo()));
    }

    public List<Nodo> nodos() {
        return nodos;
    }

    /** Aristas en orden de dibujo: las más relevantes al final, para que queden encima. */
    public List<Arista> aristas() {
        return aristas;
    }

    public List<Banda> bandas() {
        return bandas;
    }

    public List<Fila> filas() {
        return filas;
    }

    public double xBandas() {
        return xBandas;
    }

    public double anchoBandas() {
        return anchoBandas;
    }

    /** Indica si hay paquetes fuera de la estratificación, dibujados en una columna aparte. */
    public boolean tieneColumnaLateral() {
        return nodos.stream().anyMatch(Nodo::lateral);
    }

    public double xColumna() {
        return xColumna;
    }

    public double anchoColumna() {
        return anchoColumna;
    }

    public double ancho() {
        return ancho;
    }

    public double alto() {
        return alto;
    }

    /** Ordenada a partir de la cual se dibuja la leyenda. */
    public double yLeyenda() {
        return alto - MARGEN_INFERIOR + 18;
    }

    public Set<EstadoNodo> estadosPresentes() {
        return estadosPresentes;
    }

    public Set<TipoArista> tiposPresentes() {
        return tiposPresentes;
    }

    // ------------------------------------------------------------------ cálculo

    public static DisposicionGrafo calcular(GrafoDependencias grafo, ConfiguracionReglas config, Resultado resultado) {
        Clasificacion clasificacion = Clasificacion.desde(resultado);

        // 1. Bandas: capas presentes en el grafo, de la superior a la inferior.
        Map<String, List<String>> porCapa = new LinkedHashMap<>();
        List<String> sinCapa = new ArrayList<>();
        for (String paquete : ordenados(grafo.nodos())) {
            String capa = config.capaDe(paquete);
            if (capa == null) {
                sinCapa.add(paquete);
            } else {
                porCapa.computeIfAbsent(capa, c -> new ArrayList<>()).add(paquete);
            }
        }

        List<String> capasPresentes = new ArrayList<>();
        List<List<String>> grupos = new ArrayList<>();
        List<String> lateral;
        if (porCapa.isEmpty()) {
            // Sin estratificación declarada: todo el grafo es una sola banda implícita.
            capasPresentes.add(null);
            grupos.add(sinCapa);
            lateral = List.of();
        } else {
            for (String capa : ordenarCapas(config)) {
                if (porCapa.containsKey(capa)) {
                    capasPresentes.add(capa);
                    grupos.add(porCapa.get(capa));
                }
            }
            lateral = sinCapa;
        }

        // 2. Filas dentro de cada banda.
        List<List<String>> filas = new ArrayList<>();
        List<String> capaDeFila = new ArrayList<>();
        for (int i = 0; i < grupos.size(); i++) {
            for (List<String> fila : filasDe(grafo, grupos.get(i))) {
                filas.add(fila);
                capaDeFila.add(capasPresentes.get(i));
            }
        }

        // 3. Medidas y reducción de cruces.
        Map<String, Double> anchoDe = new HashMap<>();
        for (String paquete : grafo.nodos()) {
            int conexiones = Math.max(grafo.acoplamientoEferente(paquete), grafo.acoplamientoAferente(paquete));
            anchoDe.put(paquete, anchoNodo(etiqueta(paquete, config), conexiones));
        }
        double anchoMaximoFila = 0;
        for (List<String> fila : filas) {
            anchoMaximoFila = Math.max(anchoMaximoFila, anchoFila(fila, anchoDe));
        }
        double xInicio = MARGEN_LATERAL;
        Map<String, Double> xCentro = new HashMap<>();
        for (List<String> fila : filas) {
            colocarFila(fila, anchoDe, xInicio, anchoMaximoFila, xCentro);
        }
        Map<String, Set<String>> vecinos = vecinos(grafo);
        Map<String, Integer> filaDe = new HashMap<>();
        for (int i = 0; i < filas.size(); i++) {
            for (String paquete : filas.get(i)) {
                filaDe.put(paquete, i);
            }
        }
        for (int barrido = 0; barrido < BARRIDOS; barrido++) {
            boolean descendente = barrido % 2 == 0;
            for (int k = 0; k < filas.size(); k++) {
                int i = descendente ? k : filas.size() - 1 - k;
                reordenar(filas.get(i), i, descendente, vecinos, filaDe, xCentro);
                colocarFila(filas.get(i), anchoDe, xInicio, anchoMaximoFila, xCentro);
            }
        }

        // 4. Ordenadas: bandas y filas.
        Map<String, Double> yDe = new HashMap<>();
        List<Banda> bandas = new ArrayList<>();
        double y = MARGEN_SUPERIOR;
        int i = 0;
        while (i < filas.size()) {
            String capa = capaDeFila.get(i);
            double yBanda = y;
            y += RELLENO_BANDA_SUPERIOR;
            boolean primera = true;
            while (i < filas.size() && iguales(capaDeFila.get(i), capa)) {
                if (!primera) {
                    y += SEPARACION_FILAS;
                }
                for (String paquete : filas.get(i)) {
                    yDe.put(paquete, y);
                }
                y += ALTO_NODO;
                primera = false;
                i++;
            }
            y += RELLENO_BANDA_INFERIOR;
            bandas.add(new Banda(capa, yBanda, y - yBanda));
            y += SEPARACION_BANDAS;
        }
        double finBandas = y - SEPARACION_BANDAS;

        // 5. Columna lateral para los paquetes sin capa.
        double xBandas = 16;
        double anchoBandas = xInicio + anchoMaximoFila + MARGEN_LATERAL - 16 - xBandas;
        double xColumna = xInicio + anchoMaximoFila + SEPARACION_COLUMNA;
        double anchoColumna = 0;
        for (String paquete : lateral) {
            anchoColumna = Math.max(anchoColumna, anchoDe.get(paquete));
        }
        double finLateral = 0;
        if (!lateral.isEmpty()) {
            double yMinimo = MARGEN_SUPERIOR + RELLENO_BANDA_SUPERIOR;
            double yMedio = (MARGEN_SUPERIOR + finBandas) / 2 - ALTO_NODO / 2;
            List<String> ordenLateral = new ArrayList<>(lateral);
            Map<String, Double> objetivo = new HashMap<>();
            for (String paquete : ordenLateral) {
                double suma = 0;
                int cuenta = 0;
                for (String vecino : vecinos.getOrDefault(paquete, Set.of())) {
                    if (yDe.containsKey(vecino)) {
                        suma += yDe.get(vecino);
                        cuenta++;
                    }
                }
                objetivo.put(paquete, cuenta == 0 ? yMedio : suma / cuenta);
            }
            ordenLateral.sort(Comparator.comparingDouble((String p) -> objetivo.get(p))
                    .thenComparing(Comparator.naturalOrder()));
            double anterior = Double.NEGATIVE_INFINITY;
            for (String paquete : ordenLateral) {
                double yNodo = Math.max(objetivo.get(paquete), yMinimo);
                yNodo = Math.max(yNodo, anterior + ALTO_NODO + SEPARACION_LATERAL);
                yDe.put(paquete, yNodo);
                xCentro.put(paquete, xColumna + anchoColumna / 2);
                anterior = yNodo;
            }
            finLateral = anterior + ALTO_NODO + RELLENO_BANDA_INFERIOR;
        }

        // 6. Nodos definitivos.
        Map<String, Nodo> nodoDe = new LinkedHashMap<>();
        for (String paquete : ordenados(grafo.nodos())) {
            double ancho = anchoDe.get(paquete);
            boolean esLateral = lateral.contains(paquete);
            nodoDe.put(paquete, new Nodo(
                    paquete,
                    etiqueta(paquete, config),
                    config.capaDe(paquete),
                    clasificacion.estado(paquete, config),
                    xCentro.get(paquete) - ancho / 2,
                    yDe.get(paquete),
                    ancho,
                    esLateral,
                    esLateral ? -1 : filaDe.get(paquete),
                    grafo.acoplamientoEferente(paquete),
                    grafo.acoplamientoAferente(paquete)));
        }

        // 7. Aristas, clasificadas y trazadas.
        double centroBandas = xInicio + anchoMaximoFila / 2;
        Set<String> extremosDerechos = new HashSet<>();
        for (List<String> fila : filas) {
            extremosDerechos.add(fila.get(fila.size() - 1));
        }
        List<Plan> planes = new ArrayList<>();
        for (String[] par : grafo.aristas()) {
            planes.add(new Plan(nodoDe.get(par[0]), nodoDe.get(par[1]),
                    clasificacion.tipo(par[0], par[1], config), centroBandas, extremosDerechos));
        }
        asignarPuertos(planes);
        List<Arista> aristas = new ArrayList<>();
        for (Plan plan : planes) {
            boolean ascendente = !plan.origen.lateral() && !plan.destino.lateral()
                    && plan.destino.fila() < plan.origen.fila();
            aristas.add(new Arista(plan.origen, plan.destino, plan.tipo, plan.trayecto(), ascendente));
        }
        aristas.sort(Comparator.comparingInt((Arista a) -> a.tipo().ordinal())
                .thenComparing(a -> a.origen().paquete())
                .thenComparing(a -> a.destino().paquete()));

        List<Fila> filasFinales = new ArrayList<>();
        for (int f = 0; f < filas.size(); f++) {
            filasFinales.add(new Fila(capaDeFila.get(f), List.copyOf(filas.get(f))));
        }

        double anchoTotal = lateral.isEmpty()
                ? xBandas + anchoBandas + 16
                : xColumna + anchoColumna + 40;
        double altoTotal = Math.max(finBandas, finLateral) + MARGEN_INFERIOR;
        return new DisposicionGrafo(new ArrayList<>(nodoDe.values()), aristas, bandas, filasFinales,
                xBandas, anchoBandas, xColumna - 20, anchoColumna + 40, anchoTotal, altoTotal);
    }

    // ------------------------------------------------------------ bandas y filas

    /**
     * Ordena las capas de la superior a la inferior a partir de la matriz de
     * dependencias permitidas: la profundidad de una capa es la longitud del
     * camino más largo de capas de las que puede depender. A igual profundidad
     * se respeta el orden de declaración. Si la matriz contuviera un ciclo, la
     * recursión se corta y el orden de declaración decide.
     */
    static List<String> ordenarCapas(ConfiguracionReglas config) {
        List<String> declaradas = config.nombresCapas();
        Map<String, Integer> profundidad = new HashMap<>();
        for (String capa : declaradas) {
            profundidad(capa, config, profundidad, new HashSet<>());
        }
        List<String> orden = new ArrayList<>(declaradas);
        orden.sort(Comparator.comparingInt((String c) -> -profundidad.get(c))
                .thenComparingInt(declaradas::indexOf));
        return orden;
    }

    private static int profundidad(String capa, ConfiguracionReglas config,
            Map<String, Integer> memoria, Set<String> enCurso) {
        if (memoria.containsKey(capa)) {
            return memoria.get(capa);
        }
        if (!enCurso.add(capa)) {
            return 0;
        }
        int maxima = 0;
        for (String destino : config.destinosPermitidos(capa)) {
            if (!destino.equals(capa)) {
                maxima = Math.max(maxima, 1 + profundidad(destino, config, memoria, enCurso));
            }
        }
        enCurso.remove(capa);
        memoria.put(capa, maxima);
        return maxima;
    }

    /**
     * Distribuye en filas los paquetes de una banda: camino más largo sobre la
     * condensación del subgrafo, de modo que quien depende queda por encima de
     * aquello de lo que depende. Las filas que exceden el máximo se parten.
     */
    static List<List<String>> filasDe(GrafoDependencias grafo, List<String> miembros) {
        Set<String> conjunto = new HashSet<>(miembros);
        GrafoDependencias subgrafo = grafo.filtrar(conjunto::contains);
        List<List<String>> componentes = new Tarjan(subgrafo).componentesFuertementeConexos();

        Map<String, Integer> componenteDe = new HashMap<>();
        for (int c = 0; c < componentes.size(); c++) {
            for (String paquete : componentes.get(c)) {
                componenteDe.put(paquete, c);
            }
        }
        Map<Integer, Set<Integer>> sucesores = new HashMap<>();
        int[] entrantes = new int[componentes.size()];
        for (String paquete : miembros) {
            int c = componenteDe.get(paquete);
            for (String destino : subgrafo.destinosDe(paquete)) {
                int d = componenteDe.get(destino);
                if (c != d && sucesores.computeIfAbsent(c, k -> new HashSet<>()).add(d)) {
                    entrantes[d]++;
                }
            }
        }

        int[] rango = new int[componentes.size()];
        Deque<Integer> pendientes = new ArrayDeque<>();
        List<Integer> fuentes = new ArrayList<>();
        for (int c = 0; c < componentes.size(); c++) {
            if (entrantes[c] == 0) {
                fuentes.add(c);
            }
        }
        fuentes.sort(Comparator.comparing(c -> minimo(componentes.get(c))));
        pendientes.addAll(fuentes);
        while (!pendientes.isEmpty()) {
            int c = pendientes.poll();
            List<Integer> siguientes = new ArrayList<>(sucesores.getOrDefault(c, Set.of()));
            siguientes.sort(Comparator.comparing(d -> minimo(componentes.get(d))));
            for (int d : siguientes) {
                rango[d] = Math.max(rango[d], rango[c] + 1);
                if (--entrantes[d] == 0) {
                    pendientes.add(d);
                }
            }
        }

        Map<Integer, List<String>> porRango = new TreeMap<>();
        for (String paquete : miembros) {
            porRango.computeIfAbsent(rango[componenteDe.get(paquete)], r -> new ArrayList<>()).add(paquete);
        }
        List<List<String>> filas = new ArrayList<>();
        for (List<String> nivel : porRango.values()) {
            nivel.sort(Comparator.naturalOrder());
            for (int inicio = 0; inicio < nivel.size(); inicio += MAXIMO_POR_FILA) {
                filas.add(new ArrayList<>(nivel.subList(inicio, Math.min(nivel.size(), inicio + MAXIMO_POR_FILA))));
            }
        }
        return filas;
    }

    // -------------------------------------------------------- reducción de cruces

    private static void reordenar(List<String> fila, int indice, boolean descendente,
            Map<String, Set<String>> vecinos, Map<String, Integer> filaDe, Map<String, Double> xCentro) {
        Map<String, Double> clave = new HashMap<>();
        for (String paquete : fila) {
            double suma = 0;
            int cuenta = 0;
            for (String vecino : vecinos.getOrDefault(paquete, Set.of())) {
                Integer f = filaDe.get(vecino);
                if (f != null && (descendente ? f < indice : f > indice)) {
                    suma += xCentro.get(vecino);
                    cuenta++;
                }
            }
            clave.put(paquete, cuenta == 0 ? xCentro.get(paquete) : suma / cuenta);
        }
        List<String> anterior = new ArrayList<>(fila);
        fila.sort(Comparator.comparingDouble((String p) -> clave.get(p))
                .thenComparingInt(anterior::indexOf));
    }

    private static void colocarFila(List<String> fila, Map<String, Double> anchoDe, double xInicio,
            double anchoMaximo, Map<String, Double> xCentro) {
        double x = xInicio + (anchoMaximo - anchoFila(fila, anchoDe)) / 2;
        for (String paquete : fila) {
            double ancho = anchoDe.get(paquete);
            xCentro.put(paquete, x + ancho / 2);
            x += ancho + SEPARACION_HORIZONTAL;
        }
    }

    private static double anchoFila(List<String> fila, Map<String, Double> anchoDe) {
        double total = 0;
        for (String paquete : fila) {
            total += anchoDe.get(paquete);
        }
        return total + SEPARACION_HORIZONTAL * Math.max(0, fila.size() - 1);
    }

    // --------------------------------------------------------------- trazado

    private enum Lado { ARRIBA, ABAJO, IZQUIERDA, DERECHA }

    /**
     * Forma de una arista según la posición relativa de sus extremos. Las que
     * descienden van del borde inferior del origen al superior del destino; las
     * que remontan la estratificación rodean por fuera, hacia el lado más
     * próximo, para que se distingan a simple vista; las que unen paquetes de una
     * misma fila se trazan como arcos ---por arriba hacia la derecha, por abajo
     * hacia la izquierda---, de modo que una dependencia mutua no se superponga
     * consigo misma; y las que tocan la columna lateral se trazan en horizontal.
     */
    private enum Forma {
        DESCENDENTE, ASCENDENTE, HACIA_COLUMNA, HACIA_COLUMNA_POR_DEBAJO,
        DESDE_COLUMNA, DESDE_COLUMNA_POR_ENCIMA, ARCO_SUPERIOR, ARCO_INFERIOR, RODEO
    }

    /** Arista en preparación: se conoce su forma y sus lados, y luego se le asignan los puertos. */
    private static final class Plan {
        final Nodo origen;
        final Nodo destino;
        final TipoArista tipo;
        final Forma forma;
        final Lado ladoOrigen;
        final Lado ladoDestino;
        double x0;
        double y0;
        double x1;
        double y1;

        Plan(Nodo origen, Nodo destino, TipoArista tipo, double centro, Set<String> extremosDerechos) {
            this.origen = origen;
            this.destino = destino;
            this.tipo = tipo;
            if (origen.lateral() && destino.lateral()) {
                boolean baja = destino.cy() >= origen.cy();
                forma = baja ? Forma.DESCENDENTE : Forma.ASCENDENTE;
                ladoOrigen = baja ? Lado.ABAJO : Lado.ARRIBA;
                ladoDestino = baja ? Lado.ARRIBA : Lado.ABAJO;
            } else if (destino.lateral()) {
                // Un paquete con vecinos a su derecha sale por debajo: en horizontal
                // pasaría por detrás de ellos y parecería una arista hacia el vecino.
                boolean libre = extremosDerechos.contains(origen.paquete());
                forma = libre ? Forma.HACIA_COLUMNA : Forma.HACIA_COLUMNA_POR_DEBAJO;
                ladoOrigen = libre ? Lado.DERECHA : Lado.ABAJO;
                ladoDestino = Lado.IZQUIERDA;
            } else if (origen.lateral()) {
                boolean libre = extremosDerechos.contains(destino.paquete());
                forma = libre ? Forma.DESDE_COLUMNA : Forma.DESDE_COLUMNA_POR_ENCIMA;
                ladoOrigen = Lado.IZQUIERDA;
                ladoDestino = libre ? Lado.DERECHA : Lado.ARRIBA;
            } else if (origen.fila() == destino.fila()) {
                boolean derecha = destino.cx() > origen.cx();
                forma = derecha ? Forma.ARCO_SUPERIOR : Forma.ARCO_INFERIOR;
                ladoOrigen = derecha ? Lado.ARRIBA : Lado.ABAJO;
                ladoDestino = ladoOrigen;
            } else if (destino.fila() > origen.fila()) {
                forma = Forma.DESCENDENTE;
                ladoOrigen = Lado.ABAJO;
                ladoDestino = Lado.ARRIBA;
            } else {
                forma = Forma.RODEO;
                ladoOrigen = origen.cx() >= centro ? Lado.DERECHA : Lado.IZQUIERDA;
                ladoDestino = ladoOrigen;
            }
        }

        Trayecto trayecto() {
            switch (forma) {
                case DESCENDENTE, ASCENDENTE -> {
                    double tramo = (y1 - y0) * 0.45;
                    return new Trayecto(x0, y0, x0, y0 + tramo, x1, y1 - tramo, x1, y1);
                }
                case HACIA_COLUMNA, DESDE_COLUMNA -> {
                    double mitad = (x1 - x0) / 2;
                    return new Trayecto(x0, y0, x0 + mitad, y0, x1 - mitad, y1, x1, y1);
                }
                case HACIA_COLUMNA_POR_DEBAJO -> {
                    return new Trayecto(x0, y0, x0, y0 + 38, x1 - (x1 - x0) * 0.45, y1, x1, y1);
                }
                case DESDE_COLUMNA_POR_ENCIMA -> {
                    return new Trayecto(x0, y0, x0 - (x0 - x1) * 0.45, y0, x1, y1 - 38, x1, y1);
                }
                case ARCO_SUPERIOR, ARCO_INFERIOR -> {
                    double altura = Math.min(30, 18 + 0.08 * Math.abs(x1 - x0));
                    double signo = forma == Forma.ARCO_SUPERIOR ? -1 : 1;
                    return new Trayecto(x0, y0, x0, y0 + signo * altura, x1, y1 + signo * altura, x1, y1);
                }
                default -> {
                    double curvatura = Math.min(CURVATURA_MAXIMA, 34 + 0.22 * Math.abs(y0 - y1));
                    double signo = ladoOrigen == Lado.DERECHA ? 1 : -1;
                    double extremo = signo > 0 ? Math.max(x0, x1) : Math.min(x0, x1);
                    return new Trayecto(x0, y0, extremo + signo * curvatura, y0,
                            extremo + signo * curvatura, y1, x1, y1);
                }
            }
        }
    }

    /**
     * Reparte los extremos de las aristas a lo largo del lado del nodo por el que
     * entran o salen. Sin este reparto, todas las flechas que llegan a un paquete
     * por el mismo lado convergerían en un único punto y sus puntas se taparían
     * entre sí. Los extremos se ordenan según la posición del otro nodo, para que
     * las aristas no se crucen innecesariamente al salir; en los rodeos el orden
     * se invierte, de modo que los arcos queden anidados en lugar de cruzarse.
     */
    private static void asignarPuertos(List<Plan> planes) {
        Map<String, List<Object[]>> porLado = new LinkedHashMap<>();
        for (Plan plan : planes) {
            porLado.computeIfAbsent(plan.origen.paquete() + "|" + plan.ladoOrigen, k -> new ArrayList<>())
                   .add(new Object[] {plan, Boolean.TRUE});
            porLado.computeIfAbsent(plan.destino.paquete() + "|" + plan.ladoDestino, k -> new ArrayList<>())
                   .add(new Object[] {plan, Boolean.FALSE});
        }
        for (List<Object[]> extremos : porLado.values()) {
            extremos.sort(Comparator.comparingDouble((Object[] e) -> clavePuerto((Plan) e[0], (Boolean) e[1]))
                    .thenComparing(e -> otro((Plan) e[0], (Boolean) e[1]).paquete()));
            int total = extremos.size();
            for (int i = 0; i < total; i++) {
                Plan plan = (Plan) extremos.get(i)[0];
                boolean esOrigen = (Boolean) extremos.get(i)[1];
                Nodo nodo = esOrigen ? plan.origen : plan.destino;
                Lado lado = esOrigen ? plan.ladoOrigen : plan.ladoDestino;
                double t = (i + 1.0) / (total + 1);
                double x;
                double y;
                switch (lado) {
                    case ARRIBA -> {
                        x = nodo.x() + 10 + t * (nodo.ancho() - 20);
                        y = nodo.y();
                    }
                    case ABAJO -> {
                        x = nodo.x() + 10 + t * (nodo.ancho() - 20);
                        y = nodo.abajo();
                    }
                    case IZQUIERDA -> {
                        x = nodo.x();
                        y = nodo.y() + 6 + t * (ALTO_NODO - 12);
                    }
                    default -> {
                        x = nodo.derecha();
                        y = nodo.y() + 6 + t * (ALTO_NODO - 12);
                    }
                }
                if (esOrigen) {
                    plan.x0 = x;
                    plan.y0 = y;
                } else {
                    plan.x1 = x;
                    plan.y1 = y;
                }
            }
        }
    }

    private static double clavePuerto(Plan plan, boolean esOrigen) {
        Nodo otro = otro(plan, esOrigen);
        Lado lado = esOrigen ? plan.ladoOrigen : plan.ladoDestino;
        if (lado == Lado.ARRIBA || lado == Lado.ABAJO) {
            return otro.cx();
        }
        return plan.forma == Forma.RODEO ? -otro.cy() : otro.cy();
    }

    private static Nodo otro(Plan plan, boolean esOrigen) {
        return esOrigen ? plan.destino : plan.origen;
    }

    // ------------------------------------------------------------- auxiliares

    /**
     * Etiqueta visible de un paquete: su nombre relativo a la raíz de análisis,
     * que es común a todos y solo ocuparía espacio. La raíz misma se muestra con
     * su último segmento.
     */
    static String etiqueta(String paquete, ConfiguracionReglas config) {
        String raiz = config.raiz();
        if (paquete.equals(raiz)) {
            int punto = raiz.lastIndexOf('.');
            return punto < 0 ? raiz : raiz.substring(punto + 1);
        }
        if (paquete.startsWith(raiz + ".")) {
            return paquete.substring(raiz.length() + 1);
        }
        return paquete;
    }

    /**
     * Anchura de un nodo: la que exige su etiqueta o, si es mayor, la que necesita
     * para repartir sus conexiones sin que las puntas de flecha se amontonen. Un
     * paquete del que dependen veinte otros no cabe en un recuadro pensado para su
     * nombre, y ensancharlo comunica además, de un vistazo, que es un concentrador.
     */
    static double anchoNodo(String etiqueta, int conexiones) {
        double porTexto = etiqueta.length() * ANCHO_CARACTER + 2 * RELLENO_NODO;
        double porConexiones = Math.min(ANCHO_MAXIMO_POR_CONEXIONES, 20 + SEPARACION_PUERTOS * conexiones);
        return Math.max(ANCHO_MINIMO_NODO, Math.max(porTexto, porConexiones));
    }

    private static Map<String, Set<String>> vecinos(GrafoDependencias grafo) {
        Map<String, Set<String>> vecinos = new HashMap<>();
        for (String[] par : grafo.aristas()) {
            vecinos.computeIfAbsent(par[0], k -> new HashSet<>()).add(par[1]);
            vecinos.computeIfAbsent(par[1], k -> new HashSet<>()).add(par[0]);
        }
        return vecinos;
    }

    private static List<String> ordenados(Set<String> paquetes) {
        List<String> lista = new ArrayList<>(paquetes);
        lista.sort(Comparator.naturalOrder());
        return lista;
    }

    private static String minimo(List<String> paquetes) {
        return paquetes.stream().min(Comparator.naturalOrder()).orElse("");
    }

    private static boolean iguales(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /** Traduce los hallazgos de las reglas a estados de nodo y tipos de arista. */
    private record Clasificacion(Map<String, Integer> componenteDe, Set<String> concentrados,
            Set<String> aristasQueViolanCapas) {

        static Clasificacion desde(Resultado resultado) {
            Map<String, Integer> componenteDe = new HashMap<>();
            Set<String> concentrados = new HashSet<>();
            Set<String> violanCapas = new HashSet<>();
            int componente = 0;
            for (ResultadoRegla regla : resultado.porRegla()) {
                for (Violacion v : regla.violaciones()) {
                    switch (v.regla()) {
                        case ReglaCiclos.NOMBRE -> {
                            componente++;
                            for (String paquete : v.ubicacion()) {
                                componenteDe.put(paquete, componente);
                            }
                        }
                        case ReglaConcentracion.NOMBRE -> concentrados.addAll(v.ubicacion());
                        case ReglaIntegridadCapas.NOMBRE -> {
                            if (v.ubicacion().size() == 2) {
                                violanCapas.add(clave(v.ubicacion().get(0), v.ubicacion().get(1)));
                            }
                        }
                        default -> {
                            // Una regla futura sin representación específica se dibuja sin resaltar.
                        }
                    }
                }
            }
            return new Clasificacion(componenteDe, concentrados, violanCapas);
        }

        EstadoNodo estado(String paquete, ConfiguracionReglas config) {
            boolean ciclo = componenteDe.containsKey(paquete);
            boolean concentrado = concentrados.contains(paquete);
            if (ciclo && concentrado) {
                return EstadoNodo.CICLO_Y_CONCENTRACION;
            }
            if (ciclo) {
                return EstadoNodo.CICLO;
            }
            if (concentrado) {
                return EstadoNodo.CONCENTRACION;
            }
            return config.esCompartido(paquete) ? EstadoNodo.COMPARTIDO : EstadoNodo.NORMAL;
        }

        TipoArista tipo(String origen, String destino, ConfiguracionReglas config) {
            if (aristasQueViolanCapas.contains(clave(origen, destino))) {
                return TipoArista.VIOLA_CAPAS;
            }
            Integer c = componenteDe.get(origen);
            if (c != null && c.equals(componenteDe.get(destino))) {
                return TipoArista.CICLO;
            }
            if (config.esCompartido(origen) || config.esCompartido(destino)) {
                return TipoArista.NUCLEO;
            }
            return TipoArista.NORMAL;
        }

        private static String clave(String origen, String destino) {
            return origen + " -> " + destino;
        }
    }
}
