# C2 · repetición 2 · deepseek-flash

**Estilo reconocido:** estratificación en capas (presentación → negocio → persistencia) con un núcleo de entidades (`dominio`) del que dependen todas las capas, por lo que lo dejo **sin capa** y fuera de la integridad de capas. `negocio.calculo` lo declaro como capa inferior propia, porque una calculadora de totales debe poder reutilizarse sin arrastrar el servicio que la invoca.

**Dudas:** la arista `negocio.calculo → negocio` (3 referencias) cierra un ciclo con `negocio → negocio.calculo`; parece deriva (la calculadora conoce al servicio) y no la legitimo, pero conviene confirmar cuál es la dirección prevista. Si el equipo considera que `calculo` es solo un detalle interno de `negocio`, bastaría con absorberlo en esa capa, aunque el ciclo seguiría señalado por FF2. Umbral de concentración en 3: el mayor Ce observado en paquetes con rol acotado es 2, y `negocio` llega a 3 por orquestar exactamente tres colaboradores por diseño.
