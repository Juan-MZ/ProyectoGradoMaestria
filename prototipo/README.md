# Arquitecta

Prototipo funcional del mecanismo de detección de deriva arquitectónica descrito
en el proyecto de grado _«Mecanismo de detección automática de deriva
arquitectónica mediante el uso de Fitness Functions y análisis estático de grafos
de dependencia»_ (Maestría en Ingeniería de Software, Pontificia Universidad
Javeriana Cali).

Implementa un flujo de tres etapas: **extracción** de dependencias desde el
código compilado, **construcción** de un grafo estructural y **validación**
mediante _Fitness Functions_, con cálculo de una Tasa de Conformidad (CR) y
clasificación del estado arquitectónico.

---

## Componentes

| Paquete               | Responsabilidad                                                                                                                                                                                                                          |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `…arquitecta.config`  | Catálogo de reglas como configuración: capas, matriz de adyacencias, umbrales y modo de clasificación.                                                                                                                                   |
| `…arquitecta.grafo`   | Grafo de dependencias con evidencia por arista (`GrafoDependencias`), extracción desde bytecode con ArchUnit (`ConstructorGrafo`), componentes fuertemente conexos (`Tarjan`) y reconstrucción de la ruta cíclica (`BuscadorCiclos`).    |
| `…arquitecta.reglas`  | Las tres _Fitness Functions_: integridad de capas, ciclos de dependencia y concentración de dependencias salientes. Cada regla se autodescribe (propiedad, racionalidad, unidad de verificación, criterio de incumplimiento, severidad). |
| `…arquitecta.motor`   | Agrega las verificaciones, calcula la CR y clasifica el estado por CR, por severidad y en su combinación.                                                                                                                                |
| `…arquitecta.reporte` | Reporte textual con declaración de alcance, catálogo ejecutable y exportadores a JSON, CSV y DOT.                                                                                                                                        |

La arquitectura interna es estrictamente acíclica y estratificada; el propio
mecanismo la verifica (véase _Autoanálisis_, más abajo).

## Requisitos

- **JDK 17 o superior** (verificado con JDK 17 y JDK 26).
- Maven es **opcional**: los scripts usan las dependencias ya empaquetadas en
  `lib/`, por lo que no se requiere conexión a internet ni repositorio local.

## Inicio rápido

```bat
build.bat        REM compila nucleo, pruebas y los cinco escenarios
test.bat         REM ejecuta la bateria de pruebas (96 comprobaciones)
experimento.bat  REM ejecuta el experimento completo y escribe resultados\
```

En macOS o Linux, los equivalentes son `./build.sh`, `./test.sh` y
`./experimento.sh`.

## Uso manual

```bat
java -cp "out\arquitecta-core;lib\*" co.javeriana.msw.arquitecta.ArquitectaApp ^
     <directorio-de-clases> <archivo-config> [opciones]
```

| Opción                    | Efecto                                                                                                                            |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `--etiqueta <nombre>`     | Nombra el escenario en el reporte y en los archivos de evidencia.                                                                 |
| `--salida <directorio>`   | Escribe reporte (`.txt`), evidencia estructurada (`.json`), grafo (`.dot`), hallazgos (`.csv`) y agrega una fila a `resumen.csv`. |
| `--modo cr\|cr+severidad` | Sobrescribe el modo de clasificación declarado en la configuración.                                                               |
| `--fallar-si <estado>`    | Devuelve código de salida 2 si el veredicto alcanza esa gravedad (afordancia para uso futuro en integración continua).            |
| `--catalogo`              | Imprime el catálogo de _Fitness Functions_ derivado de la implementación vigente y termina.                                       |

## Configuración

Todo el conocimiento sobre qué es admisible vive en un archivo `.properties`; el
motor no contiene ninguna regla codificada. Véase `rules.properties` para el caso
de estudio y `rules-arquitecta.properties` para el autoanálisis.

```properties
raiz=com.caso
capas=presentacion,negocio,persistencia
capa.presentacion=com.caso.presentacion
capa.negocio=com.caso.negocio
capa.persistencia=com.caso.persistencia
permitido.presentacion=negocio
permitido.negocio=persistencia
permitido.persistencia=
umbral.concentracion=3
umbral.cr.aceptable=85
umbral.cr.observacion=70
clasificacion.modo=cr+severidad
```

Los paquetes no asignados a ninguna capa (por ejemplo, un dominio compartido)
quedan fuera de la regla de integridad de capas: al no pertenecer a la
estratificación, no puede afirmarse que la vulneren.

> **Orden de declaración.** La asignación de paquetes a capas se resuelve por
> prefijo, en el orden en que las capas aparecen en `capas`. Si una capa es
> prefijo de otra, debe declararse después de la más específica.

## Interpretación del resultado

La CR agrega las verificaciones atómicas de todas las reglas —una por cada arista
entre capas distintas, y una por cada paquete en el caso de ciclos y de
concentración— y se calcula como

```
CR = 100 × (verificaciones − incumplimientos) / verificaciones
```

El mecanismo deriva de ella dos lecturas complementarias y adopta, en el modo
`cr+severidad`, la más restrictiva:

| Criterio            | Qué mide                         | Clasificación                                                                   |
| ------------------- | -------------------------------- | ------------------------------------------------------------------------------- |
| Tasa de Conformidad | **Extensión** del incumplimiento | `CR ≥ 85%` aceptable · `70% ≤ CR < 85%` en observación · `CR < 70%` no conforme |
| Severidad máxima    | **Gravedad** del peor hallazgo   | crítica → no conforme · mayor → en observación · sin hallazgos → aceptable      |

El modo `cr` reproduce la formulación original, basada únicamente en la CR, y se
conserva para poder comparar ambos criterios.

## Escenarios del experimento

`caso-estudio/` contiene la implementación de referencia: un pequeño sistema por
capas (`presentacion`, `negocio`, `persistencia`) con un dominio compartido
(`com.caso.dominio`). Los cuatro escenarios restantes introducen derivas
deliberadas, los tres primeros de forma **aislada** para poder medir la
sensibilidad de cada regla por separado:

| Escenario                       | Deriva inducida                                                                                                                            |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `caso-estudio-c1-capas`         | Salto de capa `presentacion → persistencia`: el controlador accede al repositorio sin pasar por el servicio. No cierra ciclos.             |
| `caso-estudio-c2-ciclo`         | Ciclo `com.caso.negocio ↔ com.caso.negocio.calculo`, contenido dentro de una misma capa, de modo que la estratificación permanece intacta. |
| `caso-estudio-c3-concentracion` | Paquete `com.caso.util` con `Ce = 4`, sin capa asignada y sin ser destino de nadie: no rompe capas ni cierra ciclos.                       |
| `caso-estudio-c4-combinado`     | Las tres derivas simultáneas: ruptura de capas, ciclo entre tres paquetes y componente dios.                                               |

## Resultados verificados

Salida de `experimento.bat` (idéntica a la comprobada por la batería de pruebas):

| Escenario          | Paquetes | Dependencias | Verificaciones | Incumpl. |       CR | Estado (modo `cr`) | Estado (modo `cr+severidad`) |
| ------------------ | -------: | -----------: | -------------: | -------: | -------: | ------------------ | ---------------------------- |
| C0 · base          |        4 |            5 |             10 |        0 | 100,00 % | Aceptable          | Aceptable                    |
| C1 · capas         |        4 |            6 |             11 |        1 |  90,91 % | Aceptable          | En observación               |
| C2 · ciclo         |        5 |            8 |             12 |        2 |  83,33 % | En observación     | No conforme                  |
| C3 · concentración |        5 |            9 |             12 |        1 |  91,67 % | Aceptable          | En observación               |
| C4 · combinado     |        5 |           11 |             14 |        6 |  57,14 % | No conforme        | No conforme                  |

Las tres derivas se detectan en todos los escenarios. Nótese que en C1 y en C3 la
CR por sí sola clasifica como _aceptable_ un sistema con una violación real: al
ser una proporción no ponderada, diluye los incumplimientos localizados. Incorporar
la severidad corrige esa lectura sin alterar el cálculo de la CR.

## Autoanálisis

El mecanismo se aplica sobre su propio código compilado:

```bat
java -cp "out\arquitecta-core;lib\*" co.javeriana.msw.arquitecta.ArquitectaApp ^
     out\arquitecta-core rules-arquitecta.properties --etiqueta Arquitecta-autoanalisis
```

Resultado: 6 paquetes, 14 dependencias, 26 verificaciones, 0 incumplimientos →
**CR 100 % · Aceptable**. La arquitectura interna del prototipo satisface las
mismas reglas que exige a los sistemas que evalúa.

Con el umbral de concentración calibrado en 3 en lugar de 5, el autoanálisis
señala la raíz de composición (`Ce = 5`) y el módulo de reporte (`Ce = 4`). Ambos
son concentradores legítimos por su papel arquitectónico, no derivas: el caso
ilustra que el umbral debe calibrarse según el sistema y que la regla, al operar
solo sobre la topología, es ciega al rol que cada paquete desempeña.

## Evidencia generada

`experimento.bat` escribe en `resultados/`:

```
resultados/
├─ catalogo-reglas.txt          catálogo derivado de la implementación
├─ modo-cr/                     un subdirectorio por modo de clasificación
│  ├─ resumen.csv               una fila por escenario
│  ├─ c1-capas.txt              reporte legible
│  ├─ c1-capas.json             evidencia estructurada y completa
│  ├─ c1-capas.dot              grafo con violaciones resaltadas
│  └─ c1-capas-hallazgos.csv    una fila por hallazgo
└─ modo-cr-severidad/           idem, con la clasificación refinada
```

Los archivos `.dot` pueden renderizarse con Graphviz:
`dot -Tpng resultados/modo-cr/c4-combinado.dot -o c4.png`.

## Pruebas

`test.bat` ejecuta 96 comprobaciones sobre el grafo, el algoritmo de Tarjan, la
carga de configuración, las tres reglas, el motor, los cinco escenarios y los
exportadores, incluidos los casos límite (grafo vacío, umbrales en su frontera
exacta, configuraciones inválidas). La batería se construyó **sin dependencias
externas**, en coherencia con el requisito de ligereza del propio mecanismo, y
devuelve código de salida distinto de cero si alguna comprobación falla.

## Compilación con Maven

```bat
mvn -q clean package
```

Genera `target/arquitecta.jar` con las dependencias empaquetadas
(`maven-shade-plugin`). La batería de pruebas se compila en la fase
`test-compile`, pero se ejecuta mediante `test.bat` / `test.sh`, no por
_surefire_, porque deliberadamente no depende de un marco de pruebas externo.

## Notas

- Al ejecutar con **JDK 26** aparecen avisos benignos en `stderr` (SLF4J sin
  proveedor de _logging_ y una advertencia sobre `sun.misc.Unsafe` emitida por
  Guava, embebida en ArchUnit). No afectan el resultado; con JDK 17 no se
  presentan.
- Los scripts fijan la página de códigos UTF-8 (`chcp 65001`) y la salida se
  emite explícitamente en UTF-8, de modo que los reportes conserven los acentos
  en la consola de Windows.
