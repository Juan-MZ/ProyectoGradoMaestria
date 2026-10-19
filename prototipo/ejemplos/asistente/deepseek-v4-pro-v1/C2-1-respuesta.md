# C2 · repetición 1 · deepseek-v4-pro

Reconozco una arquitectura en capas con dominio como núcleo compartido: `presentacion -> negocio -> persistencia`, todas sobre `dominio`. La duda principal es `negocio.calculo`: hoy forma un ciclo con `negocio`; lo traté como subpaquete interno de `negocio`, pero ese ciclo queda señalado como deriva probable porque no respeta la dependencia esperada de un detalle interno. También dejo en preguntas si `presentacion` debe ver `dominio` directamente.
