# Exploración del asistente de catálogo con modelos reales

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
tres repeticiones por escenario y dos versiones de las instrucciones; después,
el modelo local por defecto (Ollama, `qwen2.5:14b`) con la segunda versión. Esa
serie corrió en CPU: con el contexto por defecto de Ollama (32 768 tokens) el
modelo no cabía en los 12 GB de la GPU. Con `num_ctx = 8192` cabe entero y cada
propuesta tarda ~11 s en lugar de ~100 s; la corrida de control
`qwen2.5-14b-gpu-tiempos/` reproduce el mismo comportamiento.

| Carpeta | Modelo | Instrucciones |
| --- | --- | --- |
| `deepseek-v4-pro-v1/` | `deepseek-v4-pro` | iniciales |
| `deepseek-flash-v1/` | `deepseek-flash` | iniciales |
| `deepseek-v4-pro-v2/` | `deepseek-v4-pro` | con reglas sobre el umbral y el núcleo de dominio |
| `deepseek-flash-v2/` | `deepseek-flash` | con reglas sobre el umbral y el núcleo de dominio |
| `qwen2.5-14b-local/` | `qwen2.5:14b` (Ollama, local) | segunda versión |

Cada carpeta contiene `resultados.csv` y, por corrida, el catálogo propuesto
(`Cn-r.properties`) y la explicación del modelo (`Cn-r-respuesta.md`).

## Resultados

| | v4-pro v1 | flash v1 | v4-pro v2 | flash v2 | qwen 14B local |
| --- | --- | --- | --- | --- | --- |
| Catálogos válidos | 15/15 | 15/15 | 15/15 | 15/15 | 11/15 |
| Estratificación prevista bien declarada | 15/15 | 15/15 | 15/15 | 15/15 | 10/11 |
| Derivas inducidas que siguen detectándose | 21/21 | 21/21 | **20/21** | 21/21 | **11/17** |
| Corridas con hallazgos sobre la línea base (C0) | 1/3 | 1/3 | 0/3 | 0/3 | 2/3 |
| Corridas con algún hallazgo adicional | 7/15 | 5/15 | 4/15 | 4/15 | 7/11 |
| Umbral de concentración propuesto | 2–6 | 2–5 | 2–6 | 2–5 | 3–6 |
| Mediana de tiempo por propuesta | 44 s | 12 s | 50 s | 11 s | 100 s (CPU) · ~11 s (GPU) |

- **Ningún catálogo de DeepSeek legitimó una deriva de capas ni un ciclo** en las
  60 corridas.
- **El modelo local es menos fiable**: ocultó 6 de 17 derivas, 5 declarando
  `util` como rol compartido (exento de concentración) y 1 permitiendo
  `persistencia → negocio` en C4. La revisión determinista se amplió para
  advertir cuando una exención recae sobre el paquete que más concentra;
  aplicada a posteriori (`revision.csv` en cada carpeta, clase
  `RevisionRetroactiva`) advierte en las 5 corridas que ocultaban concentración y
  en ninguna de DeepSeek.
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
