# C1 · repetición 3 · deepseek-v4-pro

Reconozco una arquitectura en capas: presentacion -> negocio -> persistencia, con `com.caso.dominio` como núcleo compartido sin capa (Ce=0, Ca=3). La dependencia `presentacion -> persistencia` contradice esa estratificación, así que no la permito y la dejo señalada como deriva probable. Fijo el umbral Ce en 2 a partir de los paquetes sanos (`negocio` tiene Ce=2); hoy `presentacion` (Ce=3) lo supera por ese acceso directo. No veo exenciones necesarias.
