# ADR-0021. Guardia de versión de bytecode (Java ≤ 23)

- **Estado:** Aceptada
- **Fecha:** 2026-10-19 (retrospectiva)

## Contexto

ArchUnit 1.3.0 lee clases hasta Java 23 y **omite en silencio** las posteriores.
Con un JDK 26 instalado, un proyecto compilado sin `--release` producía un grafo
vacío o incompleto: un falso «aceptable».

## Decisión

El resolvedor de proyectos lee la versión del primer `.class` y rechaza, con
un mensaje explicativo, el análisis si supera la versión 23 (regla de
extracción R7).

## Alternativas consideradas

- Actualizar ArchUnit: la guardia sigue siendo necesaria para la próxima versión
  de Java.

## Consecuencias

- Un análisis imposible nunca se reporta como conforme.

## Respaldo

Prueba en `PruebasServicio`.
