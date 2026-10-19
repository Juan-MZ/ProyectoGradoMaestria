# C4 · repetición 2 · deepseek-v4-pro

Reconozco una arquitectura por capas: `presentacion` → `negocio` → `persistencia` → `dominio`. El paquete `util` queda fuera de capas por su vocación transversal, aunque hoy está acoplado a todas las capas. Detecto un ciclo `negocio`-`persistencia` y una dependencia `persistencia -> presentacion` que contradicen la estratificación. La duda principal es si `presentacion` puede depender directamente de `dominio` y qué papel debe cumplir `util`.
