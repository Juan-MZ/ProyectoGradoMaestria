# C4 · repetición 2 · deepseek-v4-pro

Reconozco una arquitectura en capas clásica: `presentacion` consume `negocio`, `negocio` consume `persistencia` y `persistencia` accede a `dominio` como núcleo compartido. Dejo `dominio` y `util` fuera de capas; `dominio` por ser entidades compartidas y `util` por ser utilidades, aunque su acoplamiento a todas las capas quedará señalado por concentración. Actualmente hay un ciclo `presentacion -> negocio -> persistencia -> presentacion` y flechas prohibidas desde `persistencia`; las declaro como deriva probable.
