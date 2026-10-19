# C1 · repetición 3 · deepseek-v4-pro

Reconozco una arquitectura en capas clásica: `presentacion` → `negocio` → `persistencia` → `dominio`. El perfil muestra un salto de capa: `presentacion` depende de `persistencia`, cuando debería llegar a ella a través de `negocio`. Con las dependencias autorizadas, el Ce máximo previsto es 2, y el Ce=3 actual de `presentacion` delata ese acceso adicional. No veo necesidad de exenciones.
