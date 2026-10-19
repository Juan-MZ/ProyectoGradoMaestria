# Validación en entorno controlado sobre sistemas reales

Evidencia de la sección 4.7 de la monografía. **Complementa** el experimento
controlado del caso de estudio (OBJ4), que se mantiene sobre un único sistema.

**Idea.** El caso de estudio permite saber qué derivas contiene, pero es pequeño;
un sistema real es grande, pero no se sabe qué derivas contiene. Aquí se toman
sistemas reales y se les **inyectan derivas conocidas**: así se sabe exactamente
qué debería detectarse.

| Sistema | Fuente | Catálogo |
| --- | --- | --- |
| Apache Commons Lang 3.14.0 | Maven Central | `commons-lang3/catalogo.properties` (propuesto por el asistente; sin capas) |
| Jackson Databind 2.17.2 | Maven Central | `jackson-databind/catalogo.properties` (propuesto por el asistente) |
| ArchUnit 1.3.0 | `lib/` | `../archunit/rules-archunit.properties` (hipótesis del autor, sección 4.6) |
| Guava 33.2.1 | Maven Central | `guava/catalogo.properties` (propuesto por el asistente) |
| Spring Framework PetClinic (commit `cf39292`) | GitHub, compilada | `spring-framework-petclinic/catalogo.properties` (transcribe su `readme.md`) |
| MyBatis JPetStore 6 (commit `c9a37f2`) | GitHub, compilada | `jpetstore-6/catalogo.properties` (transcribe su `src/site`) |
| DDD Sample (commit `d8b8962`) | GitHub, compilada | `dddsample-core/catalogo.properties` (capas de Evans, según sus `package.html`) |

Los catálogos propuestos por el asistente se adoptaron **sin modificarlos**, como
hipótesis; cada `propuesta.md` guarda la explicación del modelo y la revisión de
la herramienta. Los de las tres aplicaciones **no los propuso el asistente**:
transcriben la arquitectura que documenta cada proyecto, y su umbral es el
percentil 90 del Ce de la propia versión (procedimiento de `../estudios/umbral.md`).
Los `.jar` y las aplicaciones no se versionan: `arquitecta sistemas` descarga los
primeros y verifica su SHA-1; `arquitecta aplicaciones` clona y compila las
segundas en el commit fijado.

**Procedimiento.** A cada sistema se le inyecta, primero por separado y luego
junta, una deriva de cada familia: una dependencia entre capas que la matriz no
permite, una que cierra un ciclo y un paquete nuevo con más dependencias salientes
que el umbral. Cada deriva es una clase mínima, generada directamente en bytecode,
que añade **exactamente una dependencia** entre paquetes
(`src/test/java/.../sistemas/InyectorDerivas.java`). Su ubicación se elige de forma
determinista para que active solo su regla (`derivas-inyectadas.md`); en JPetStore,
estrictamente estratificado, toda arista que cierra un ciclo viola también las
capas, y así se declara. Como el sistema real ya trae hallazgos propios, se mide la
**diferencia** con la versión original.

## Resultados (`resultados.csv`)

| Sistema | Clases | CR original | Derivas detectadas | Hallazgos no inyectados | ΔCR con una deriva |
| --- | --- | --- | --- | --- | --- |
| Commons Lang | 403 | 82,35 % | 4/4 | 0 | −1,8 a −5,9 pp |
| Jackson Databind | 784 | 60,00 % | 6/6 | 0 | −0,3 a −3,3 pp |
| ArchUnit | 923 | 86,41 % | 6/6 | 0 | −0,7 a −1,9 pp |
| Guava | 2 017 | 100,00 % | 6/6 | 0 | −1,2 a −2,4 pp |
| Spring PetClinic | 49 | 100,00 % | 3/3 | 0 | −3,9 a −8,3 pp |
| MyBatis JPetStore 6 | 26 | 100,00 % | 3/3 | 0 | −5,9 a −18,8 pp |
| DDD Sample | 94 | 89,11 % | 3/3 | 0 | −0,8 a −2,0 pp |
| **Total** | | | **31/31** | **0** | |

- **La detección se sostiene en código real**, sin ruido y en menos de un segundo.
- **La CR se diluye**: en Guava, una deriva aislada la deja por encima del 97 % y la
  CR sola seguiría diciendo «Aceptable»; el criterio con severidad la marca.
- **Las aplicaciones cumplen su arquitectura documentada**, salvo DDD Sample, con 8
  hallazgos reales: un DTO de `interfaces.handling` usado por `application` (que
  además cierra un ciclo), un ciclo entre los agregados `cargo` y `handling`, y
  consumidores JMS de infraestructura que invocan servicios de aplicación.
- **En un sistema muy acoplado una deriva nueva puede no ser un hallazgo nuevo**:
  Jackson ya tiene un ciclo de 19 paquetes, y el inyectado lo agranda a 21.
- **Un catálogo deducido del código actúa como línea base**: el de Guava describe
  el estado actual (CR 100 %), pero detecta toda deriva posterior.

**Límites.** Derivas mínimas y sintéticas; catálogos de las bibliotecas
hipotéticos; aplicaciones de referencia, no sistemas en producción.

## Reproducir

```bat
arquitecta aplicaciones
arquitecta sistemas
```

Para regenerar los catálogos de las bibliotecas con el asistente (requiere un
modelo configurado; borre antes los `catalogo.properties`):

```bat
java -cp "out\arquitecta-core;out\arquitecta-test;lib\*" ^
     co.javeriana.msw.arquitecta.sistemas.ExperimentoSistemas proponer
```
