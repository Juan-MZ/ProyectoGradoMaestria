# ADR-0013. Interfaz Swing y lanzador único

- **Estado:** Aceptada
- **Fecha:** 2026-10-19 (retrospectiva)

## Contexto

Ejecutar el prototipo desde VS Code producía errores de *classpath*, y una PyME
necesita poder usarlo sin conocer Java.

## Decisión

Interfaz gráfica en Swing (incluida en el JDK) y un lanzador único
`arquitecta.bat|.sh` que compila solo cuando el código cambia y expone todos
los usos (interfaz, análisis por consola, pruebas, experimento, estudios).

## Alternativas consideradas

- JavaFX: ya no forma parte del JDK; añadiría una dependencia.
- Interfaz web: exige un servidor; contradice el procesamiento local.

## Consecuencias

- Doble clic para usarlo; mismo binario para consola e interfaz.
- Swing tiene un aspecto menos moderno, compensado con un kit propio de
  componentes.

## Respaldo

RNF1 (bajo costo de adopción) y RNF2 (procesamiento local) de la monografía.
