# C4 · repetición 1 · deepseek-flash

**Estilo reconocido.** Estratificación en cuatro niveles: `presentacion → negocio → persistencia → dominio`, con `dominio` como núcleo estable (Ce=0, Ca=4) y `presentacion` como única entrada. `util` no encaja en la pila: es un paquete transversal, sin capa, que hoy depende de las cuatro capas (Ce=4) y nadie depende de él (Ca=0).

**Dudas.** El grafo real contradice la pila en dos puntos que no pienso legitimar: `persistencia → negocio` (cierra un ciclo con negocio) y `persistencia → presentacion` (salto hacia arriba). También es dudoso el papel de `util`: o es un núcleo de utilidades que solo debería mirar a `dominio`, o es un paquete mal ubicado que arrastra responsabilidades de todas las capas.

**Umbral.** Los Ce observados son {0, 2, 2, 3, 4}: las capas estratificadas llegan como máximo a 3, y solo `util` (4) lo supera; propongo 3 para señalarlo sin marcar a las capas legítimas.
