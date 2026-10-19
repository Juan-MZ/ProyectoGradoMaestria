# C0 · repetición 1 · deepseek-flash

Reconozco una arquitectura en capas con un núcleo de dominio compartido: `presentacion -> negocio -> persistencia`, y `dominio` usado por las tres capas. Las dependencias observadas encajan con ese estilo; la única duda es si `presentacion` debe poder usar `dominio` directamente o solo a través de `negocio`. No hay paquetes con concentración saliente alta: el máximo Ce es 2. No propongo exenciones porque no hay evidencia de código generado, de terceros ni de una fachada que concentre dependencias por diseño.
