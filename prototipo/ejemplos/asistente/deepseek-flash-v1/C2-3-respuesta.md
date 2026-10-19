# C2 · repetición 3 · deepseek-flash

## Lectura del perfil

El grafo (una vez ignorado el ciclo) dibuja una **arquitectura en capas clásica**: `presentacion → negocio → persistencia → dominio`, con `dominio` como capa base (Ce=0, Ca=4) y `negocio` como orquestador (depende de persistencia, dominio y de su propio subpaquete `calculo`). El reparto de Ce es plano (máximo 3 sobre 5 paquetes), así que no hay señales de concentración; el riesgo real está en el **ciclo `negocio ↔ negocio.calculo`** (FF2, crítica), que ninguna intención razonable declara y que por tanto no legitimo. Declaro `negocio.calculo` dentro de la capa `negocio` (es un detalle de cálculo interno, no un componente aparte) y dejo la duda en `preguntas`: si el equipo quiere que `CalculadoraTotales` dependa *solo* del dominio, conviene elevarlo a capa propia y entonces el ciclo también saltaría FF1. Permito `presentacion → dominio` porque el controlador manipula entidades del modelo (1 referencia); no lo considero deriva.
