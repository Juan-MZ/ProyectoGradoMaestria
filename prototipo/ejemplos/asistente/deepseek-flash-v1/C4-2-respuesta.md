# C4 · repetición 2 · deepseek-flash

## Lectura del perfil

El perfil sugiere una **arquitectura en capas** clásica: `presentacion → negocio → persistencia → dominio`, con `dominio` como base sin dependencias salientes (Ce=0, Ca=4) y un paquete `util` que hoy actúa como transversal pero que depende de las cuatro capas.

Las aristas `presentacion→negocio`, `presentacion→dominio`, `negocio→dominio`, `negocio→persistencia` y `persistencia→dominio` encajan con esa estratificación. Las que no encajan son `persistencia→negocio` (invierte la capa), `persistencia→presentacion` (salta a la capa más alta y cierra un ciclo de tres paquetes) y el abanico completo de `util` (Ce=4, la concentración más alta del grafo).

Por eso dejo `util` **fuera de las capas pero sin exención**: se evalúa con ciclos y concentración, y con umbral 3 queda señalado. Dudas en `preguntas` en lugar de suponer.
