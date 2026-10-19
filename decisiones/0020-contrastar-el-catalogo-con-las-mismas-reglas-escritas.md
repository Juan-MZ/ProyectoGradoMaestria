# ADR-0020. Contrastar el catálogo con las mismas reglas escritas en ArchUnit

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

La pregunta segura de un jurado es qué aporta el mecanismo frente a ArchUnit,
sobre el que se apoya.

## Decisión

Se escriben las tres reglas del caso de estudio con ArchUnit
(`EquivalenteArchUnit.java`: arquitectura en capas, *slices* libres de ciclos y
una condición propia para Ce, que ArchUnit no ofrece) y se comparan con el
catálogo en los cinco escenarios.

## Alternativas consideradas

- Argumentarlo solo de forma cualitativa.

## Consecuencias

- Detección por regla idéntica en los cinco escenarios.
- 33 líneas de Java (con código propio para Ce) frente a 14 líneas de
  propiedades.
- ArchUnit entrega pasa/falla por regla; el mecanismo entrega además la CR, el
  estado con severidad, la evidencia por hallazgo, el alcance del diagnóstico y
  la tendencia. Ese es el aporte: el proceso de evaluación, no el motor de
  reglas.

## Respaldo

Evidencia: `prototipo/ejemplos/archunit/equivalente-archunit.md`.
