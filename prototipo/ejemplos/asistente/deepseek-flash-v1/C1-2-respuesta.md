# C1 · repetición 2 · deepseek-flash

**Estilo reconocido:** capas clásicas (presentación → negocio → persistencia → dominio) sobre un modelo de dominio anémico compartido. La evidencia es la cadena `presentacion → negocio → persistencia → dominio` y un `dominio` con Ce=0 y Ca=3, es decir, una hoja estable de vocabulario que todos consultan.

**Dudas:** el grafo muestra `presentacion → persistencia`, que salta `negocio` y salta la capa de servicio: lo trato como deriva, no como permiso. También hay que decidir si `presentacion` puede tocar entidades de `dominio` directamente (lo he permitido, es la práctica habitual con modelo de dominio) o si debe pasar solo por DTOs de `negocio`. El umbral de concentración se fija en 3 porque con solo 4 paquetes el máximo Ce observado ya equivale a depender de todos los demás; así FF3 no duplica el hallazgo de FF1.
