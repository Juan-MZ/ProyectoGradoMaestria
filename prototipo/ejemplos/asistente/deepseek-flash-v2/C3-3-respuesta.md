# C3 · repetición 3 · deepseek-flash

**Estilo reconocido:** estratificación en capas clásica (presentacion → negocio → persistencia → dominio), con `com.caso.dominio` como núcleo de entidades compartido (Ce=0, Ca=4) y `com.caso.util` como paquete transversal sin capa.

**Base:** las aristas reales (`presentacion→negocio`, `negocio→persistencia`, `persistencia→dominio`) dibujan esa cadena sin saltos. `presentacion→dominio` lo declaro permitido porque las entidades del núcleo se usan por diseño en todas las capas.

**Duda principal:** `com.caso.util` tiene Ce=4 y depende de **todas** las capas, incluidas las superiores: eso es una inversión de dependencias (utilidades deberían ser hoja), no un uso previsto, así que **no** lo legitimo ni lo exento; queda señalado por concentración y descrito en derivas. El umbral lo fijo en 3 porque los paquetes sanos no pasan de Ce=2.
