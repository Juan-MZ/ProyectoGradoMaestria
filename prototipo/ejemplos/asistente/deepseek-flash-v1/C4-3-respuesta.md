# C4 · repetición 3 · deepseek-flash

**Estilo reconocido:** estratificación en cuatro capas —presentación → negocio → persistencia → dominio—, con `dominio` como núcleo estable (Ce=0, Ca=4) y `util` como paquete transversal sin capa. La dirección dominante del grafo confirma esa lectura.

**Dudas:** el grafo muestra tres problemas que no voy a legitimar: `persistencia -> negocio` (inversión que además cierra un ciclo con `negocio -> persistencia`), `persistencia -> presentacion` (salto de capa) y `util` con Ce=4, es decir, acoplado a las cuatro capas. Ninguna de las tres se declara permitida. Como los paquetes son de una sola clase, el umbral de concentración lo fijo en 3 (el máximo del resto de paquetes) para que señale precisamente a `util`.
