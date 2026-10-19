# ADR-0016. Validación complementaria por inyección controlada de derivas en sistemas reales

- **Estado:** Aceptada
- **Fecha:** 2026-10-19 (retrospectiva)

## Contexto

El caso de estudio permite saber qué derivas contiene, pero es pequeño y
sintético; un sistema real es grande, pero no se sabe qué derivas contiene.

## Decisión

Se inyectan derivas conocidas en binarios reales: clases mínimas generadas en
bytecode que añaden exactamente una dependencia entre paquetes, ubicadas de forma
determinista para activar solo su regla. Se mide la diferencia con la versión
original. No sustituye al experimento de OBJ4, que se mantiene sobre un único
sistema como aprobó el anteproyecto.

## Alternativas consideradas

- Pedir a desarrolladores que introduzcan derivas: no reproducible y sin acceso
  a equipos.
- Usar solo el caso de estudio: no permite generalizar la detección.

## Consecuencias

- 31 de 31 derivas detectadas en 7 sistemas, sin hallazgos espurios.
- La inyección mide la exhaustividad de extracción y detección, no la capacidad
  de encontrar deriva *real*: por eso se complementa con ADR-0018.
- En sistemas estrictamente estratificados (JPetStore), toda arista que cierra
  un ciclo apunta hacia arriba y viola también las capas; el planificador lo
  admite solo en ese caso y lo declara.

## Respaldo

Wohlin et al. (2012): inyección de defectos como técnica de evaluación
controlada. Evidencia: `prototipo/ejemplos/sistemas/`.
