# ADR-0002. Prototipo en Java estándar, sin Spring Boot ni otro framework

- **Estado:** Aceptada
- **Fecha:** 2026-09-02 (retrospectiva)

## Contexto

El anteproyecto acota el trabajo a un único ecosistema y a un prototipo. Se
preguntó si convenía adecuarlo a Spring Boot u otro framework conocido.

## Decisión

El prototipo se escribe en Java 17 estándar (biblioteca de la plataforma, Swing
para la interfaz, `java.net.http` para el asistente) y su única dependencia de
terceros es ArchUnit (con su dependencia SLF4J). Se distribuye como un único
`.jar` ejecutable y se compila con `javac` desde los lanzadores, sin Maven ni
Gradle.

## Alternativas consideradas

- Spring Boot: aporta inyección de dependencias, servidor web y configuración,
  ninguno de los cuales necesita una herramienta de línea de órdenes y de
  escritorio; añadiría decenas de dependencias al artefacto que se evalúa.
- Maven/Gradle para compilar: habitual, pero exige instalarlos o descargar un
  *wrapper*; los lanzadores con `javac` funcionan con solo un JDK.

## Consecuencias

- Una PyME puede ejecutar la herramienta copiando un archivo.
- El autoanálisis del prototipo (9 paquetes) es legible y sirve de ejemplo.
- Se renuncia a la gestión de dependencias automática: las dos bibliotecas se
  versionan en `prototipo/lib/`.

## Respaldo

Hamed et al. (2023) señalan que las PyMES requieren mecanismos de gobernanza
livianos y de bajo costo de adopción; es el requisito no funcional RNF1 de la
monografía.
