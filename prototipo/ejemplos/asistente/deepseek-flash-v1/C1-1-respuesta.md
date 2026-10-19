# C1 · repetición 1 · deepseek-flash

Reconozco una arquitectura en capas con dominio en la base: presentación → negocio → persistencia → dominio. La única dependencia que contradice claramente esa estratificación es `presentacion -> persistencia`, que salta negocio. Mantengo `presentacion -> dominio` como plausible, pero lo dejo confirmado en preguntas. El umbral de concentración queda en 2 porque el máximo esperado para presentación, usando negocio y dominio, es 2; su Ce actual de 3 delata el acceso directo al repositorio.
