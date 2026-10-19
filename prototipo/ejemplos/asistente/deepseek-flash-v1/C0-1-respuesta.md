# C0 · repetición 1 · deepseek-flash

**Estilo reconocido:** estratificación en cuatro capas típica de una aplicación Java de negocio —`presentacion` (controlador) → `negocio` (servicio) → `persistencia` (repositorio) → `dominio` (entidades)—, deducida de las cinco aristas: el dominio no depende de nadie (Ce=0, Ca=3) y el controlador solo sería llamado desde fuera (Ca=0).
**Dudas:** la arista `presentacion -> dominio` (1 referencia) puede ser legítima (el controlador usa `Cliente`/`Venta` como tipos de datos) o una deriva que salta la capa de negocio. No la autorizo: si se legitimara, la herramienta nunca la señalaría. También queda por decidir si `negocio -> persistencia` es lo previsto o si el equipo prefiere invertir la dependencia con interfaces declaradas en el dominio/negocio.
**Umbral:** con solo cuatro paquetes el Ce máximo posible es 3; fijo 2 para que solo se señale un paquete que acapare casi todas las dependencias restantes.
