# Registro de decisiones

Cada archivo registra una decisión de diseño, de método o de evaluación que
afecta al prototipo o a la monografía: el **contexto** que la motivó, la
**decisión**, las **alternativas** que se descartaron, sus **consecuencias** y su
**respaldo** bibliográfico o empírico. El formato sigue los *Architecture
Decision Records* de Nygard, ampliados con alternativas y respaldo por tratarse
de un trabajo de investigación (ADR-0001).

Las decisiones anteriores a la creación de esta carpeta se registraron de forma
retrospectiva y se fechan con el cambio correspondiente del historial de
versiones. Una decisión no se reescribe: se sustituye por otra ADR.

| ADR | Decisión | Estado | Fecha |
| --- | --- | --- | --- |
| [0001](0001-registrar-las-decisiones-de-diseno-e-investigacion-como.md) | Registrar las decisiones de diseño e investigación como ADR | Aceptada | 2026-10-19 |
| [0002](0002-prototipo-en-java-estandar-sin-spring-boot-ni-otro.md) | Prototipo en Java estándar, sin Spring Boot ni otro framework | Aceptada | 2026-09-02 (retrospectiva) |
| [0003](0003-extraer-la-arquitectura-implementada-del-bytecode-con.md) | Extraer la arquitectura implementada del bytecode, con ArchUnit | Aceptada | 2026-09-02 (retrospectiva) |
| [0004](0004-granularidad-de-paquete-para-el-grafo-y-las-reglas.md) | Granularidad de paquete para el grafo y las reglas | Aceptada | 2026-09-02 (retrospectiva) |
| [0005](0005-catalogo-de-tres-fitness-functions-ff1-ff3.md) | Catálogo de tres Fitness Functions (FF1-FF3) seleccionadas con criterios C1-C5 | Aceptada | 2026-09-02 (retrospectiva) |
| [0006](0006-la-arquitectura-prevista-se-declara-en-un-catalogo-de.md) | La arquitectura prevista se declara en un catálogo de propiedades | Aceptada | 2026-09-02 (retrospectiva) |
| [0007](0007-exenciones-declaradas-excluir-y-rol-compartido.md) | Exenciones declaradas: `excluir` y `rol.compartido` | Aceptada | 2026-10-01 (retrospectiva) |
| [0008](0008-cr-por-verificacion-atomica-reportando-tambien-la-cr.md) | CR por verificación atómica, reportando también la CR por regla del anteproyecto | Aceptada | 2026-10-19 |
| [0009](0009-clasificacion-combinada-cr-severidad-el-veredicto-mas.md) | Clasificación combinada `cr+severidad` (el veredicto más restrictivo) | Aceptada | 2026-09-02 (retrospectiva) |
| [0010](0010-severidades-ciclos-critica-capas-y-concentracion-mayor.md) | Severidades: ciclos «crítica», capas y concentración «mayor» | Aceptada | 2026-10-19 (respaldo añadido) |
| [0011](0011-umbral-de-concentracion-percentil-90-del-ce-de-la-linea.md) | Umbral de concentración: percentil 90 del Ce de la línea base | Aceptada | 2026-10-19 |
| [0012](0012-ff3-mide-solo-la-concentracion-eferente-ce-no-el-hub.md) | FF3 mide solo la concentración eferente (Ce), no el *hub-like* completo | Aceptada | 2026-10-19 |
| [0013](0013-interfaz-swing-y-lanzador-unico.md) | Interfaz Swing y lanzador único | Aceptada | 2026-10-19 (retrospectiva) |
| [0014](0014-asistente-de-catalogo-con-modelos-de-lenguaje-como.md) | Asistente de catálogo con modelos de lenguaje, como aporte complementario | Aceptada | 2026-10-19 (retrospectiva) |
| [0015](0015-el-asistente-usa-por-defecto-un-modelo-local.md) | El asistente usa por defecto un modelo local | Aceptada | 2026-10-19 |
| [0016](0016-validacion-complementaria-por-inyeccion-controlada-de.md) | Validación complementaria por inyección controlada de derivas en sistemas reales | Aceptada | 2026-10-19 (retrospectiva) |
| [0017](0017-aplicaciones-en-capas-como-conjunto-de-validacion.md) | Aplicaciones en capas como conjunto de validación independiente | Aceptada | 2026-10-19 |
| [0018](0018-estudio-de-evolucion-historica-deriva-real-no-inyectada.md) | Estudio de evolución histórica: deriva real, no inyectada | Aceptada | 2026-10-19 |
| [0019](0019-oraculo-externo-contrastar-el-grafo-con-jdepend.md) | Oráculo externo: contrastar el grafo con JDepend | Aceptada | 2026-10-19 |
| [0020](0020-contrastar-el-catalogo-con-las-mismas-reglas-escritas.md) | Contrastar el catálogo con las mismas reglas escritas en ArchUnit | Aceptada | 2026-10-19 |
| [0021](0021-guardia-de-version-de-bytecode-java-23.md) | Guardia de versión de bytecode (Java ≤ 23) | Aceptada | 2026-10-19 (retrospectiva) |
| [0022](0022-licencia-dual-documento-cc-by-nc-nd-4-0-y-codigo-apache.md) | Licencia dual: documento CC BY-NC-ND 4.0 y código Apache 2.0 | Aceptada | 2026-10-19 (retrospectiva) |
| [0023](0023-compilar-la-monografia-con-docker-tex-live-completo.md) | Compilar la monografía con Docker (TeX Live completo) | Aceptada | 2026-10-19 (retrospectiva) |
| [0024](0024-contraste-explicito-de-la-hipotesis-y-evaluacion-de.md) | Contraste explícito de la hipótesis y evaluación de utilidad con usuarios | Aceptada | 2026-10-19 |
| [0025](0025-reemplazar-la-literatura-gris-por-fuentes-arbitradas-en.md) | Reemplazar la literatura gris por fuentes arbitradas en afirmaciones centrales | Aceptada | 2026-10-19 |
| [0026](0026-registro-honesto-del-cronograma-y-de-las-revisiones-con.md) | Registro honesto del cronograma y de las revisiones con el director | Aceptada | 2026-10-19 |
| [0027](0027-estudio-de-caso-sobre-un-sistema-real-con-juicio-experto.md) | Estudio de caso sobre un sistema real, contrastado con el juicio de un experto | Propuesta | 2026-10-23 |

## Desviaciones respecto del anteproyecto

El anteproyecto aprobado no se modifica. Las decisiones que se apartan de su
letra, y por qué, son:

| Anteproyecto | Trabajo realizado | ADR |
| --- | --- | --- |
| CR = reglas cumplidas / reglas totales | CR por verificación para clasificar; la del anteproyecto se reporta siempre como «CR por regla» | 0008 |
| Clasificación por umbrales de la CR | Se añade la severidad (modo configurable; el original se conserva) | 0009 |
| Análisis estático «del código fuente» | Análisis del código compilado, derivado fiel del fuente | 0003 |
| Un único sistema en el estudio de caso | Se mantiene para OBJ4; se añaden validaciones complementarias en 7 sistemas reales y 35 versiones publicadas | 0016-0018 |
| Revisiones mensuales con el director | No se realizaron; se declara | 0026 |
| (no previsto) Asistente de catálogo con LLM | Aporte complementario, fuera de OBJ1-OBJ4 | 0014, 0015 |
