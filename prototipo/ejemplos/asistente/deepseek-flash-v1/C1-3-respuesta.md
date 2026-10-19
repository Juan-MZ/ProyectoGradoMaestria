# C1 · repetición 3 · deepseek-flash

## Explicación

El perfil muestra una estratificación en capas clásica de tres niveles más un modelo de dominio: `presentacion` → `negocio` → `persistencia` → `dominio`. La forma del grafo lo confirma: `dominio` no depende de nadie (Ce=0), `negocio` depende de dominio y persistencia, y `presentacion` es la única con Ca=0 y el mayor Ce. Lo declarable como intención es esa cadena descendente, con `dominio` visible desde todas las capas por su papel de modelo compartido.

Hay una arista que no encaja: `presentacion -> persistencia`. Es un salto de capa (accede a datos saltándose negocio). No la voy a permitir; la registro como deriva probable. También dejo abierta la duda de si `presentacion -> dominio` es intención (usar entidades en la vista) o contaminación del modelo en la capa de entrada.

El umbral de concentración lo fijo en 4: el máximo observado es Ce=3 (`presentacion`), de modo que hoy nadie incumple y se deja solo un punto de holgura para crecer.
