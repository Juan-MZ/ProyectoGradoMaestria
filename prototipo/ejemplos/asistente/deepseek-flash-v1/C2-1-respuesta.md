# C2 · repetición 1 · deepseek-flash

**Estilo reconocido:** estratificación en cuatro niveles (presentación → negocio → persistencia → dominio) sobre una raíz pequeña, típica de un ejercicio MVC. Lo deduzco de que `presentacion` solo entra a `negocio`/`dominio`, `negocio` orquesta `persistencia`, y `dominio` (Clientela/Venta) tiene Ce=0 y Ca=4: es el vocabulario compartido, no una capa de servicio.

**Dudas principales:** (1) `negocio.calculo` y `negocio` se referencian mutuamente — eso es un ciclo real, no una decisión de diseño, y lo dejo como deriva; (2) `presentacion -> dominio` existe hoy: lo permito porque el modelo de dominio es compartido, pero si el equipo quiere que la vista solo hable con `negocio`, hay que declararlo y se señalará; (3) `negocio.calculo` lo absorbo en la capa `negocio` (hereda por prefijo) para no inventar una capa que nadie declaró.
