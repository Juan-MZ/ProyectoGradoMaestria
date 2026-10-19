# C4 · repetición 2 · deepseek-flash

## Lectura del perfil

Reconozco una **arquitectura en capas** clásica: `presentacion → negocio → persistencia → dominio`, con un dominio que solo es usado (Ca=4, Ce=0) y por tanto actúa como núcleo compartido. La declaración es plausible con solo 5 paquetes y un controlador/servicio/repositorio por capa.

Las dudas vienen de tres aristas que contradicen esa estratificación: `persistencia → presentacion` (inversión clara), `persistencia → negocio` (cierra un ciclo con negocio) y `util` con Ce=4, que conoce las cuatro capas. No las voy a legitimar: las dejo como derivas. El domino lo declaro como capa inferior (permite vigilar que no empiece a depender de nadie), aunque también sería válido dejarlo sin capa como núcleo compartido.

El umbral lo fijo en 3 porque los paquetes de capa tienen Ce entre 2 y 3; solo `util` (Ce=4) lo supera, y no lo eximo porque una clase de utilidades genéricas no tiene por qué conocer todo el sistema.
