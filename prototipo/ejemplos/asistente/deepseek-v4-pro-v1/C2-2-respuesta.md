# C2 · repetición 2 · deepseek-v4-pro

Reconozco una arquitectura en capas: `presentacion` → `negocio` → `persistencia` → `dominio`, con `com.caso.negocio.calculo` como subpaquete de la capa de negocio. Las dependencias existentes son compatibles con esa estratificación, salvo el ciclo entre `com.caso.negocio` y `com.caso.negocio.calculo`, que FF2 detectará y que dejo señalado como deriva probable. Propongo umbral de concentración 5 porque el Ce máximo actual es 3 y quiero margen para el coordinador de negocio sin ocultar concentraciones futuras.
