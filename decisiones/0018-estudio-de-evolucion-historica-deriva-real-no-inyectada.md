# ADR-0018. Estudio de evolución histórica: deriva real, no inyectada

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

Un jurado puede objetar que todas las derivas detectadas fueron fabricadas. Hace
falta mostrar que el mecanismo detecta deriva que nadie introdujo a propósito,
y ejercitar la lectura por tendencia del proceso (P6).

## Decisión

Se aplica el catálogo declarado para una versión de referencia a versiones
publicadas antes y después de ella, descargadas de Maven Central con su SHA-1:
Jackson Databind (13 versiones, 2.4-2.22), Guava (12, 18.0-33.7) y Commons Lang
(10, 3.1-3.21). Las versiones posteriores responden a la pregunta operativa: si
el equipo hubiera declarado esa arquitectura, ¿qué deriva habría aparecido
después?

## Alternativas consideradas

- Reconstruir la historia desde los repositorios git: obliga a compilar
  versiones antiguas con sus herramientas de época; los binarios publicados son
  el artefacto oficial y reproducible.

## Consecuencias

- Deriva real posterior a la referencia: Jackson 2.19.0 introduce
  `introspect → deser.impl` (`POJOPropertiesCollector` invoca a
  `UnwrappedPropertyHandler`) y una nueva concentración; Commons Lang 3.21.0,
  una concentración en `builder`. Guava se mantiene en 100 % en sus doce versiones.
- Hacia atrás se ve cómo crecieron los ciclos (Commons Lang: de 2 a 6 paquetes;
  Jackson: de 16 a 19).
- Los catálogos son hipótesis: lo detectado es deriva respecto de ellos.

## Respaldo

Lehman (1980), leyes de evolución; Li et al. (2022), la erosión se manifiesta
en la evolución. Evidencia: `prototipo/ejemplos/estudios/evolucion.md`.
