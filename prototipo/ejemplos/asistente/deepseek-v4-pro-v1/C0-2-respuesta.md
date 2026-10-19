# C0 · repetición 2 · deepseek-v4-pro

Reconozco un estilo en capas simple: `presentacion → negocio → persistencia`, con `com.caso.dominio` como paquete compartido fuera de la integridad de capas. No hay ciclos y la concentración actual es baja (`Ce` máxima 2). La única duda es si `presentacion` debe usar `dominio` directamente o solo a través de `negocio`; por eso la dejo señalada como deriva probable.
