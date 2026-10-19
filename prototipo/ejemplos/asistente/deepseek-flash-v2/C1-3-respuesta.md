# C1 · repetición 3 · deepseek-flash

**Estilo reconocido:** capas en su forma clásica (presentación → negocio → persistencia → dominio), con `dominio` como núcleo de entidades del que dependen todas las capas por diseño. Lo deduzco de que `dominio` tiene Ce=0 y Ca=3, `negocio` orquesta servicio + repositorio y `presentacion` es la única hoja (Ca=0).

**Dudas:** la arista `presentacion -> persistencia` no encaja en ninguna estratificación plausible (un controlador no debería hablar con el repositorio), así que **no la autorizo** y la marco como deriva. También queda por confirmar si `presentacion` debe usar entidades de `dominio` directamente o solo DTOs, y si `negocio` debería depender de una interfaz de repositorio (puerto) en vez de la implementación en `persistencia`.
