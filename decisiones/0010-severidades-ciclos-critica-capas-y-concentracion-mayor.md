# ADR-0010. Severidades: ciclos «crítica», capas y concentración «mayor»

- **Estado:** Aceptada
- **Fecha:** 2026-10-19 (respaldo añadido)

## Contexto

La tabla del catálogo asignaba severidades sin citar respaldo.

## Decisión

Se mantienen las severidades y se documenta su respaldo: los ciclos son la
única violación que impide por completo el orden de los paquetes y que propaga
cambios en ambos sentidos (ADP); la ruptura de capas y la concentración son
violaciones localizadas que pueden corregirse moviendo una dependencia o
dividiendo un paquete.

## Alternativas consideradas

- Severidad única para todo: la clasificación por severidad perdería su
  sentido.
- Severidades calculadas por instancia (como el ATDI): requieren un modelo
  entrenado y métricas de tamaño que superan el alcance.

## Consecuencias

- Coherente con la priorización de *smells* de la literatura.
- Es una convención declarada, no una medida: el modo `cr` permite prescindir
  de ella.

## Respaldo

Martin (2003) (principio de dependencias acíclicas); Sas et al. (2019) y Sas y
Avgeriou (2023, IEEE TSE 49(8)) estiman mayor severidad y deuda para los ciclos;
Arcelli Fontana et al. (2016) tratan ciclos y concentración como *smells* de
inestabilidad.
