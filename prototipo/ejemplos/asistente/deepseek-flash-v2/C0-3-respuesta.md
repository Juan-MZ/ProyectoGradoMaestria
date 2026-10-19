# C0 · repetición 3 · deepseek-flash

**Estilo reconocido:** estratificación en capas (presentación → negocio → persistencia) con un núcleo de entidades de dominio usado por todas. Se deduce de las cuatro dependencias: presentación solo habla con negocio, negocio coordina persistencia, y `dominio` tiene Ce=0 y Ca=3, señal típica de núcleo compartido.

**Dudas:** la dependencia `presentacion -> dominio` puede ser legítima (usar entidades como modelo de vista) o una deriva (presentación leyendo el modelo persistido sin pasar por negocio). No la declaro permitida ni la prohíbo: dejo `dominio` fuera de la integridad de capas y la señalo como deriva probable para que el equipo decida. También queda la duda de si negocio debería depender de una abstracción de persistencia en vez del paquete concreto.
