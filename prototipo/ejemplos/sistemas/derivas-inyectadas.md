# Derivas inyectadas por sistema

## Apache Commons Lang 3.14.0

- Capas: no aplica (el catálogo no declara capas)
- Ciclo: `org.apache.commons.lang3.math` → `org.apache.commons.lang3.mutable` (cierra un ciclo sin violar las capas)
- Concentración: paquete nuevo `org.apache.commons.lang3.derivainyectada` con 6 dependencias salientes (umbral 5)

## ArchUnit 1.3.0

- Capas: `com.tngtech.archunit.core.domain` → `com.tngtech.archunit.library.cycle_detection` (dependencia que la matriz no permite y que no cierra ningún ciclo)
- Ciclo: `com.tngtech.archunit.lang.syntax.elements` → `com.tngtech.archunit.lang.syntax` (cierra un ciclo sin violar las capas)
- Concentración: paquete nuevo `com.tngtech.archunit.derivainyectada` con 7 dependencias salientes (umbral 6)

## Jackson Databind 2.17.2

- Capas: `com.fasterxml.jackson.databind.deser.impl` → `com.fasterxml.jackson.databind.ser` (dependencia que la matriz no permite y que no cierra ningún ciclo)
- Ciclo: `com.fasterxml.jackson.databind.jsonFormatVisitors` → `com.fasterxml.jackson.databind.module` (cierra un ciclo sin violar las capas)
- Concentración: paquete nuevo `com.fasterxml.jackson.databind.derivainyectada` con 13 dependencias salientes (umbral 12)

## Guava 33.2.1

- Capas: `com.google.common.base.internal` → `com.google.common.cache` (dependencia que la matriz no permite y que no cierra ningún ciclo)
- Ciclo: `com.google.common.annotations` → `com.google.common.base` (cierra un ciclo sin violar las capas)
- Concentración: paquete nuevo `com.google.derivainyectada` con 9 dependencias salientes (umbral 8)

