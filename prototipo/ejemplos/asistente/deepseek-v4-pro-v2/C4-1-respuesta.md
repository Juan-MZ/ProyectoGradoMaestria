# C4 · repetición 1 · deepseek-v4-pro

Reconozco una arquitectura en capas: presentación → negocio → persistencia → dominio, con dominio como núcleo compartido. Declaro `util` como capa base sin dependencias para detectar su acoplamiento actual hacia todas las capas. La deriva principal es `persistencia` dependiendo de `negocio` y `presentacion`, lo que crea un ciclo. Dudo si `presentacion` debe usar `dominio` directamente y si `util` puede depender de `dominio`.
