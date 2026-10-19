# C3 · repetición 3 · deepseek-v4-pro

Reconozco una arquitectura en capas: `presentacion -> negocio -> persistencia -> dominio`. El paquete `com.caso.util` parece una utilidad mal acoplada o un composition root no declarado, porque hoy depende de todas las capas y nadie depende de él. Lo dejo fuera de la integridad de capas para que FF3 lo evalúe; fijo el umbral en 3 porque la mayoría de paquetes tiene Ce ≤ 2 y `util` con Ce=4 es el valor atípico. Me quedan dudas sobre el papel real de `util` y sobre si `presentacion -> dominio` debe permitirse.
