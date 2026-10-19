package co.javeriana.msw.arquitecta.asistente;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

/**
 * Proveedor, dirección, modelo y clave con que el asistente consulta a un modelo
 * de lenguaje.
 *
 * <p>Se conserva en {@code ~/.arquitecta/asistente.properties}, en la carpeta del
 * usuario y nunca en la del proyecto, para que no termine por descuido en un
 * repositorio. La clave solo se guarda si el usuario lo pide; si no, se toma de
 * la variable de entorno del proveedor ({@code ANTHROPIC_API_KEY},
 * {@code OPENAI_API_KEY}) o se pide en cada sesión.</p>
 */
public record ConfiguracionModelo(Proveedor proveedor, String url, String modelo, String clave) {

    /** Archivo donde se conserva la configuración. */
    public static final Path ARCHIVO = Path.of(System.getProperty("user.home"), ".arquitecta", "asistente.properties");

    public ConfiguracionModelo {
        url = url == null ? "" : url.strip();
        modelo = modelo == null ? "" : modelo.strip();
        clave = clave == null ? "" : clave.strip();
    }

    /** Configuración inicial de un proveedor, con sus valores por defecto. */
    public static ConfiguracionModelo porDefecto(Proveedor proveedor) {
        return new ConfiguracionModelo(proveedor, proveedor.urlPorDefecto(), proveedor.modeloPorDefecto(), "");
    }

    /** La clave efectiva: la indicada o, en su defecto, la de la variable de entorno. */
    public String claveEfectiva() {
        if (!clave.isEmpty() || proveedor.variableEntorno() == null) {
            return clave;
        }
        String entorno = System.getenv(proveedor.variableEntorno());
        return entorno == null ? "" : entorno.strip();
    }

    /** Motivo por el que todavía no puede usarse, o {@code null} si está completa. */
    public String incompleta() {
        if (url.isEmpty()) {
            return "Indique la dirección del servicio.";
        }
        if (modelo.isEmpty()) {
            return "Indique el modelo; «Probar conexión» lista los disponibles.";
        }
        if (proveedor.requiereClave() && claveEfectiva().isEmpty()) {
            return "Indique la clave de API"
                    + (proveedor.variableEntorno() == null ? "." : " o defina la variable " + proveedor.variableEntorno() + ".");
        }
        return null;
    }

    /** Lee la configuración guardada, o la de Anthropic por defecto si no hay ninguna. */
    public static ConfiguracionModelo cargar() {
        return cargar(ARCHIVO);
    }

    static ConfiguracionModelo cargar(Path archivo) {
        Properties props = new Properties();
        if (Files.isRegularFile(archivo)) {
            try (InputStream in = Files.newInputStream(archivo)) {
                props.load(in);
            } catch (IOException e) {
                // Una configuración ilegible equivale a no tener ninguna.
            }
        }
        Proveedor proveedor;
        try {
            proveedor = Proveedor.valueOf(props.getProperty("proveedor", Proveedor.ANTHROPIC.name()));
        } catch (IllegalArgumentException e) {
            proveedor = Proveedor.ANTHROPIC;
        }
        return new ConfiguracionModelo(proveedor,
                props.getProperty("url", proveedor.urlPorDefecto()),
                props.getProperty("modelo", proveedor.modeloPorDefecto()),
                props.getProperty("clave", ""));
    }

    /** Guarda la configuración; la clave, solo si {@code recordarClave}. */
    public void guardar(boolean recordarClave) throws IOException {
        guardar(ARCHIVO, recordarClave);
    }

    void guardar(Path archivo, boolean recordarClave) throws IOException {
        Files.createDirectories(archivo.getParent());
        Properties props = new Properties();
        props.setProperty("proveedor", proveedor.name());
        props.setProperty("url", url);
        props.setProperty("modelo", modelo);
        if (recordarClave && !clave.isEmpty()) {
            props.setProperty("clave", clave);
        }
        try (OutputStream out = Files.newOutputStream(archivo)) {
            props.store(out, "Configuracion del asistente de catalogo de Arquitecta");
        }
        try {
            // En sistemas POSIX, legible solo por su dueño; en Windows la carpeta
            // del usuario ya es privada.
            Files.setPosixFilePermissions(archivo, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException e) {
            // Sistema de archivos sin permisos POSIX.
        }
    }
}
