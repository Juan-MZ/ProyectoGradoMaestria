# Evolución histórica: deriva real frente a un catálogo declarado

Cada serie aplica, sin modificarlo, el catálogo declarado para la versión de referencia (sección 4.7) a versiones publicadas antes y después de ella. Las posteriores responden a la pregunta operativa del mecanismo: si el equipo hubiera declarado esa arquitectura, ¿qué deriva habría aparecido después? Las anteriores muestran desde cuándo existía lo que hoy se señala.

## Jackson Databind (referencia 2.17.2)

| Versión | Posición | Clases | Paquetes | CR | CR por regla | Capas | Ciclos (paq.) | Mayor SCC | Concentración | Estado (CR+sev.) | Nuevos | Resueltos |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2.4.0 | anterior | 554 | 20 | 65.38 % | 0.00 % | 1 | 16 | 16 | 1 | No conforme | 0 | 0 |
| 2.6.0 | anterior | 563 | 20 | 62.96 % | 0.00 % | 3 | 16 | 16 | 1 | No conforme | 2 | 0 |
| 2.8.0 | anterior | 581 | 20 | 62.26 % | 0.00 % | 3 | 16 | 16 | 1 | No conforme | 0 | 0 |
| 2.10.0 | anterior | 654 | 21 | 58.93 % | 0.00 % | 3 | 18 | 18 | 2 | No conforme | 2 | 1 |
| 2.12.0 | anterior | 700 | 22 | 59.32 % | 0.00 % | 3 | 19 | 19 | 2 | No conforme | 1 | 1 |
| 2.14.0 | anterior | 771 | 23 | 60.66 % | 0.00 % | 3 | 19 | 19 | 2 | No conforme | 0 | 0 |
| 2.16.0 | anterior | 778 | 23 | 60.00 % | 0.00 % | 3 | 19 | 19 | 2 | No conforme | 0 | 0 |
| 2.17.2 | referencia | 784 | 23 | 60.00 % | 0.00 % | 3 | 19 | 19 | 2 | No conforme | 0 | 0 |
| 2.18.0 | posterior | 789 | 23 | 60.00 % | 0.00 % | 3 | 19 | 19 | 2 | No conforme | 0 | 0 |
| 2.19.0 | posterior | 807 | 23 | 57.38 % | 0.00 % | 4 | 19 | 19 | 3 | No conforme | 2 | 0 |
| 2.20.0 | posterior | 808 | 23 | 57.38 % | 0.00 % | 4 | 19 | 19 | 3 | No conforme | 0 | 0 |
| 2.21.0 | posterior | 812 | 23 | 57.38 % | 0.00 % | 4 | 19 | 19 | 3 | No conforme | 0 | 0 |
| 2.22.0 | posterior | 811 | 23 | 57.38 % | 0.00 % | 4 | 19 | 19 | 3 | No conforme | 0 | 0 |

### Cambios entre versiones consecutivas

- **2.6.0**
  - aparece: Integridad de capas | com.fasterxml.jackson.databind.introspect , com.fasterxml.jackson.databind.ser
  - aparece: Integridad de capas | com.fasterxml.jackson.databind.introspect , com.fasterxml.jackson.databind.ser.impl
- **2.10.0**
  - aparece: Ciclos de dependencia | com.fasterxml.jackson.databind.annotation , com.fasterxml.jackson.databind.cfg , com.fasterxml.jackson.databind.deser , com.fasterxml.jackson.databind.deser.impl , com.fasterxml.jackson.databind.deser.std , com.fasterxml.jackson.databind.exc , com.fasterxml.jackson.databind.ext , com.fasterxml.jackson.databind.introspect , com.fasterxml.jackson.databind.json , com.fasterxml.jackson.databind.jsonschema , com.fasterxml.jackson.databind.jsontype , com.fasterxml.jackson.databind.jsontype.impl , com.fasterxml.jackson.databind.node , com.fasterxml.jackson.databind.ser , com.fasterxml.jackson.databind.ser.impl , com.fasterxml.jackson.databind.ser.std , com.fasterxml.jackson.databind.type , com.fasterxml.jackson.databind.util
  - aparece: Concentración de dependencias salientes | com.fasterxml.jackson.databind.deser
  - desaparece: Ciclos de dependencia | com.fasterxml.jackson.databind.annotation , com.fasterxml.jackson.databind.cfg , com.fasterxml.jackson.databind.deser , com.fasterxml.jackson.databind.deser.impl , com.fasterxml.jackson.databind.deser.std , com.fasterxml.jackson.databind.ext , com.fasterxml.jackson.databind.introspect , com.fasterxml.jackson.databind.jsonschema , com.fasterxml.jackson.databind.jsontype , com.fasterxml.jackson.databind.jsontype.impl , com.fasterxml.jackson.databind.node , com.fasterxml.jackson.databind.ser , com.fasterxml.jackson.databind.ser.impl , com.fasterxml.jackson.databind.ser.std , com.fasterxml.jackson.databind.type , com.fasterxml.jackson.databind.util
- **2.12.0**
  - aparece: Ciclos de dependencia | com.fasterxml.jackson.databind.annotation , com.fasterxml.jackson.databind.cfg , com.fasterxml.jackson.databind.deser , com.fasterxml.jackson.databind.deser.impl , com.fasterxml.jackson.databind.deser.std , com.fasterxml.jackson.databind.exc , com.fasterxml.jackson.databind.ext , com.fasterxml.jackson.databind.introspect , com.fasterxml.jackson.databind.jdk14 , com.fasterxml.jackson.databind.json , com.fasterxml.jackson.databind.jsonschema , com.fasterxml.jackson.databind.jsontype , com.fasterxml.jackson.databind.jsontype.impl , com.fasterxml.jackson.databind.node , com.fasterxml.jackson.databind.ser , com.fasterxml.jackson.databind.ser.impl , com.fasterxml.jackson.databind.ser.std , com.fasterxml.jackson.databind.type , com.fasterxml.jackson.databind.util
  - desaparece: Ciclos de dependencia | com.fasterxml.jackson.databind.annotation , com.fasterxml.jackson.databind.cfg , com.fasterxml.jackson.databind.deser , com.fasterxml.jackson.databind.deser.impl , com.fasterxml.jackson.databind.deser.std , com.fasterxml.jackson.databind.exc , com.fasterxml.jackson.databind.ext , com.fasterxml.jackson.databind.introspect , com.fasterxml.jackson.databind.json , com.fasterxml.jackson.databind.jsonschema , com.fasterxml.jackson.databind.jsontype , com.fasterxml.jackson.databind.jsontype.impl , com.fasterxml.jackson.databind.node , com.fasterxml.jackson.databind.ser , com.fasterxml.jackson.databind.ser.impl , com.fasterxml.jackson.databind.ser.std , com.fasterxml.jackson.databind.type , com.fasterxml.jackson.databind.util
- **2.19.0**
  - aparece: Concentración de dependencias salientes | com.fasterxml.jackson.databind.introspect
  - aparece: Integridad de capas | com.fasterxml.jackson.databind.introspect , com.fasterxml.jackson.databind.deser.impl

## Guava (referencia 33.2.1-jre)

| Versión | Posición | Clases | Paquetes | CR | CR por regla | Capas | Ciclos (paq.) | Mayor SCC | Concentración | Estado (CR+sev.) | Nuevos | Resueltos |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 18.0 | anterior | 1687 | 16 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 21.0 | anterior | 1834 | 17 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 24.0-jre | anterior | 1934 | 17 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 27.0-jre | anterior | 1954 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 30.0-jre | anterior | 2022 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 33.0.0-jre | anterior | 2015 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 33.2.1-jre | referencia | 2017 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 33.3.0-jre | posterior | 2014 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 33.4.0-jre | posterior | 2015 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 33.5.0-jre | posterior | 1958 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 33.6.0-jre | posterior | 1965 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |
| 33.7.0-jre | posterior | 1960 | 18 | 100.00 % | 100.00 % | 0 | 0 | 1 | 0 | Aceptable | 0 | 0 |

### Cambios entre versiones consecutivas

Ninguno.

## Apache Commons Lang (referencia 3.14.0)

| Versión | Posición | Clases | Paquetes | CR | CR por regla | Capas | Ciclos (paq.) | Mayor SCC | Concentración | Estado (CR+sev.) | Nuevos | Resueltos |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 3.1 | anterior | 153 | 12 | 90.91 % | 50.00 % | 0 | 2 | 2 | 0 | No conforme | 0 | 0 |
| 3.4 | anterior | 224 | 12 | 86.36 % | 50.00 % | 0 | 3 | 3 | 0 | No conforme | 1 | 1 |
| 3.7 | anterior | 270 | 13 | 87.50 % | 50.00 % | 0 | 3 | 3 | 0 | No conforme | 0 | 0 |
| 3.10 | anterior | 288 | 14 | 88.46 % | 50.00 % | 0 | 3 | 3 | 0 | No conforme | 0 | 0 |
| 3.12.0 | anterior | 345 | 17 | 84.38 % | 50.00 % | 0 | 5 | 3 | 0 | No conforme | 1 | 0 |
| 3.14.0 | referencia | 403 | 18 | 82.35 % | 50.00 % | 0 | 6 | 6 | 0 | No conforme | 1 | 2 |
| 3.16.0 | posterior | 395 | 18 | 82.35 % | 50.00 % | 0 | 6 | 6 | 0 | No conforme | 0 | 0 |
| 3.18.0 | posterior | 413 | 18 | 82.35 % | 50.00 % | 0 | 6 | 6 | 0 | No conforme | 0 | 0 |
| 3.20.0 | posterior | 421 | 18 | 82.35 % | 50.00 % | 0 | 6 | 6 | 0 | No conforme | 0 | 0 |
| 3.21.0 | posterior | 439 | 18 | 79.41 % | 0.00 % | 0 | 6 | 6 | 1 | No conforme | 1 | 0 |

### Cambios entre versiones consecutivas

- **3.4**
  - aparece: Ciclos de dependencia | org.apache.commons.lang3.builder , org.apache.commons.lang3.reflect , org.apache.commons.lang3.tuple
  - desaparece: Ciclos de dependencia | org.apache.commons.lang3.builder , org.apache.commons.lang3.tuple
- **3.12.0**
  - aparece: Ciclos de dependencia | org.apache.commons.lang3.function , org.apache.commons.lang3.stream
- **3.14.0**
  - aparece: Ciclos de dependencia | org.apache.commons.lang3.builder , org.apache.commons.lang3.exception , org.apache.commons.lang3.function , org.apache.commons.lang3.reflect , org.apache.commons.lang3.stream , org.apache.commons.lang3.tuple
  - desaparece: Ciclos de dependencia | org.apache.commons.lang3.builder , org.apache.commons.lang3.reflect , org.apache.commons.lang3.tuple
  - desaparece: Ciclos de dependencia | org.apache.commons.lang3.function , org.apache.commons.lang3.stream
- **3.21.0**
  - aparece: Concentración de dependencias salientes | org.apache.commons.lang3.builder

