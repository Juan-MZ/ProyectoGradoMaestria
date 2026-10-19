package co.javeriana.msw.arquitecta.servicio;

import java.util.List;

/**
 * Descripción estructural de un proyecto, sin su código: los paquetes, cuántas
 * clases tiene cada uno, algunos nombres de clase y las dependencias entre
 * paquetes.
 *
 * <p>Es todo lo que necesita quien deba proponer un catálogo de reglas para un
 * sistema que no lo tiene, y deliberadamente nada más. Cuando esa propuesta la
 * redacta un modelo de lenguaje externo, este perfil es exactamente lo que sale
 * del equipo, y {@link #describir()} es el texto literal que se envía: la
 * interfaz lo muestra antes de enviarlo.</p>
 *
 * @param nombre       nombre del proyecto (el de su carpeta o archivo).
 * @param raiz         raíz de paquetes inferida.
 * @param paquetes     paquetes bajo la raíz, en orden alfabético.
 * @param dependencias dependencias entre esos paquetes.
 */
public record Perfil(String nombre, String raiz, List<Paquete> paquetes, List<Dependencia> dependencias) {

    /** Máximo de nombres de clase que se envían por paquete. */
    public static final int EJEMPLOS_POR_PAQUETE = 8;

    /**
     * @param ejemplos hasta {@link #EJEMPLOS_POR_PAQUETE} nombres simples de clase.
     */
    public record Paquete(String nombre, int clases, int ce, int ca, List<String> ejemplos) {
    }

    /** @param referencias número de referencias entre clases que sustentan la dependencia. */
    public record Dependencia(String origen, String destino, int referencias) {
    }

    public Perfil {
        paquetes = List.copyOf(paquetes);
        dependencias = List.copyOf(dependencias);
    }

    /** Texto con que se describe el proyecto al asistente. */
    public String describir() {
        StringBuilder sb = new StringBuilder();
        sb.append("Proyecto: ").append(nombre).append('\n');
        sb.append("Raíz de paquetes inferida: ").append(raiz).append('\n');
        sb.append("Paquetes: ").append(paquetes.size()).append(" · dependencias entre paquetes: ")
                .append(dependencias.size()).append("\n\n");
        sb.append("PAQUETES (clases; Ce = dependencias salientes, Ca = entrantes; nombres de clase de ejemplo)\n");
        for (Paquete p : paquetes) {
            sb.append("- ").append(p.nombre()).append(" (").append(p.clases()).append(p.clases() == 1 ? " clase" : " clases")
                    .append("; Ce=").append(p.ce()).append(", Ca=").append(p.ca()).append(")");
            if (!p.ejemplos().isEmpty()) {
                sb.append(": ").append(String.join(", ", p.ejemplos()));
                if (p.clases() > p.ejemplos().size()) {
                    sb.append(", …");
                }
            }
            sb.append('\n');
        }
        sb.append("\nDEPENDENCIAS (origen -> destino, referencias entre clases)\n");
        if (dependencias.isEmpty()) {
            sb.append("(ninguna)\n");
        }
        for (Dependencia d : dependencias) {
            sb.append("- ").append(d.origen()).append(" -> ").append(d.destino())
                    .append(" (").append(d.referencias()).append(")\n");
        }
        return sb.toString();
    }
}
