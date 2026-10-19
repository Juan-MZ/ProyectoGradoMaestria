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
| `…arquitecta.servicio` | Fachada del análisis que comparten la línea de comandos y la interfaz gráfica: reconoce la carpeta de un proyecto Maven, Gradle, IntelliJ o Eclipse (o un `.jar`), carga el catálogo o infiere una configuración mínima y ejecuta el pipeline completo. |
| `…arquitecta.asistente` | Asistente opcional que, con un modelo de lenguaje (Claude, OpenAI, DeepSeek o uno local con Ollama o LM Studio), propone el catálogo de reglas de un proyecto que no lo tiene. Se acopla a la interfaz sin que esta dependa de él. |
| `…arquitecta.interfaz` | Interfaz gráfica de escritorio (Swing, incluida en el JDK): selección del proyecto, indicadores, grafo interactivo, hallazgos con su evidencia y exportación. Solo presenta: pide el análisis al servicio y dibuja su resultado. |

La arquitectura interna es estrictamente acíclica y estratificada; el propio
mecanismo la verifica (véase _Autoanálisis_, más abajo).

## Estructura

```
prototipo/
├── arquitecta.bat · arquitecta.sh   Punto de entrada único (doble clic: abre la aplicación)
├── src/main/java/                   Código del mecanismo
├── src/test/java/                   Batería de pruebas
├── lib/                             ArchUnit y SLF4J (sin Maven ni red)
├── caso-estudio/                    Los cinco escenarios del experimento y su catálogo
│   ├── c0-base/ … c4-combinado/      Un proyecto por escenario (src/main/java)
│   └── rules.properties             Catálogo de reglas común
├── ejemplos/
│   ├── autoanalisis/                Catálogo con que el prototipo se analiza a sí mismo
│   ├── archunit/                    Catálogos para analizar ArchUnit 1.3.0
│   ├── graficos/                    Grafos SVG generados por la herramienta
│   └── interfaz/                    Capturas de la aplicación
├── scripts/                         Guiones que invoca el lanzador
├── pom.xml                          Compilación alternativa con Maven
└── .vscode/                         Ejecución con F5 y tareas en VS Code
```

Lo generado no se versiona: `out/` (clases y `arquitecta.jar`), el
`target/classes` de cada escenario y `resultados/` (evidencia del experimento).

## Requisitos

- **JDK 17 o superior** (verificado con JDK 17 y JDK 26).
- Nada más: las dependencias ya están en `lib/`, de modo que no hace falta
  Maven, repositorio de artefactos ni conexión a internet.

## Inicio rápido

**Doble clic en `arquitecta.bat`** (o `./arquitecta.sh` en macOS y Linux): la
primera vez compila el prototipo y el caso de estudio, y abre la aplicación. En
su pantalla de inicio, los botones **C0 … C4** analizan cada escenario del caso de
estudio con un clic. Desde entonces solo recompila cuando el código cambia.

El mismo lanzador reúne todo lo demás:

```bat
arquitecta                         REM abre la aplicacion
arquitecta C:\ruta\al\proyecto      REM la abre con ese proyecto (carpeta o .jar)
arquitecta analizar <proyecto> [reglas] [opciones]   REM analisis por consola
arquitecta compilar                REM compila nucleo, pruebas, arquitecta.jar y escenarios
arquitecta pruebas                 REM bateria de pruebas (272 comprobaciones)
arquitecta experimento             REM experimento completo; evidencia en resultados\
arquitecta figuras                 REM regenera los grafos y capturas de la monografia
arquitecta aplicaciones            REM clona y compila las 3 aplicaciones en capas de la validacion
arquitecta sistemas                REM inyecta derivas en 7 sistemas reales y mide su deteccion
arquitecta estudios [modo]         REM evolucion, umbral, oraculo JDepend, equivalente ArchUnit
arquitecta ayuda
```

En macOS o Linux se usa igual, como `./arquitecta.sh <subcomando>`; desde
PowerShell, como `.\arquitecta <subcomando>`.

La compilación produce además **`out/arquitecta.jar`**, un único archivo con las
dependencias dentro: basta con copiarlo a otro equipo con Java 17 para usar la
herramienta (`java -jar arquitecta.jar`, o doble clic, abre la aplicación).

**En VS Code**, abra la carpeta `prototipo/` (no la del repositorio): la
extensión de Java importa el `pom.xml` y quedan disponibles las configuraciones
de ejecución _Arquitecta: aplicación_, _analizar C4 por consola_ y _pruebas_
(F5), y las tareas _compilar_, _pruebas_, _experimento_ y _figuras_
(_Terminal → Ejecutar tarea…_). Abrir una carpeta superior hace que la extensión
trate los archivos como sueltos y no encuentre la clase principal.

Para ver el grafo de cualquier sistema basta con una línea; el resultado se abre
en el navegador, sin instalar nada:

```bat
arquitecta analizar mi-sistema.jar mis-reglas.properties --svg grafo.svg
```

## Interfaz gráfica

`arquitecta.bat` (o `./arquitecta.sh`) abre la aplicación de escritorio. No
requiere instalar nada más que el JDK: está construida con Swing, que el propio
JDK incluye.

![Interfaz gráfica de Arquitecta sobre el escenario combinado C4](ejemplos/interfaz/interfaz-grafo.png)

El flujo sigue tres pasos, en la barra lateral:

1. **Proyecto.** _Examinar…_ abre un selector de carpetas; también se puede
   arrastrar la carpeta (o un `.jar`) a la ventana. No hace falta localizar a
   mano el directorio de clases: basta con la carpeta raíz del proyecto, y la
   aplicación reconoce `target/classes` (Maven), `build/classes/java/main`
   (Gradle), `out/production` (IntelliJ IDEA), `bin` (Eclipse) o el `.jar`
   generado. Si la carpeta solo contiene fuentes `.java`, lo indica y pide
   compilar primero, en lugar de devolver un análisis vacío.
2. **Catálogo de reglas.** Si el proyecto trae su catálogo (`rules.properties`,
   `arquitecta.properties`, `reglas.properties` o `.arquitecta.properties`, en su
   carpeta o en la que la contiene), se carga solo. Si no, se aplica una **configuración mínima**: se infiere la raíz
   de paquetes y se evalúan ciclos y concentración; la estratificación no puede
   verificarse sin capas declaradas, y la interfaz lo advierte.
3. **Clasificación.** _Extensión y gravedad_ (recomendado, modo
   `cr+severidad`) o _Solo Tasa de Conformidad_ (modo `cr`).

El resultado muestra la CR (con su variación respecto del análisis anterior del
mismo proyecto, que se conserva en `~/.arquitecta/historial.csv`), el estado, el
tamaño del sistema, los hallazgos y la conformidad por regla, y tres pestañas:

- **Grafo**: la misma disposición por capas que el SVG, con zoom (rueda), arrastre
  y selección. Al hacer clic en un paquete se atenúa todo lo que no le concierne
  y una ficha muestra su capa, `Ce`/`Ca`, de quién depende, quién depende de él
  y los hallazgos en que interviene. _Ampliar grafo_ pliega los indicadores para
  dedicar al grafo toda la altura.
- **Hallazgos**: tabla por severidad; al seleccionar uno se ve su evidencia (las
  llamadas concretas en el código) y se resalta en el grafo.
- **Reporte**: el reporte textual, idéntico al de la línea de comandos.

_Exportar…_ escribe la evidencia completa (reporte, JSON, CSV y el grafo en SVG,
TikZ y DOT) en la carpeta elegida, y _Guardar SVG…_ solo la imagen del grafo.
Atajos: `Ctrl+O` abre un proyecto y `F5` repite el análisis.

La interfaz también se puede abrir con un proyecto ya cargado
(`arquitecta C:\ruta\al\proyecto`) y, para documentación, dibujarse sin
ventana: `--gui --proyecto <ruta> [--reglas <archivo>] --captura salida.png`
(`arquitecta figuras` genera así las capturas de `ejemplos/interfaz/`).

## Asistente de catálogo (IA)

Un proyecto que no tiene catálogo solo puede evaluarse con la configuración
mínima, que no verifica las capas. El asistente ayuda a redactarlo: en la barra
lateral, **Definir con el asistente…** abre una ventana que lee la estructura del
proyecto, se la presenta a un modelo de lenguaje y conversa con usted hasta
acordar el catálogo.

![Asistente de catálogo proponiendo el catálogo del escenario C4](ejemplos/interfaz/interfaz-asistente.png)

- **Lo que propone** son los parámetros de las tres _Fitness Functions_ —capas y
  sus paquetes, dependencias permitidas, umbral de concentración y exenciones—,
  con la justificación de cada decisión, las dependencias actuales que considera
  **deriva probable** y las **preguntas** que no puede responder sin usted. No
  crea ni modifica reglas: esas son las de la herramienta.
- **No legitima la deriva.** Está instruido para declarar la arquitectura
  _prevista_: una dependencia que existe pero contradice la estratificación no se
  permite, sino que se señala.
- **Usted decide.** La propuesta aparece en un editor donde puede corregirla a
  mano; se valida con el mismo cargador que un catálogo escrito a mano, y
  **Previsualizar** muestra el diagnóstico que produciría sobre su código.
  **Guardar y usar…** la escribe (por defecto como `arquitecta.properties` en la
  carpeta del proyecto, donde la aplicación la encontrará sola) y analiza.
- **Se le puede responder.** Cada mensaje viaja con el catálogo tal como está en
  el editor y con el resultado de la última previsualización, para que el
  asistente parta de sus cambios e interprete los hallazgos.

**Modelos.** En la misma ventana se elige el proveedor y se configura su API:

| Proveedor | Dirección por defecto | Clave |
| --- | --- | --- |
| Anthropic · Claude | `https://api.anthropic.com` (modelo `claude-opus-5-5`) | `ANTHROPIC_API_KEY` o la indicada |
| OpenAI | `https://api.openai.com/v1` | `OPENAI_API_KEY` o la indicada |
| DeepSeek | `https://api.deepseek.com` (modelo `deepseek-v4-pro`; también `deepseek-flash`) | `DEEPSEEK_API_KEY` o la indicada |
| Ollama (local, **por defecto**) | `http://localhost:11434/v1` | no requiere |
| LM Studio (local) | `http://localhost:1234/v1` | no requiere |
| Otro compatible con OpenAI | la que se indique | opcional |

Mientras no se elija expresamente un servicio externo, el asistente usa Ollama:
por defecto ningún dato sale del equipo
([ADR-0015](../decisiones/0015-el-asistente-usa-por-defecto-un-modelo-local.md)).
Con una GPU de 12 GB, un modelo de 14B como `qwen2.5:14b` no cabe con el contexto
por defecto de Ollama (32 768 tokens) y la carga falla o cae a CPU; basta un
contexto de 8 192, sobrado para el asistente (~2 100 tokens por consulta):

```bat
ollama pull qwen2.5:14b
curl http://localhost:11434/api/create -d "{\"model\":\"qwen2.5-14b-gpu\",\"from\":\"qwen2.5:14b\",\"parameters\":{\"num_ctx\":8192}}"
```

y elegir `qwen2.5-14b-gpu` como modelo (o definir `OLLAMA_CONTEXT_LENGTH=8192`).

_Probar conexión_ comprueba la dirección y la clave y lista los modelos
disponibles. La configuración se conserva en `~/.arquitecta/asistente.properties`
(en su carpeta de usuario, nunca en la del proyecto); la clave, solo si marca
_Recordar la clave en este equipo_.

**Qué sale del equipo.** Solo el _perfil estructural_: nombres de paquetes,
número de clases, hasta ocho nombres de clase por paquete y las dependencias entre
paquetes. Nunca el código fuente. _Ver el perfil que se envía_ muestra el texto
exacto antes de enviarlo. Con Ollama o LM Studio no sale nada del equipo.

**Evaluación con un modelo real.** Con DeepSeek, en 60 propuestas sobre el caso de
estudio y sin darle contexto, ningún catálogo legitimó una deriva de capas ni un
ciclo, pero el umbral de concentración resultó inestable y una vez ocultó la deriva
inducida. Por eso la herramienta revisa cada propuesta con una regla fija y avisa
cuando el umbral deja sin señalar al paquete que más concentra. Datos, catálogos y
cómo repetirlo: [ejemplos/asistente/](ejemplos/asistente/README.md).

El asistente no añade dependencias: usa el cliente HTTP del propio JDK. Sus
pruebas no consultan ningún modelo real, sino un servidor local que imita las API.

## Uso manual

```bat
arquitecta analizar <proyecto | directorio-de-clases | archivo.jar> [archivo-config] [opciones]
```

Equivale a `java -jar out\arquitecta.jar <...>`, que es lo que hay que usar con
el `.jar` copiado a otro equipo.

El sistema puede indicarse como un directorio de clases compiladas, como la
carpeta raíz de un proyecto Maven, Gradle, IntelliJ o Eclipse, o directamente como
el `.jar` con que se distribuye. Si se omite el archivo de configuración, se
aplica la configuración mínima descrita en _Interfaz gráfica_. Si no se encuentra ninguna
clase bajo la raíz declarada, el mecanismo termina con un error: un grafo vacío
obtendría una CR del 100 % y se declararía aceptable, de modo que apuntar por
descuido al directorio de fuentes produciría un falso negativo silencioso.

| Opción                    | Efecto                                                                                                                            |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `--etiqueta <nombre>`     | Nombra el escenario en el reporte y en los archivos de evidencia.                                                                 |
| `--salida <directorio>`   | Escribe reporte (`.txt`), evidencia estructurada (`.json`), hallazgos (`.csv`), el grafo dibujado (`.svg`, `.tikz`, `.dot`) y agrega una fila a `resumen.csv`. |
| `--svg <archivo>`         | Dibuja el grafo como imagen SVG, que se abre en cualquier navegador.                                                              |
| `--tikz <archivo>`        | Dibuja el grafo como figura TikZ, lista para `\input` en un documento LaTeX.                                                       |
| `--historico <csv>`       | Compara con la última corrida registrada para la misma etiqueta y reporta la variación. Si se omite y hay `--salida`, usa el `resumen.csv` de ese directorio. |
| `--modo cr\|cr+severidad` | Sobrescribe el modo de clasificación declarado en la configuración.                                                               |
| `--fallar-si <estado>`    | Devuelve código de salida 2 si el veredicto alcanza esa gravedad (afordancia para uso futuro en integración continua).            |
| `--catalogo`              | Imprime el catálogo de _Fitness Functions_ derivado de la implementación vigente y termina.                                       |

## Visualización del grafo

![Grafo del escenario combinado C4](ejemplos/graficos/c4-combinado.svg)

El mecanismo dibuja el grafo que extrae, sin depender de Graphviz ni de ninguna
otra herramienta. Lo hace con un algoritmo propio de disposición por capas, por
una razón concreta: los algoritmos genéricos colocan los nodos según el sentido
de las aristas, y justo las violaciones —que remontan la estratificación— pueden
invertir el orden de las capas. Aquí el orden vertical lo dicta la arquitectura
declarada.

- **Bandas por capa**, ordenadas según la matriz de dependencias permitidas, no
  según el orden en que se declararon.
- **Filas** dentro de cada banda por camino más largo, de modo que también las
  dependencias internas apunten hacia abajo; los paquetes de un ciclo comparten fila.
- **Reducción de cruces** por baricentro.
- **Columna lateral** para los paquetes sin capa.

El trazado codifica el significado: las dependencias que remontan las capas
rodean los nodos por fuera; las de una misma fila se dibujan como arcos, por
arriba en un sentido y por abajo en el otro; las flechas que llegan a un mismo
lado se reparten a lo largo de él, y los paquetes muy referenciados se ensanchan.

| Elemento | Significado |
| --- | --- |
| Arista roja continua | Viola la integridad de capas |
| Arista roja discontinua | Forma parte de un ciclo |
| Recuadro rojo | Paquete en un ciclo |
| Recuadro ámbar | Concentración excesiva de dependencias salientes |
| Recuadro gris discontinuo | Núcleo compartido exento |

Cada paquete del SVG tiene un texto emergente con su nombre completo, su capa,
su `Ce`, su `Ca` y su estado. La misma disposición se emite como **TikZ** —las
figuras de grafos de la monografía las genera la herramienta, no se dibujaron a
mano— y como **DOT**, que fija las filas para que tampoco Graphviz pueda invertir
las capas.

`arquitecta figuras` regenera todas las figuras: las TikZ en
`../Maestria_PlantillaProyectoGrado/figuras/` y las SVG en `ejemplos/graficos/`.

## Configuración

Todo el conocimiento sobre qué es admisible vive en un archivo `.properties`; el
motor no contiene ninguna regla codificada. Véase `caso-estudio/rules.properties` para
el caso de estudio y `ejemplos/autoanalisis/rules-arquitecta.properties` para el
autoanálisis.

```properties
raiz=com.caso
capas=presentacion,negocio,persistencia
capa.presentacion=com.caso.presentacion
capa.negocio=com.caso.negocio
capa.persistencia=com.caso.persistencia
permitido.presentacion=negocio
permitido.negocio=persistencia
permitido.persistencia=
excluir=
rol.compartido=
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

### Exenciones

Un catálogo que no admita excepciones obliga a elegir entre tolerar falsos
positivos o apagar la regla entera. Se distinguen dos situaciones, y el reporte
enumera ambas para que la excepción quede registrada y sea auditable.

| Clave            | Para qué                                                                                                         | Alcance                                                  |
| ---------------- | ---------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| `excluir`        | Código que **no forma parte** del sistema evaluado aunque el empaquetado lo sitúe bajo la misma raíz: dependencias vendorizadas, código generado. Lo saca del grafo. | El paquete **y todos sus subpaquetes**.                   |
| `rol.compartido` | Paquetes que **sí forman parte** del sistema pero cuyo papel —núcleo de anotaciones, raíz de composición, fachada— hace que concentren dependencias por diseño. Quedan exentos de las reglas de ciclos y de concentración, pero siguen en el grafo y en la evidencia. | **Exacto**, no se hereda a los subpaquetes.               |

La asimetría es deliberada. Excluir código ajeno es siempre una cuestión de
subárbol; un papel arquitectónico, en cambio, lo desempeña un paquete concreto, y
propagarlo hacia abajo eximiría al sistema entero cada vez que el núcleo
compartido resultara ser la raíz, que es el caso más frecuente.

### Tendencia

El valor absoluto de la CR padece un efecto de dilución: al ser una proporción,
una violación aislada pesa cada vez menos conforme crece el sistema. Su
**variación** no sufre ese problema, porque se mide contra el propio sistema y no
contra un umbral universal. Conservando el `resumen.csv` entre corridas, el
reporte añade la comparación automáticamente:

```
  Tasa de Conformidad     : 86.41%
  Tendencia               : +4.10 pp respecto de la corrida anterior (82.31%) · mejora
```

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

`caso-estudio/c0-base/` contiene la implementación de referencia: un pequeño
sistema por capas (`presentacion`, `negocio`, `persistencia`) con un dominio
compartido (`com.caso.dominio`). Los cuatro escenarios restantes introducen
derivas deliberadas, los tres primeros de forma **aislada** para poder medir la
sensibilidad de cada regla por separado:

| Escenario (`caso-estudio/…`) | Deriva inducida                                                                                                                            |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `c1-capas`                   | Salto de capa `presentacion → persistencia`: el controlador accede al repositorio sin pasar por el servicio. No cierra ciclos.             |
| `c2-ciclo`                   | Ciclo `com.caso.negocio ↔ com.caso.negocio.calculo`, contenido dentro de una misma capa, de modo que la estratificación permanece intacta. |
| `c3-concentracion`           | Paquete `com.caso.util` con `Ce = 4`, sin capa asignada y sin ser destino de nadie: no rompe capas ni cierra ciclos.                       |
| `c4-combinado`               | Las tres derivas simultáneas: ruptura de capas, ciclo entre tres paquetes y componente dios.                                               |

Cada escenario tiene la estructura de un proyecto Maven y se compila en su propio
`target/classes`, de modo que su carpeta se elige tal cual en la aplicación.

## Resultados verificados

Salida de `arquitecta experimento` (idéntica a la comprobada por la batería de pruebas):

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
arquitecta analizar out\arquitecta-core ejemplos\autoanalisis\rules-arquitecta.properties ^
     --etiqueta Arquitecta-autoanalisis
```

Resultado: 9 paquetes, 26 dependencias, 40 verificaciones, 0 incumplimientos →
**CR 100 % · Aceptable**. La arquitectura interna del prototipo satisface las
mismas reglas que exige a los sistemas que evalúa.

La configuración declara las capas `servicio` e `interfaz` entre el reporte y la
raíz, y prohíbe a la interfaz el acceso directo a la configuración y a la
extracción: si alguna vez los usara en lugar de pedírselos al servicio, la regla
lo señalaría como deriva. Declara además como núcleo compartido
(`rol.compartido`) la raíz de composición y el servicio, porque dependen de los
módulos que ensamblan por definición del patrón (raíz de composición y fachada),
no por deterioro. Declararlo permite mantener el umbral
estricto en lugar de relajarlo para todo el sistema, que es lo que ocultaría
concentraciones reales en los demás paquetes. Bajándolo a 3 aparecen el módulo de
reporte, la interfaz y el asistente (`Ce = 4` cada uno), que dependen de los cuatro
módulos con que trabajan: el caso
ilustra que el umbral no es universal y que cada sistema obliga a decidir dónde
está su techo razonable.

## Validación en entorno controlado sobre sistemas reales

`arquitecta sistemas` inyecta derivas conocidas —una de cada familia y después las
tres juntas— en cuatro bibliotecas reales (Commons Lang, Jackson Databind, ArchUnit
y Guava; de 403 a 2 017 clases) y en tres aplicaciones en capas cuya arquitectura
documentan sus propios proyectos (Spring PetClinic, MyBatis JPetStore 6 y DDD
Sample; se preparan con `arquitecta aplicaciones`), y mide si el mecanismo las
detecta: **31 de 31 detectadas, ningún hallazgo que no se inyectara**, en menos de
un segundo por sistema. En Guava, una deriva aislada deja la CR por encima del 97 % —«Aceptable»
para la CR sola— y el criterio con severidad la señala. Detalle, catálogos y datos:
[ejemplos/sistemas/](ejemplos/sistemas/README.md).

`arquitecta estudios` completa la validación con cuatro estudios cuya evidencia
queda en [ejemplos/estudios/](ejemplos/estudios/README.md): la **deriva real** a lo
largo de 35 versiones publicadas, la **calibración del umbral** de concentración por
percentiles, el contraste del grafo con **JDepend** como oráculo independiente y
las mismas reglas escritas con **ArchUnit** para comparar esfuerzo y resultado.

## Sobre código real: ArchUnit 1.3.0

El mecanismo se aplicó sobre una biblioteca de terceros —923 clases propias en 22
paquetes— declarando como hipótesis la estratificación `base ← core ← lang ←
library`. El análisis completo tarda **1,2 segundos** y se hace directamente
sobre el `.jar`; las configuraciones están en `ejemplos/archunit/`:

```bat
arquitecta analizar lib\archunit-1.3.0.jar ejemplos\archunit\rules-archunit.properties --svg archunit.svg
```

![Grafo de ArchUnit 1.3.0 dibujado por la herramienta](ejemplos/graficos/archunit.svg)

Las capas aparecen en el orden correcto aunque la configuración las declara de
abajo arriba, porque el orden se deduce de la matriz de dependencias permitidas.

|                          | Sin exenciones | Con exenciones declaradas |
| ------------------------ | -------------: | ------------------------: |
| Paquetes                 |             43 |                        22 |
| Dependencias             |            222 |                       107 |
| Integridad de capas      |          61/61 |                     61/61 |
| Ciclos (mayor)           |    12 incumpl. |  9 incumpl. (7 → 4 paq.)  |
| Concentración            |    14 marcados |                 5 marcados |
| **Tasa de Conformidad**  |      82,31 %   |                  86,41 %  |

ArchUnit empaqueta Guava y ASM bajo su propio prefijo: sin exenciones, casi la
mitad del grafo es código que no le pertenece. Declararlas elimina **9 de los 14**
hallazgos de concentración y reduce el ciclo mayor de 7 paquetes a 4, porque el
paquete de anotaciones al que todo referencia actuaba como puente entre módulos
que entre sí no se alcanzan.

Dos lecturas importan. La primera: la regla de capas dio **61/61 en ambas
configuraciones**, de modo que la hipótesis sobre su arquitectura era correcta y
la regla no produjo un solo falso positivo. La segunda: los tres ciclos
**sobreviven** a las exenciones, que es como debe ser — el objetivo era retirar el
ruido, no silenciar los hallazgos.

## Evidencia generada

`arquitecta experimento` escribe en `resultados/`:

```
resultados/
├─ catalogo-reglas.txt          catálogo derivado de la implementación
├─ modo-cr/                     un subdirectorio por modo de clasificación
│  ├─ resumen.csv               una fila por escenario
│  ├─ c1-capas.txt              reporte legible
│  ├─ c1-capas.json             evidencia estructurada y completa
│  ├─ c1-capas.svg              grafo dibujado (abrir en el navegador)
│  ├─ c1-capas.tikz             el mismo grafo como figura LaTeX
│  ├─ c1-capas.dot              el mismo grafo para Graphviz
│  └─ c1-capas-hallazgos.csv    una fila por hallazgo
└─ modo-cr-severidad/           idem, con la clasificación refinada
```

Los `.svg` se abren directamente en cualquier navegador. Los `.dot` pueden
procesarse además con Graphviz si se prefiere.

## Pruebas

`arquitecta pruebas` ejecuta 272 comprobaciones sobre el grafo, el algoritmo de Tarjan, la
carga de configuración, las tres reglas, el motor, las exenciones, los cinco
escenarios, los exportadores, la disposición gráfica, la comparación entre
corridas, el servicio de análisis (reconocimiento de proyectos Maven y Gradle,
paridad entre carpeta y `.jar`, configuración mínima) y el asistente de catálogo
(contra un servidor local que imita las API de los modelos), incluidos los casos
límite (grafo vacío, umbrales en su frontera exacta, configuraciones inválidas,
exenciones que no cuelgan de la raíz). La batería se construyó **sin dependencias
externas**, en coherencia con el requisito de ligereza del propio mecanismo, y
devuelve código de salida distinto de cero si alguna comprobación falla.

## Compilación con Maven

```bat
mvn -q clean package
```

Genera `target/arquitecta.jar` con las dependencias empaquetadas
(`maven-shade-plugin`). Ejecutado sin argumentos (`java -jar target/arquitecta.jar`,
o con doble clic) abre la interfaz gráfica. La batería de pruebas se compila en la fase
`test-compile`, pero se ejecuta mediante `arquitecta pruebas`, no por
_surefire_, porque deliberadamente no depende de un marco de pruebas externo.

## Notas

- Al ejecutar con **JDK 26** aparecen avisos benignos en `stderr` (SLF4J sin
  proveedor de _logging_ y una advertencia sobre `sun.misc.Unsafe` emitida por
  Guava, embebida en ArchUnit). No afectan el resultado; con JDK 17 no se
  presentan.
- Los scripts fijan la página de códigos UTF-8 (`chcp 65001`) y la salida se
  emite explícitamente en UTF-8, de modo que los reportes conserven los acentos
  en la consola de Windows.
