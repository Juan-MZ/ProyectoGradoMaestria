# C2 · repetición 3 · deepseek-flash

**Estilo reconocido:** estratificación en tres capas (presentacion → negocio → persistencia) con un núcleo de entidades (`dominio`) usado por todas ellas. Lo deduzco de que `presentacion` solo depende de `negocio` y `dominio`, y `negocio` orquesta cálculo y persistencia.

**Dudas:** (1) `negocio.calculo` y `negocio` se referencian mutuamente: es un ciclo real (FF2, crítica) entre un servicio y su calculadora; no lo legitimo como capa aparte ni lo eximo, lo dejo dentro de `negocio` para que quede señalado. (2) No sé si el equipo prevé que `negocio` dependa directamente de `persistencia` o solo de interfaces de repositorio; asumo lo primero por la estratificación clásica, pero conviene confirmarlo. (3) `dominio` queda sin capa por ser núcleo compartido, así ninguna capa incumple por usarlo.
