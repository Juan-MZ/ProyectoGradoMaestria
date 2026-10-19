# ADR-0014. Asistente de catálogo con modelos de lenguaje, como aporte complementario

- **Estado:** Aceptada
- **Fecha:** 2026-10-19 (retrospectiva)
- **Relacionadas:** ADR-0015

## Contexto

Quien adopta la herramienta casi nunca tiene su arquitectura prevista escrita, y
redactar el catálogo es la barrera de entrada. Un modelo de lenguaje puede
proponerlo a partir del perfil estructural del código.

## Decisión

Se incorpora un asistente que propone el catálogo y lo explica, con varios
proveedores (Anthropic, OpenAI, DeepSeek, Ollama, LM Studio, compatibles). El
asistente **no implementa Fitness Functions ni decide**: propone una
declaración que la persona revisa. Una revisión determinista advierte cuando el
umbral propuesto deja sin señalar al paquete más acoplado. **No cuenta para
OBJ1-OBJ4**: es un aporte complementario y se evalúa en el anexo B.

## Alternativas consideradas

- Sin asistente: la barrera de entrada queda intacta.
- Asistente que genera reglas nuevas: fuera del alcance aprobado y no
  verificable.

## Consecuencias

- En 60 corridas (DeepSeek), el asistente no legitimó derivas de capas ni
  ciclos, pero su umbral fue inestable (2-6) y una vez ocultó una concentración;
  la revisión determinista la habría señalado.
- Con un modelo local (15 corridas) aparecieron dos vías nuevas: eximir al
  paquete concentrador como `rol.compartido` (5 corridas) y permitir una
  dependencia entre capas que era deriva (1). La revisión se amplió con una
  segunda regla: advertir cuando una exención recae sobre el paquete que más
  concentra. Aplicadas a posteriori a las 75 propuestas, las dos reglas
  advierten en todas las que ocultaban derivas de concentración, sin activarse
  la nueva en ninguna de DeepSeek. La legitimación de capas no se puede
  comprobar con reglas: es el juicio que el catálogo expresa.
- Riesgo de privacidad: el perfil (nombres de paquetes y clases, sin código)
  sale del equipo si el proveedor es remoto (ADR-0015).

## Respaldo

Riesgo ético de supervisión humana del anteproyecto; evidencia en
`prototipo/ejemplos/asistente/`.
