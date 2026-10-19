# C0 · repetición 3 · deepseek-flash

## Explicación

El perfil muestra una **arquitectura en capas** clásica de cuatro niveles: `presentacion` → `negocio` → `persistencia` → `dominio`, con el dominio como núcleo estable (Ce=0, Ca=3) y sin ciclos. El grafo encaja casi perfectamente con esa estratificación: no hay saltos hacia arriba ni inversiones.

La única decisión dudosa es la arista `presentacion -> dominio`: es un salto de dos niveles, pero también es el patrón habitual de un modelo de dominio compartido como vocabulario común (entidades usadas para leer/presentar datos). La propongo como **permitida** por ese motivo, no porque exista; si el equipo quiere forzar que la presentación solo hable con negocio, basta quitarla del `permitido` y aparecerá como deriva.

Sin ciclos, sin paquetes que concentren acoplamiento (Ce máximo = 2), sin código generado ni de terceros: no propongo exenciones. El umbral de concentración lo fijo en 3, un escalón por encima del máximo actual, para que actúe como tope y no como fotografía del estado presente.
