# C2 · repetición 2 · deepseek-flash

## Lectura del perfil

Reconozco una **arquitectura en capas clásica**: `presentacion → negocio → persistencia → dominio`, con `dominio` como base (Ce=0, Ca=4, solo entidades `Cliente`/`Venta`) y `negocio` como orquestador (usa persistencia y calculo). `negocio.calculo` lo agrupo dentro de la capa `negocio` porque hoy es un detalle interno del servicio; el ciclo que forma con `negocio` no queda legitimado por eso: FF2 lo sigue detectando, y es la deriva más grave del proyecto (severidad crítica).

Dos dudas que no resuelvo por mi cuenta: (1) `presentacion -> dominio` (una referencia): puede ser uso de las entidades como modelo de vista —legítimo si el equipo lo declara— o un salto de capa; por defecto **no** lo permito. (2) Si `calculo` debe ser una subcapa independiente (negocio→calculo sí, calculo→negocio no).

El umbral de concentración lo fijo en 4: el máximo observado es Ce=3 (`negocio`, que ya toca a sus tres colaboradores naturales), así que solo se señalará un paquete que llegue a depender de casi todo el sistema.
