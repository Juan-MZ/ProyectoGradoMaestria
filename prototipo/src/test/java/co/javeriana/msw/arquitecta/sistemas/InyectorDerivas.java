package co.javeriana.msw.arquitecta.sistemas;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Inyecta derivas controladas en un sistema distribuido como {@code .jar}.
 *
 * <p>Cada deriva es una clase mínima, generada directamente en formato
 * {@code .class}, que declara un campo cuyo tipo pertenece al paquete destino:
 * añade exactamente una dependencia entre paquetes y nada más. Generar el
 * <em>bytecode</em> en lugar de compilar evita depender del resto de bibliotecas
 * del sistema y permite referenciar cualquier clase, sea o no pública, de modo
 * que la deriva se coloca exactamente donde el diseño del experimento lo pide.</p>
 */
final class InyectorDerivas {

    private InyectorDerivas() {
    }

    /**
     * Clase {@code paquete.nombre} con un campo por cada tipo indicado (nombres
     * binarios, con puntos).
     */
    static byte[] clase(String paquete, String nombre, String... tipos) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(0xCAFEBABE);
            out.writeShort(0);
            out.writeShort(52); // Java 8: lo lee cualquier importador.
            // Reserva de constantes: 1 esta clase, 3 Object, y por campo, nombre y descriptor.
            int constantes = 4 + 2 * tipos.length;
            out.writeShort(constantes + 1);
            out.writeByte(7);                       // #1 Class -> #2
            out.writeShort(2);
            out.writeByte(1);                       // #2 Utf8 nombre interno
            out.writeUTF((paquete + "." + nombre).replace('.', '/'));
            out.writeByte(7);                       // #3 Class -> #4
            out.writeShort(4);
            out.writeByte(1);                       // #4 Utf8 java/lang/Object
            out.writeUTF("java/lang/Object");
            for (int i = 0; i < tipos.length; i++) {
                out.writeByte(1);                   // nombre del campo
                out.writeUTF("referencia" + i);
                out.writeByte(1);                   // descriptor del campo
                out.writeUTF("L" + tipos[i].replace('.', '/') + ";");
            }
            out.writeShort(0x0001 | 0x0010 | 0x1000); // public final synthetic
            out.writeShort(1);
            out.writeShort(3);
            out.writeShort(0);                      // interfaces
            out.writeShort(tipos.length);           // campos
            for (int i = 0; i < tipos.length; i++) {
                out.writeShort(0x0002);             // private
                out.writeShort(5 + 2 * i);
                out.writeShort(6 + 2 * i);
                out.writeShort(0);
            }
            out.writeShort(0);                      // métodos
            out.writeShort(0);                      // atributos
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Copia el {@code .jar} original añadiendo las clases indicadas (ruta interna → contenido). */
    static void jarConClases(Path original, Path destino, Map<String, byte[]> clases) throws IOException {
        Files.createDirectories(destino.toAbsolutePath().getParent());
        try (InputStream in = Files.newInputStream(original);
             JarInputStream jar = new JarInputStream(in);
             OutputStream salida = Files.newOutputStream(destino);
             JarOutputStream nuevo = jar.getManifest() == null ? new JarOutputStream(salida)
                     : new JarOutputStream(salida, jar.getManifest())) {
            JarEntry entrada;
            byte[] buffer = new byte[64 * 1024];
            java.util.Set<String> copiadas = new java.util.HashSet<>();
            while ((entrada = jar.getNextJarEntry()) != null) {
                // Algunos empaquetados repiten entradas de directorio; se copia la primera.
                if (!copiadas.add(entrada.getName())) {
                    continue;
                }
                nuevo.putNextEntry(new JarEntry(entrada.getName()));
                int n;
                while ((n = jar.read(buffer)) > 0) {
                    nuevo.write(buffer, 0, n);
                }
                nuevo.closeEntry();
            }
            for (Map.Entry<String, byte[]> c : clases.entrySet()) {
                nuevo.putNextEntry(new JarEntry(c.getKey()));
                nuevo.write(c.getValue());
                nuevo.closeEntry();
            }
        }
    }
}
