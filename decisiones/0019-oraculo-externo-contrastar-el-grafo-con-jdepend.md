# ADR-0019. Oráculo externo: contrastar el grafo con JDepend

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

Todo el diagnóstico depende de que el grafo extraído sea correcto, y hasta ahora
solo lo verificaban pruebas escritas por el mismo autor.

## Decisión

Se ejecuta JDepend 2.9.5 (guru.nidi), una herramienta independiente con su
propio lector de bytecode, como proceso aparte sobre los mismos binarios, y se
comparan las aristas entre paquetes del sistema y los componentes cíclicos.
JDepend no se incorpora a la herramienta.

## Alternativas consideradas

- Comparar con Arcan: no se distribuye como biblioteca ejecutable estable.
- Revisar manualmente una muestra: no escala.

## Consecuencias

- 475 aristas coinciden; 20 solo las ve el mecanismo y 1 solo JDepend, y
  **todas** se explican por las reglas de extracción documentadas: argumentos de
  tipos genéricos (JDepend no lee la firma genérica), anotaciones con retención
  `CLASS` (Guava) y un tipo que solo aparece en una variable local (DDD Sample).
- JDepend aborta ante `module-info.class` (constante de Java 9); se le
  entregan las clases extraídas sin descriptores de módulo.

## Respaldo

Clark, JDepend (herramienta de métricas de paquetes de Martin). Evidencia:
`prototipo/ejemplos/estudios/oraculo-jdepend.md`.
