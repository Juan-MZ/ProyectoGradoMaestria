# C3 · repetición 3 · deepseek-v4-pro

Reconozco un estilo de capas clásico: presentación → negocio → persistencia, con dominio como núcleo compartido usado por todas las capas. Las dependencias entre las capas observadas son compatibles. El paquete `util` llama a todas las capas y no recibe dependencias: no es un núcleo compartido, sino concentración de dependencias. Por eso dejo `dominio` y `util` fuera de la integridad de capas, fijo el umbral en 3 para que `util` quede señalado y no lo eximo. Me queda duda de si presentación debe usar `dominio` directamente y qué papel debe tener `util`.
