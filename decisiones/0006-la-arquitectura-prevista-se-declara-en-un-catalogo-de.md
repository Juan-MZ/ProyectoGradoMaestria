# ADR-0006. La arquitectura prevista se declara en un catálogo de propiedades

- **Estado:** Aceptada
- **Fecha:** 2026-09-02 (retrospectiva)

## Contexto

La deriva solo existe respecto de una arquitectura prevista, y alguien tiene
que declararla. El anteproyecto exige no depender de documentación externa.

## Decisión

La persona que diseña el sistema declara la arquitectura prevista en un archivo
`.properties` de pocas decenas de líneas: raíz, capas con prefijos de paquete
(primera coincidencia), matriz explícita y no transitiva de dependencias
permitidas, exenciones, umbral y modo de clasificación. El archivo se versiona
junto al código. Sin catálogo, el mecanismo funciona con una configuración
mínima (raíz inferida; FF2 y FF3).

## Alternativas consideradas

- Pruebas de ArchUnit escritas en Java: exigen programar y no producen una
  métrica agregada (comparación cuantitativa en ADR-0020).
- Un lenguaje de dominio propio (como el DSL de REMEDY): más expresivo, pero con
  una curva de aprendizaje que no se justifica para tres reglas.
- Diagramas UML: es la documentación externa que el anteproyecto quiere evitar.

## Consecuencias

- El catálogo es pequeño, legible y versionable; se valida antes de usarse.
- Su calidad depende de quien lo escribe; por eso existe el asistente
  (ADR-0014) y la revisión determinista.

## Respaldo

Murphy et al. (2001): el modelo de alto nivel y el mapa son las entradas que el
ingeniero aporta en los modelos de reflexión; Knodel y Popescu (2007) comparan
enfoques de verificación por reglas de dependencia.
