# C2 · repetición 2 · deepseek-v4-pro

Reconozco una arquitectura en capas clásica: presentación → negocio → persistencia, con un dominio compartido en la base. La principal deriva probable es el ciclo entre paquetes `com.caso.negocio` y `com.caso.negocio.calculo`. El Ce máximo actual es 3, así que un umbral de 5 da margen sin ocultar concentraciones. Me queda la duda de si la presentación debe depender directamente del dominio y si esa dependencia de cálculo hacia negocio es prevista o accidental.
