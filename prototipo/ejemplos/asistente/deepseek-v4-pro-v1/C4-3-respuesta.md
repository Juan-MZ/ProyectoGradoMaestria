# C4 · repetición 3 · deepseek-v4-pro

Reconozco una arquitectura en capas por los nombres de paquete: presentación, negocio, persistencia y dominio, con utilidades como capa inferior de soporte. El perfil actual muestra un ciclo presentación-negocio-persistencia (`persistencia → negocio` y `persistencia → presentación`) y un paquete `util` que depende de todas las capas. Asumo capas estrictas: la presentación solo usa negocio y utilidades no depende de capas superiores. El umbral de concentración 3 aísla a `util` (Ce=4) sin marcar a `persistencia` (Ce=3).
