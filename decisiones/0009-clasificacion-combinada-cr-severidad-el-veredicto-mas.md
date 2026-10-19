# ADR-0009. Clasificación combinada `cr+severidad` (el veredicto más restrictivo)

- **Estado:** Aceptada
- **Fecha:** 2026-09-02 (retrospectiva)
- **Relacionadas:** ADR-0010

## Contexto

En el experimento, C1 y C3 contienen una deriva real detectada, pero la CR
(90,91 % y 91,67 %) los clasificaba como «aceptables»: la proporción diluye una
violación aislada.

## Decisión

Se añade un segundo criterio, la severidad del peor hallazgo (crítica → no
conforme; mayor → en observación), y el veredicto final es el más restrictivo de
los dos. La CR no se modifica y el modo `cr` reproduce el comportamiento
original para poder comparar. El reporte advierte cuando la severidad endurece
el veredicto.

## Alternativas consideradas

- Subir el umbral de aceptación: desplaza el problema; con más verificaciones
  la dilución vuelve.
- Ponderar la CR por severidad: mezcla extensión y gravedad en un solo número y
  lo hace menos auditable.

## Consecuencias

- Clasifica correctamente los cinco escenarios (el criterio solo por CR, tres).
- En los tres sistemas reales que partían de un estado aceptable (Guava,
  PetClinic, JPetStore), toda deriva inyectada aislada cambia el veredicto
  final aunque la CR apenas varíe (Guava: −1,2 pp).
- Un único ciclo basta para «no conforme»: exigente para sistemas legados; por
  eso el modo es configurable.
- Amenaza a la validez: se diseñó tras observar C1 y C3. Se mitigó validándolo
  después en sistemas que no intervinieron en su diseño (ADR-0016, ADR-0017,
  ADR-0018).

## Respaldo

Hevner et al. (2004); Peffers et al. (2007) (ciclo de diseño y evaluación).
