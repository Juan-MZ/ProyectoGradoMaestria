# C3 · repetición 2 · deepseek-flash

**Estilo reconocido:** estratificación en capas clásica (presentación → negocio → persistencia → dominio), con un paquete `dominio` que actúa como núcleo compartido (Ce=0, Ca=4) y un paquete `util` que hoy depende de *todas* las capas (Ce=4), lo que contradice el papel habitual de unas utilidades: deberían ser hoja, no usuarias de la aplicación.

**Dudas:** `com.caso.util` puede ser (a) utilidades transversales mal ubicadas o (b) la raíz de composición/fachada del sistema; no lo legitimo como exención "compartido" sin confirmarlo. También confirmar si `presentacion → dominio` es previsto (view models) o un atajo.
