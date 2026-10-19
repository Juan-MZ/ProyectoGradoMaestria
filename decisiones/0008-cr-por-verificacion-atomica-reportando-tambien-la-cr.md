# ADR-0008. CR por verificación atómica, reportando también la CR por regla del anteproyecto

- **Estado:** Aceptada
- **Fecha:** 2026-10-19
- **Relacionadas:** ADR-0009

## Contexto

El anteproyecto define la Tasa de Conformidad como *reglas cumplidas / reglas
totales*. Con tres reglas, esa fórmula solo admite cuatro valores (0, 33,3,
66,7 y 100 %): una sola deriva aislada deja el sistema en 66,7 % —«no
conforme» según los umbrales 85/70— sea cual sea su tamaño, y cien derivas en la
misma regla producen el mismo valor que una. La monografía calculaba la CR por
verificación atómica sin presentar esta diferencia como una decisión.

## Decisión

La CR de la clasificación es la proporción de verificaciones atómicas superadas,
`CR = 100·(V−I)/V`. La formulación del anteproyecto se calcula y se reporta
siempre como **CR por regla** (consola, CSV, JSON, interfaz), sin intervenir en
la clasificación, para que las dos lecturas puedan contrastarse sobre la misma
evidencia.

## Alternativas consideradas

- Usar solo la CR por regla: satura. En Jackson Databind vale 0 % en las 13
  versiones analizadas, mientras la CR por verificación desciende de 65,4 % a
  57,4 % con la deriva real; en DDD Sample vale 0 % con 8 hallazgos y 89,1 % por
  verificación. No distingue una deriva de muchas ni muestra tendencia.
- Usar solo la CR por verificación sin reportar la otra: oculta la desviación
  respecto del anteproyecto aprobado.

## Consecuencias

- La CR por verificación gradúa la extensión y permite leer tendencias, pero se
  diluye en sistemas grandes; esa debilidad se compensa con la severidad
  (ADR-0009).
- La CR por regla queda como lectura binaria por dimensión («¿qué reglas se
  incumplen?»), complementaria.
- Pruebas: `PruebasArquitecta` verifica ambas (C0 = 100 %, C1-C3 = 66,67 %,
  C4 = 0 % por regla).

## Respaldo

Evidencia propia: `prototipo/ejemplos/estudios/evolucion.csv` y
`prototipo/ejemplos/sistemas/resultados.csv`. Hevner et al. (2004) y Peffers et
al. (2007): la evaluación retroalimenta el diseño del artefacto. La
transparencia del cálculo responde al riesgo ético de sobreinterpretación de la
CR declarado en el anteproyecto.
