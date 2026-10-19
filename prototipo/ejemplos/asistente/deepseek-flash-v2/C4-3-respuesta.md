# C4 · repetición 3 · deepseek-flash

**Estilo reconocido.** Estratificación en tres capas —`presentacion → negocio → persistencia`— sobre un núcleo de entidades (`dominio`) usado por todas, más un paquete `util` que en el perfil no actúa como utilidad transversal sino como consumidor de las cuatro capas (Ce=4). Lo que sostiene esta lectura: `presentacion` solo referencia `negocio` y `dominio`; `negocio` referencia `persistencia` y `dominio`; `dominio` no depende de nada (Ce=0, Ca=4).

**Dudas.** `persistencia → negocio` y `persistencia → presentacion` invierten la estratificación: no las autorizo, son derivas. `util` dependiendo de `presentacion`, `negocio` y `persistencia` no encaja con un paquete de utilidades; o es un orquestador mal ubicado o un cajón de sastre, así que lo dejo señalado por concentración en lugar de eximirlo. Dejo `dominio` sin capa por ser núcleo compartido; si el equipo prefiere declararlo capa inferior, se ajusta.
