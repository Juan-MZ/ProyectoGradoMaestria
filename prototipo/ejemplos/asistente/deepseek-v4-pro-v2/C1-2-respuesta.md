# C1 · repetición 2 · deepseek-v4-pro

Reconozco una arquitectura en capas de tres niveles —presentación, negocio y persistencia— con `com.caso.dominio` como núcleo compartido sin capa, porque es usado por todas las capas y no depende de nadie. La única dependencia que rompe la estratificación prevista es `presentacion -> persistencia`; la señalo como deriva probable. Fijo el umbral de concentración en 5 porque el mayor Ce sano actual es 3 y deja margen razonable.
