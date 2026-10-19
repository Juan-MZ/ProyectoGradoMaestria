# C2 · repetición 3 · deepseek-v4-pro

Reconozco una arquitectura en capas de tres niveles: `presentacion → negocio → persistencia`, con `com.caso.dominio` como paquete compartido de entidades. Me baso en que dominio tiene Ce=0 y Ca=4: lo usan todos pero no depende de nadie. La única violación estructural visible es el ciclo entre `com.caso.negocio` y `com.caso.negocio.calculo`; el catálogo la señalará como deriva. Propongo umbral de concentración 4 porque el Ce máximo actual es 3. Las dudas quedan en las preguntas.
