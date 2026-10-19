# ADR-0004. Granularidad de paquete para el grafo y las reglas

- **Estado:** Aceptada
- **Fecha:** 2026-09-02 (retrospectiva)

## Contexto

Las dependencias pueden analizarse entre clases, entre paquetes o entre módulos.

## Decisión

Los nodos del grafo son paquetes Java y una arista existe si al menos una clase
del paquete origen depende de una del paquete destino. La evidencia conserva
las referencias entre clases que originan cada arista.

## Alternativas consideradas

- Clases: un sistema de 2 000 clases produce un grafo ilegible y reglas de
  capas que hay que declarar por clase.
- Módulos de compilación (Maven/JPMS): demasiado grueso para el perfil PyME,
  que suele tener un único módulo.

## Consecuencias

- Las derivas entre clases del mismo paquete son invisibles (limitación
  declarada en la monografía y en el reporte).
- Coincide con la unidad en que Martin define Ce/Ca y los principios ADP/SDP.

## Respaldo

Martin (2003), cap. 20 (principios de paquetes); Sas et al. (2019) analizan
*smells* de ciclo y de concentración a nivel de paquete.
