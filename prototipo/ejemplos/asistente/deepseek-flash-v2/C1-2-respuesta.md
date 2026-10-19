# C1 · repetición 2 · deepseek-flash

**Estilo reconocido:** arquitectura en capas (presentación → negocio → persistencia) con un núcleo de dominio compartido. Lo deduzco de la forma del grafo: `dominio` tiene Ce=0 y Ca=3 (todos lo usan, no usa a nadie), `persistencia` solo conoce `dominio` y `negocio` conoce persistencia y dominio, lo que encaja con una cascada de capas.

**Duda principal:** `presentacion -> persistencia`. En una estratificación por capas es un salto (la vista accediendo al repositorio por detrás del servicio). No lo legitimo: lo dejo como deriva probable y pido confirmación. Si fuera intencional (CQRS ligero), habría que declararlo explícitamente.

**Umbral:** lo fijo en 2, el Ce del paquete con más colaboradores legítimos (`negocio`). Con 2, `presentacion` (Ce=3) queda señalado por su dependencia extra hacia persistencia; subirlo a 3 ocultaría precisamente esa anomalía.
