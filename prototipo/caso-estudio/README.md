# Caso de estudio

Sistema sintético de ventas (`com.caso`) organizado en tres capas —
`presentacion → negocio → persistencia`— más un dominio compartido
(`com.caso.dominio`) que no pertenece a ninguna. Es el sistema sobre el que se
ejecutó el experimento controlado del trabajo de grado.

| Carpeta | Escenario | Deriva introducida | Reglas que debe activar |
| --- | --- | --- | --- |
| `c0-base/` | C0 · Línea base | Ninguna: respeta la arquitectura declarada. | Ninguna |
| `c1-capas/` | C1 · Ruptura de capas | La presentación accede directamente a la persistencia. | FF1 |
| `c2-ciclo/` | C2 · Ciclo | Un subpaquete del negocio y su paquete padre se referencian mutuamente. | FF2 |
| `c3-concentracion/` | C3 · Concentración | Un paquete `util` depende de los cuatro restantes. | FF3 |
| `c4-combinado/` | C4 · Combinado | Las tres derivas a la vez. | FF1, FF2 y FF3 |

`rules.properties` es el catálogo de reglas común a los cinco escenarios.

Cada escenario es un proyecto con la estructura de Maven: el código en
`src/main/java` y, tras `arquitecta compilar`, las clases en `target/classes`
(que no se versiona). Por eso cualquiera de estas carpetas puede elegirse tal
cual en la aplicación —o pulsarse en su pantalla de inicio—: se localizan las
clases compiladas y se carga este catálogo, que está en la carpeta superior.

```bat
arquitecta analizar caso-estudio\c4-combinado caso-estudio\rules.properties
arquitecta experimento      REM los cinco escenarios, con la evidencia en resultados\
```
