# C0 · repetición 3 · deepseek-v4-pro

Reconozco una arquitectura en capas: presentación → negocio → persistencia, con `com.caso.dominio` como núcleo compartido usado por todas las capas (Ce=0, Ca=3). No hay ciclos ni concentración: todos los Ce están entre 0 y 2. La principal duda es si el acceso directo de presentación a dominio es previsto; lo dejo fuera de la integridad de capas para no marcarlo como deriva y lo pregunto.
