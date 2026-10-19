# Calibración del umbral de concentración

Distribución del acoplamiento eferente (Ce) de los paquetes que FF3 verifica (sin los de rol compartido). Percentiles por rango más cercano.

| Sistema | Conjunto | Paquetes | p50 | p70 | p80 | p90 | Máx. | Umbral del catálogo | Señalados | Señalados con p90 de calibración (Ce > 10) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Apache Commons Lang 3.14.0 | calibracion | 17 | 2 | 2 | 2 | 4 | 5 | 5 | 0 | 0 |
| Jackson Databind 2.17.2 | calibracion | 22 | 6 | 10 | 11 | 12 | 14 | 12 | 2 | 6 |
| ArchUnit 1.3.0 | calibracion | 21 | 5 | 6 | 7 | 8 | 10 | 6 | 5 | 0 |
| Guava 33.2.1 | calibracion | 18 | 3 | 5 | 6 | 7 | 7 | 8 | 0 | 0 |
| Spring Framework PetClinic | validacion | 8 | 2 | 2 | 2 | 3 | 3 | 8 | 0 | 0 |
| MyBatis JPetStore 6 | validacion | 5 | 1 | 2 | 2 | 2 | 2 | 8 | 0 | 0 |
| DDD Sample (Citerus) | validacion | 24 | 4 | 4 | 5 | 6 | 10 | 8 | 1 | 0 |
| Caso de estudio (C0) | validacion | 4 | 1 | 2 | 2 | 2 | 2 | 3 | 0 | 0 |

Conjunto de calibración: 78 paquetes; p50 = 4, p70 = 6, p80 = 7, **p90 = 10**.
