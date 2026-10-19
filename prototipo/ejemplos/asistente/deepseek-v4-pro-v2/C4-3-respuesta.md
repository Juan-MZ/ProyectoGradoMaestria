# C4 · repetición 3 · deepseek-v4-pro

Reconozco una arquitectura en capas tradicional: `presentacion → negocio → persistencia → dominio`. Me baso en los nombres de paquetes y en el flujo `ControladorVenta → ServicioVenta → RepositorioVenta → Cliente/Venta`. Dejo `util` sin capa para que la regla de concentración lo evalúe. Tengo dudas sobre si `presentacion` puede usar directamente el dominio, si `negocio` debe depender de `persistencia` o se prefiere inversión de dependencia, y el papel de `util`.
