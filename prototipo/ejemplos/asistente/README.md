# Exploración del asistente de catálogo con un modelo real

Evidencia de la exploración descrita en el Anexo B de la monografía. **No forma
parte del experimento controlado**: el asistente es una ayuda para redactar el
catálogo, ajena al mecanismo evaluado.

**Pregunta.** ¿Es seguro adoptar el catálogo que propone el asistente? En
concreto: con ese catálogo, ¿la herramienta sigue detectando las derivas que se
indujeron en el caso de estudio, o el modelo, al describir el código que ve, las
legitima?

**Procedimiento** (19 de octubre de 2026). Para cada escenario C0–C4 el
asistente propuso un catálogo **sin ningún contexto**, solo con el perfil
estructural del código —que en C1–C4 ya contiene las derivas—. Cada propuesta se
validó y se previsualizó sobre el escenario. Proveedor DeepSeek, dos modelos,
tres repeticiones por escenario y dos versiones de las instrucciones.

| Carpeta | Modelo | Instrucciones |
| --- | --- | --- |
| `deepseek-v4-pro-v1/` | `deepseek-v4-pro` | iniciales |
| `deepseek-flash-v1/` | `deepseek-flash` | iniciales |
| `deepseek-v4-pro-v2/` | `deepseek-v4-pro` | con reglas sobre el umbral y el núcleo de dominio |
| `deepseek-flash-v2/` | `deepseek-flash` | con reglas sobre el umbral y el núcleo de dominio |

Cada carpeta contiene `resultados.csv` y, por corrida, el catálogo propuesto
(`Cn-r.properties`) y la explicación del modelo (`Cn-r-respuesta.md`).

## Resultados

| | v4-pro v1 | flash v1 | v4-pro v2 | flash v2 |
| --- | --- | --- | --- | --- |
| Catálogos válidos | 15/15 | 15/15 | 15/15 | 15/15 |
| Estratificación prevista bien declarada | 15/15 | 15/15 | 15/15 | 15/15 |
| Derivas inducidas que siguen detectándose | 21/21 | 21/21 | **20/21** | 21/21 |
| Corridas con hallazgos sobre la línea base (C0) | 1/3 | 1/3 | 0/3 | 0/3 |
| Corridas con algún hallazgo adicional | 7/15 | 5/15 | 4/15 | 4/15 |
| Umbral de concentración propuesto | 2–6 | 2–5 | 2–6 | 2–5 |
| Mediana de tiempo por propuesta | 44 s | 12 s | 50 s | 11 s |

- **Ningún catálogo legitimó una deriva de capas ni un ciclo** en las 60 corridas.
- **El umbral es inestable.** En una corrida (v4-pro v2, C4) el modelo lo fijó en
  6 y ocultó la concentración inducida en `util` (Ce = 4); ya había ocurrido en
  una consulta exploratoria previa, no registrada aquí. Las instrucciones de la
  segunda versión no lo evitaron.
- **Los hallazgos sobre C0** venían de declarar `dominio` como capa sin permitir
  que la presentación lo usara; la segunda versión los eliminó, a cambio de
  umbrales más bajos que señalan la presentación de C1 (Ce = 3).
- **Salvaguarda determinista.** Como las instrucciones no bastan, la herramienta
  comprueba cada propuesta (`Revision`): si el umbral deja sin señalar al paquete
  más acoplado y este destaca sobre el resto sin estar exento, lo advierte.
  Aplicada a posteriori a los 60 catálogos, **advirtió en la corrida que ocultaba
  la deriva** y en otras 18 en que el paquete más acoplado superaba al siguiente
  por una sola dependencia: es un aviso para revisar, no un veredicto.

**Límites.** Un único caso, pequeño y sintético; las instrucciones de la segunda
versión se ajustaron con este mismo caso; un solo proveedor; y no se contrastaron
las propuestas con catálogos declarados por arquitectos de sistemas reales.

## Reproducir

Requiere una clave configurada en el asistente (`~/.arquitecta/asistente.properties`):

```bat
java -cp "out\arquitecta-core;out\arquitecta-test;lib\*" ^
     co.javeriana.msw.arquitecta.asistente.EvaluacionAsistente 3 mi-etiqueta [modelo]
```

Los resultados variarán entre ejecuciones: el modelo no es determinista.
