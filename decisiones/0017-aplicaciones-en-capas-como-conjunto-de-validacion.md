# ADR-0017. Aplicaciones en capas como conjunto de validación independiente

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

Las cuatro bibliotecas no tienen capas declaradas ni el perfil de una PyME, y
el refinamiento de la clasificación y el procedimiento del umbral se diseñaron
mirando el caso de estudio y esas bibliotecas (circularidad).

## Decisión

Se incorporan tres aplicaciones en capas de código abierto, fijadas por
*commit*: Spring Framework PetClinic, MyBatis JPetStore 6 y DDD Sample
(Citerus/Domain Language). Sus catálogos **transcriben la arquitectura que cada
proyecto documenta** (no los propuso el asistente) y se aplican el procedimiento
de umbral y la clasificación sin ajustes. Se compilan con
`arquitecta aplicaciones` (Docker con Maven y JDK 17, o el Maven Wrapper).

## Alternativas consideradas

- Una aplicación de una PyME real: no hubo acceso a una (impedimento
  documentado en la monografía, ADR-0024).
- Spring PetClinic actual: organizada por funcionalidad, no por capas.

## Consecuencias

- PetClinic y JPetStore cumplen su arquitectura documentada (CR 100 %).
- DDD Sample presenta 8 hallazgos reales e interpretables: un DTO de la capa de
  interfaces usado por la capa de aplicación (que además cierra un ciclo), un
  ciclo entre los agregados `cargo` y `handling`, y consumidores JMS de
  infraestructura que invocan servicios de aplicación (deriva o hipótesis de
  capas imprecisa; se discute).
- Siguen siendo aplicaciones de ejemplo, no sistemas de producción.

## Respaldo

Evans (2003), arquitectura en capas de DDD; documentación de cada proyecto
(readme de PetClinic, `src/site` de JPetStore, `package.html` de DDD Sample).
