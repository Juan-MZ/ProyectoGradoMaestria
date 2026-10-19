# C1 · repetición 1 · deepseek-flash

El perfil muestra una estratificación en capas clásica: `presentacion` (controlador) → `negocio` (servicio) → `persistencia` (repositorio) → `dominio`, con `dominio` como núcleo de entidades que todas las capas usan por diseño (Ce=0, Ca=3). Lo reconozco por la forma del grafo: cada capa intermedia depende solo de la inferior y no hay ciclos.

La duda principal es la arista `presentacion -> persistencia`: en un esquema de capas estricto es un salto de capa (el controlador debería pasar por `negocio`), así que NO la voy a permitir y la registro como deriva probable. Por eso dejo `dominio` sin capa (núcleo compartido) en lugar de declararlo capa inferior: así sus aristas no distorsionan la matriz, aunque sigue evaluado para ciclos y concentración.

Sobre el umbral: los paquetes sanos tienen Ce entre 0 y 2 (`negocio`=2 es el mayor de los que cooperan), así que fijo el umbral en 2; `presentacion` queda señalado con Ce=3 justamente por la dependencia directa a `persistencia` (corregida la deriva bajaría a 2 y dejaría de señalarse).
