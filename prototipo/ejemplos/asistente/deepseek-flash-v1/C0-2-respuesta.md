# C0 · repetición 2 · deepseek-flash

**Estilo reconocido:** estratificación en cuatro niveles (presentación → negocio → persistencia → dominio), típica de una arquitectura en capas con modelo de dominio al fondo y sin dependencias salientes (Ce=0). La evidencia es el grafo: `presentacion` solo entra a `negocio`, `negocio` orquesta `persistencia`, y `persistencia` y `negocio` cierran en `dominio`. El reparto de clases (2 en dominio, 1 en cada resto) sugiere un esqueleto mínimo, no un sistema maduro, así que el catálogo debe servir de plantilla más que de foto.

**Dudas:** la única arista discutible es `presentacion -> dominio` (el controlador referencia `Cliente`); la permito porque va hacia el nivel más bajo y el modelo de dominio suele atravesar capas, pero si el equipo quiere capas estrictas es la primera candidata a deriva. No propongo exenciones: ningún paquete actúa como raíz de composición ni núcleo de anotaciones.
