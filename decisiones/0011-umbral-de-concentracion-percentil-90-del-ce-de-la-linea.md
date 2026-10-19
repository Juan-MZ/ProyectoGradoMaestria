# ADR-0011. Umbral de concentración: percentil 90 del Ce de la línea base

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

El umbral `Ce > 3` del caso de estudio no tenía respaldo, y los catálogos
propuestos por el asistente usaban umbrales distintos sin un procedimiento
común. Un umbral es el parámetro más fácil de ajustar para «no ver» una
deriva.

## Decisión

Se adopta un procedimiento de derivación estadística: el umbral es el
**percentil 90 del acoplamiento eferente de los paquetes no exentos** de la
versión que se acepta como línea base (la franja de «riesgo muy alto» de Alves
et al.). Se calibró sobre cuatro bibliotecas (78 paquetes) y se aplicó sin
ajustes a un conjunto de validación independiente (tres aplicaciones en capas y
el caso de estudio). El estudio se reproduce con `arquitecta estudios umbral`.

## Alternativas consideradas

- Un umbral absoluto común (el p90 conjunto, Ce > 10): el Ce está acotado por
  el número de paquetes, de modo que en aplicaciones pequeñas no señalaría nunca
  nada (máximo observado 2-3) y en Jackson señalaría seis paquetes.
- Dejarlo a juicio de quien configura: es lo que hacía el asistente y produjo
  umbrales elegidos justo por encima del paquete más acoplado.

## Consecuencias

- En el caso de estudio, el p90 de C0 es 2; con 2 en lugar de 3, C1 y C2 suman
  un hallazgo de FF3 (la arista inyectada también eleva el Ce del origen) y
  **ninguno de los cinco veredictos cambia**: la clasificación es robusta al
  umbral. Se conserva 3 en el experimento porque estaba fijado de antemano.
- En sistemas muy pequeños el p90 coincide con el máximo y cualquier
  dependencia nueva del paquete más acoplado se señala: es el comportamiento de
  línea base buscado, con el costo de hallazgos solapados.

## Respaldo

Alves, Ypma y Visser (2010), *Deriving metric thresholds from benchmark data*,
ICSM; Lanza y Marinescu (2006), umbrales estadísticos en *Object-Oriented
Metrics in Practice*. Evidencia: `prototipo/ejemplos/estudios/umbral.md`.
