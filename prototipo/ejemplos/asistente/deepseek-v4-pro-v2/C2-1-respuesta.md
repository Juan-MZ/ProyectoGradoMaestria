# C2 · repetición 1 · deepseek-v4-pro

Reconozco una arquitectura en capas con `presentacion -> negocio -> persistencia`, más un núcleo de `dominio` usado por todas. Veo un ciclo entre `negocio` y `negocio.calculo` que conviene marcar como deriva probable. La única duda es si `presentacion` debe poder depender directamente de `dominio` o si se prefiere aislarla con DTOs de negocio.
