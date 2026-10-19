# ADR-0024. Contraste explícito de la hipótesis y evaluación de utilidad con usuarios

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

La hipótesis de trabajo del anteproyecto tiene tres componentes: el mecanismo
(i) detecta la deriva, (ii) diferencia objetivamente un estado aceptable de uno
problemático y (iii) produce evidencia útil para decisiones de mantenimiento,
sin documentación externa. La monografía solo contrastaba una versión reducida
(detección y descenso de la CR).

## Decisión

Se añade una sección que contrasta cada componente con su evidencia y declara
qué no quedó demostrado. Para (iii) se distingue lo que se demuestra por
construcción y en sistemas reales (cada hallazgo identifica regla, paquetes,
severidad y referencias hasta el método y la línea; la interpretación de DDD
Sample y de Jackson muestra decisiones concretas que la evidencia permite) de lo
que exige personas: que equipos reales juzguen el reporte útil. Esto último **no
se realizó**: no hubo acceso a equipos de PyMES ni a evaluadores durante la
ejecución, y se documenta como impedimento con el protocolo propuesto para
hacerlo (cuestionario breve tras la lectura de un reporte).

## Alternativas consideradas

- Declarar la hipótesis confirmada sin el componente (iii): sobreafirmación.
- Hacer la evaluación con el propio autor: no es evidencia independiente.

## Consecuencias

- La conclusión distingue entre «confirmada», «confirmada parcialmente» y «no
  evaluada».
- Si el autor consigue evaluadores antes de la sustentación, la sección y esta
  ADR se actualizan.

## Respaldo

Hevner et al. (2004): la utilidad del artefacto se demuestra con métodos de
evaluación adecuados; Sas y Avgeriou (2023) validan su índice con 16
profesionales, referencia del tipo de evaluación que queda pendiente.
