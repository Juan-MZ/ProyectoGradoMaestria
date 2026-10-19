# C4 · repetición 1 · deepseek-flash

**Estilo reconocido:** estratificación en cuatro capas (presentacion → negocio → persistencia → dominio), con `dominio` como núcleo de entidades del que dependen todas por diseño. Lo sugiere la forma del grafo: `presentacion` y `negocio` solo miran hacia abajo, y `dominio` tiene Ce=0 y Ca=4.

**Dudas:** hay tres aristas que no encajan con esa estratificación y que no pienso legitimar: `persistencia -> negocio` (cierra un ciclo con `negocio -> persistencia`), `persistencia -> presentacion` (inversión clara) y un paquete `util` con Ce=4 que depende de las cuatro capas pero no es usado por nadie (Ca=0), por lo que no puede tratarse como núcleo compartido ni como raíz de composición sin más información. El umbral lo fijo en 3, el mayor Ce de los paquetes con responsabilidad acotada (dominio 0, presentacion 2, negocio 2, persistencia 3), de modo que `util` (4) quede señalado en lugar de encubierto.
