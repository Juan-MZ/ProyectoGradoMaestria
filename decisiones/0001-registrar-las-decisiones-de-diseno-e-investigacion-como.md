# ADR-0001. Registrar las decisiones de diseño e investigación como ADR

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

El trabajo acumula decisiones que cambian el artefacto, el método de evaluación o
la forma de interpretar los resultados. Varias se apartan de lo que el
anteproyecto aprobado dice literalmente (por ejemplo, la forma de calcular la
Tasa de Conformidad) y un jurado preguntará, con razón, por qué. Sin un registro
explícito, la justificación queda dispersa entre el historial de versiones, los
comentarios del código y la monografía.

## Decisión

Cada decisión con consecuencias conceptuales se registra en esta carpeta como un
*Architecture Decision Record* con el formato de Nygard (contexto, decisión,
consecuencias), ampliado con dos secciones propias de un trabajo de
investigación: **alternativas consideradas** y **respaldo** (bibliográfico o
empírico). Las decisiones tomadas antes de crear la carpeta se registran de
forma retrospectiva y se señalan como tales. Una ADR no se edita para cambiar
su decisión: se crea otra que la sustituye y se marca la anterior como
«sustituida».

## Alternativas consideradas

- Documentar las decisiones solo en la monografía: obliga al lector del
  repositorio a buscar en un PDF de cien páginas y mezcla la justificación con
  la exposición.
- Comentarios en el código: no cubren las decisiones de método ni de
  evaluación.

## Consecuencias

- La monografía puede citar la ADR que justifica cada desviación.
- Las ADR retrospectivas reconstruyen el razonamiento, pero no prueban que se
  hiciera en su momento; se fechan con la fecha del cambio en el historial.

## Respaldo

Tyree y Akerman (2005), *Architecture decisions: demystifying architecture*,
IEEE Software 22(2):19-27; Nygard (2011), *Documenting Architecture
Decisions*. El propio marco de referencia de la monografía cita los ADR como
práctica de gobernanza.
