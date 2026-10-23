# ADR-0027. Estudio de caso sobre un sistema real, contrastado con el juicio de un experto

- **Estado:** Propuesta (pendiente de ejecución)
- **Fecha:** 2026-10-23
- **Relacionadas:** ADR-0016, ADR-0017, ADR-0024, ADR-0026

## Contexto

En la primera revisión, el director señaló que el experimento sobre el caso
construido por el autor (C0–C4) es una **demostración controlada**, no un
estudio de caso: el autor fabricó tanto el sistema como sus derivas. Las
validaciones sobre sistemas de código abierto (ADR-0016 a ADR-0018) usan código
real, pero nadie que conozca esos sistemas confirmó si sus hallazgos son deriva o
decisiones deliberadas. Lo que falta es un sistema real cuyo estado
arquitectónico conozca a fondo quien lo analiza, para contrastar el diagnóstico
de la herramienta con el juicio de un experto.

## Decisión

Realizar un estudio de caso sobre un sistema Java real de una empresa en la que
el autor trabaja o trabajó, con este protocolo:

1. **Antes de ejecutar la herramienta**, el autor, como experto en el sistema,
   registra por escrito:
   - la arquitectura prevista (capas, dependencias permitidas, exenciones);
   - las desviaciones que sabe o sospecha que existen, con su ubicación;
   - su estimación de la conformidad del sistema.

   Con eso fija el catálogo y la lista de derivas esperadas, de modo que no se
   ajusten después a lo que reporte la herramienta.
2. Ejecutar la herramienta con ese catálogo.
3. Contrastar hallazgo por hallazgo y clasificar cada uno:
   - **coincidente**: el experto lo esperaba y la herramienta lo reporta;
   - **nuevo y confirmado**: la herramienta lo reporta, el experto no lo
     esperaba y lo reconoce como deriva;
   - **falso positivo o decisión deliberada**: la herramienta lo reporta, pero
     el experto lo descarta como deriva;
   - **omitido**: el experto lo esperaba y la herramienta no lo reporta.

   Para los omitidos, explicar si quedan fuera de las tres reglas o caen en un
   punto ciego de la extracción.
4. Redactarlo como un estudio de caso completo: contexto, fuentes de datos,
   procedimiento, análisis, conclusiones y amenazas a la validez.

## Condiciones éticas

El anteproyecto exige autorización expresa de la entidad propietaria cuando el
caso de estudio le pertenece, y que el análisis se restrinja a información
estructural. Por tanto:

- se pide autorización por escrito a la empresa;
- la empresa y el sistema se anonimizan («empresa del sector X», paquetes
  renombrados);
- solo se publican métricas agregadas y la estructura de paquetes anonimizada,
  nunca código;
- el análisis se ejecuta localmente, sin asistente remoto.

Si no hay autorización, se documenta como impedimento y se usa la alternativa.

## Alternativas consideradas

- **Mantener solo la demostración controlada**: deja abierta la objeción del
  director y la de los jurados.
- **Un sistema de código abierto «conocido» por el autor**: no hay un experto
  que conozca la intención real del sistema.
- **Analizar sin autorización, solo anonimizando**: contradice el tratamiento de
  riesgos éticos del anteproyecto.

## Consecuencias

- Aporta la evidencia de mayor valor del trabajo: precisión de la herramienta
  frente a un experto sobre un sistema real.
- Responde en parte al componente (iii) de la hipótesis, porque el experto juzga
  si los hallazgos le servirían para decidir. No sustituye la evaluación con
  varios desarrolladores (ADR-0024).
- El experto es el propio autor, lo que introduce un sesgo que debe declararse.
  Se atenúa registrando sus expectativas antes de ejecutar la herramienta.

## Respaldo

- Runeson y Höst (2009) y Wohlin et al. (2012): diseño y reporte de estudios de
  caso en ingeniería de software.
- Murphy et al. (2001): contraste entre el modelo previsto por el ingeniero y el
  extraído del código.
