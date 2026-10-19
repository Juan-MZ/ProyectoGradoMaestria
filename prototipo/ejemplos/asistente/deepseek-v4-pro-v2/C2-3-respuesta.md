# C2 · repetición 3 · deepseek-v4-pro

Reconozco una arquitectura en capas clásica: `presentacion -> negocio -> persistencia`, con `com.caso.dominio` como núcleo compartido sin capa porque lo usan casi todos los paquetes y no depende de nadie. La única duda de diseño es el ciclo `negocio ↔ negocio.calculo`, que parece una deriva y no debe legitimarse.
