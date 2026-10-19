# Estudios complementarios de la evaluación

Evidencia de las secciones 4.8 a 4.11 de la monografía. Ninguno sustituye al
experimento controlado de OBJ4; cada uno responde a una objeción concreta que el
caso de estudio no puede responder. Todo se reproduce desde `prototipo/` con:

```bat
arquitecta aplicaciones      REM una vez: clona y compila las 3 aplicaciones en capas
arquitecta estudios          REM los cuatro estudios (o uno: umbral|evolucion|oraculo|archunit)
```

Los `.jar` se descargan de Maven Central la primera vez y se verifican contra el
SHA-1 que el repositorio publica (`out/sistemas/versiones/`, no versionado).

| Archivo | Estudio | Objeción que responde | ADR |
| --- | --- | --- | --- |
| `evolucion.md`, `evolucion.csv` | El catálogo de una versión de referencia aplicado a 35 versiones publicadas de Jackson Databind, Guava y Commons Lang | «Todas las derivas detectadas fueron fabricadas» | [0018](../../../decisiones/0018-estudio-de-evolucion-historica-deriva-real-no-inyectada.md) |
| `umbral.md`, `umbral-ce.csv`, `distribucion-ce.csv`, `sensibilidad-umbral.csv` | Distribución de Ce en un conjunto de calibración (4 bibliotecas) y otro de validación (3 aplicaciones y el caso de estudio) | «El umbral de concentración es arbitrario» | [0011](../../../decisiones/0011-umbral-de-concentracion-percentil-90-del-ce-de-la-linea.md) |
| `oraculo-jdepend.md`, `oraculo-jdepend.csv` | El grafo del mecanismo frente al de JDepend 2.9.5 sobre los mismos binarios | «El grafo solo lo verifica el propio autor» | [0019](../../../decisiones/0019-oraculo-externo-contrastar-el-grafo-con-jdepend.md) |
| `../archunit/equivalente-archunit.md` | Las reglas del caso de estudio escritas con ArchUnit | «¿Qué aporta frente a ArchUnit?» | [0020](../../../decisiones/0020-contrastar-el-catalogo-con-las-mismas-reglas-escritas.md) |

## Resultados en breve

- **Deriva real.** Jackson 2.19.0 introduce `introspect → deser.impl`
  (`POJOPropertiesCollector._addCreatorParams` invoca a
  `UnwrappedPropertyHandler.creatorParamName`) y una concentración nueva; Commons
  Lang 3.21.0, una concentración en `builder`. Guava: 100 % en sus 12 versiones.
  La CR por regla del anteproyecto vale 0 % en todas las versiones de Jackson; la
  CR por verificación baja de 65,38 % a 57,38 %.
- **Umbral.** Un umbral absoluto común (p90 conjunto = 10) no se traslada entre
  sistemas de distinto tamaño; el procedimiento sí: p90 del Ce de la propia línea
  base. En el caso de estudio, con el p90 de C0 (2) en lugar de 3, ningún
  veredicto cambia.
- **Oráculo.** 475 de 496 aristas coinciden; las 21 diferencias se explican por
  las reglas de extracción (genéricos y anotaciones con retención `CLASS`, que el
  mecanismo ve y JDepend no; un tipo de variable local, al revés).
- **ArchUnit.** Detección idéntica regla por regla; 33 líneas de Java frente a 14
  de catálogo; ArchUnit no entrega CR, estado ni severidad.

**Límites.** Los catálogos de las bibliotecas son hipótesis; las aplicaciones son
de referencia, no sistemas en producción; nadie de esos proyectos validó los
hallazgos.
