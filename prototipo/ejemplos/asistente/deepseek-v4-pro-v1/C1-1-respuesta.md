# C1 · repetición 1 · deepseek-v4-pro

Reconozco una arquitectura por capas clásica: `presentacion -> negocio -> persistencia -> dominio`. Los nombres y la dirección de las dependencias lo sugieren; `dominio` no depende de nadie y actúa como núcleo. Asumo que las dependencias directas de `presentacion` a `dominio` y a `persistencia` son deriva, no dependencias previstas.
