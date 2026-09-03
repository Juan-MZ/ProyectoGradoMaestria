package co.javeriana.msw.arquitecta;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Arnés de pruebas mínimo, sin dependencias externas.
 *
 * <p>El prototipo declara como requisito no funcional poder ejecutarse en un
 * entorno de desarrollo convencional sin infraestructura adicional; someter sus
 * propias pruebas a esa misma restricción mantiene la coherencia y garantiza que
 * la batería sea ejecutable en cualquier máquina con un JDK, sin repositorio de
 * artefactos ni conexión a internet.</p>
 */
public final class Verificador {

    private static final PrintStream SALIDA =
            new PrintStream(System.out, true, StandardCharsets.UTF_8);

    private final List<String> fallos = new ArrayList<>();
    private int totales;
    private String grupoActual = "";

    public void grupo(String nombre) {
        grupoActual = nombre;
        SALIDA.println("\n— " + nombre);
    }

    public void comprueba(String caso, boolean condicion) {
        totales++;
        if (condicion) {
            SALIDA.println("   [ok]    " + caso);
        } else {
            SALIDA.println("   [FALLA] " + caso);
            fallos.add(grupoActual + " :: " + caso);
        }
    }

    public void igual(String caso, Object esperado, Object obtenido) {
        boolean iguales = esperado == null ? obtenido == null : esperado.equals(obtenido);
        totales++;
        if (iguales) {
            SALIDA.println("   [ok]    " + caso);
        } else {
            SALIDA.println("   [FALLA] " + caso + " — esperado <" + esperado + ">, obtenido <" + obtenido + ">");
            fallos.add(grupoActual + " :: " + caso + " (esperado " + esperado + ", obtenido " + obtenido + ")");
        }
    }

    public void igual(String caso, double esperado, double obtenido, double tolerancia) {
        boolean iguales = Math.abs(esperado - obtenido) <= tolerancia;
        totales++;
        if (iguales) {
            SALIDA.println("   [ok]    " + caso);
        } else {
            SALIDA.println("   [FALLA] " + caso + " — esperado " + esperado + ", obtenido " + obtenido);
            fallos.add(grupoActual + " :: " + caso + " (esperado " + esperado + ", obtenido " + obtenido + ")");
        }
    }

    /** Comprueba que un bloque rechace una entrada inválida con la excepción esperada. */
    public void rechaza(String caso, Class<? extends Throwable> esperada, Runnable bloque) {
        totales++;
        try {
            bloque.run();
            SALIDA.println("   [FALLA] " + caso + " — no se lanzó " + esperada.getSimpleName());
            fallos.add(grupoActual + " :: " + caso + " (no se lanzó excepción)");
        } catch (Throwable t) {
            if (esperada.isInstance(t)) {
                SALIDA.println("   [ok]    " + caso);
            } else {
                SALIDA.println("   [FALLA] " + caso + " — se lanzó " + t.getClass().getSimpleName());
                fallos.add(grupoActual + " :: " + caso + " (excepción inesperada: " + t + ")");
            }
        }
    }

    public int resumen() {
        SALIDA.println("\n" + "=".repeat(78));
        SALIDA.printf("  %d comprobaciones · %d superadas · %d fallidas%n",
                totales, totales - fallos.size(), fallos.size());
        if (!fallos.isEmpty()) {
            SALIDA.println("\n  Fallos:");
            fallos.forEach(f -> SALIDA.println("   · " + f));
        }
        SALIDA.println("=".repeat(78));
        return fallos.isEmpty() ? 0 : 1;
    }
}
