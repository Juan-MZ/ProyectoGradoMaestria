# C3 · repetición 1 · deepseek-flash

**Estilo reconocido:** estratificación en cuatro niveles (presentacion → negocio → persistencia → dominio), con un paquete `util` fuera de capas. Se basa en que `dominio` no tiene dependencias salientes (Ce=0) y es referenciado por todos, y en que `negocio` coordina `persistencia` y `dominio`.

**Dudas:** (1) `presentacion -> dominio` salta una capa: no lo legitimito y lo señalo como deriva, pero puede ser intencionado (usar entidades del dominio en el controlador); conviene confirmarlo. (2) `util` tiene Ce=4 y Ca=0: depende de las cuatro capas y nadie depende de él, así que no es un hub compartido legítimo sino el candidato real a concentración; no lo declaro capa (quedaría fuera solo de FF1, pero FF3 lo evalúa) ni lo eximo. (3) Sin ciclos hoy, pero `util` es el punto por donde podrían aparecer.

Umbral: la distribución de Ce es 0, 1, 2, 2, 4; los paquetes estratificados no pasan de 2, así que fijo 3 para dejar margen y que solo `util` (4) se señale.
