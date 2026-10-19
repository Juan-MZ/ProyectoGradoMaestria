# ADR-0015. El asistente usa por defecto un modelo local

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

El anteproyecto adopta la privacidad por diseño y el procesamiento local. El
asistente proponía por defecto un proveedor remoto, de modo que la primera
consulta enviaba el perfil del código fuera del equipo salvo que la persona
cambiara la configuración.

## Decisión

Sin configuración guardada, el asistente propone **Ollama** (modelo local).
Usar un servicio remoto exige elegirlo expresamente, y la ventana muestra
siempre qué sale del equipo. Se evaluó el asistente también con un modelo local
(anexo B).

## Alternativas consideradas

- Mantener un proveedor remoto por defecto: mejor calidad de respuesta de
  entrada, a costa del principio de privacidad aprobado.

## Consecuencias

- Ningún dato sale del equipo sin una decisión explícita.
- Requiere tener Ollama y un modelo instalados; si no los hay, la ventana lo
  indica y la persona puede elegir otro proveedor.
- Prueba: `PruebasAsistente` comprueba que la configuración inicial es local.
- Costo medido (anexo B): `qwen2.5:14b` local, en CPU, produjo 11 catálogos
  válidos de 15, ocultó 6 de 17 derivas (5 por exención del paquete
  concentrador, 1 legitimando una deriva de capas) y tardó ~100 s por propuesta en CPU (~11 s en GPU con contexto de 8 192 tokens).
  Privacidad y fiabilidad entran en tensión; por eso se amplió la revisión
  determinista (ADR-0014) y la persona sigue decidiendo.

## Respaldo

Riesgo sobre información sensible del anteproyecto (tratamiento: evitarlo):
«se priorizará el procesamiento local y la minimización de datos».
