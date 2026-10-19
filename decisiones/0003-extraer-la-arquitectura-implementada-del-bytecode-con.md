# ADR-0003. Extraer la arquitectura implementada del bytecode, con ArchUnit

- **Estado:** Aceptada
- **Fecha:** 2026-09-02 (retrospectiva)
- **Relacionadas:** ADR-0019, ADR-0021

## Contexto

El anteproyecto habla de análisis estático «del código fuente». Para construir
el grafo de dependencias hay dos caminos: analizar el código fuente (requiere un
analizador sintáctico y resolver tipos, con todo el *classpath*) o analizar el
código compilado.

## Decisión

El grafo se extrae del *bytecode* (`.class` o `.jar`) mediante el importador de
ArchUnit 1.3.0. El código compilado es una derivación determinista y fiel del
código fuente: los nombres de tipos ya están resueltos, lo que elimina la
ambigüedad de importaciones y comodines. Las reglas de extracción R1-R7 y las
14 formas que producen dependencia (y las 5 que no) se documentan en la sección
3.4 de la monografía. La monografía declara explícitamente esta decisión como
una precisión del término «código fuente» del anteproyecto, no como un cambio de
alcance: el mecanismo sigue sin depender de documentación externa.

## Alternativas consideradas

- Analizar fuentes con JavaParser o el árbol del compilador: necesita compilar
  igualmente para resolver tipos y no aporta información de dependencias que el
  bytecode no tenga.
- Escribir un lector de bytecode propio: reinventa lo que ArchUnit resuelve y
  añade una fuente de errores sin valor para la investigación.

## Consecuencias

- Puntos ciegos conocidos y documentados: constantes de compilación (el
  compilador las incrusta), anotaciones con retención `SOURCE`, importaciones o
  variables sin uso, tipos de variables locales y conversiones de tipo que
  ArchUnit 1.3.0 no registra.
- El oráculo independiente (ADR-0019) confirma que esos puntos ciegos explican
  todas las diferencias con JDepend.
- Requiere que el proyecto esté compilado; la interfaz lo detecta y lo explica.

## Respaldo

Murphy et al. (2001) y Knodel y Popescu (2007) definen la extracción del modelo
implementado como el paso que alimenta la verificación de conformidad, sin
prescribir el nivel de artefacto. ArchUnit (TNG Technology Consulting, 2024)
opera sobre bytecode, igual que Arcan (Arcelli Fontana et al., 2016) y JDepend.
