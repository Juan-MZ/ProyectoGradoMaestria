# Las reglas del caso de estudio escritas con ArchUnit

Generado por `EquivalenteArchUnit` (`arquitecta estudios archunit`). Compara, sobre los cinco escenarios, lo que detectan las tres reglas escritas con ArchUnit y lo que detecta el mecanismo con `caso-estudio/rules.properties`.

| Escenario | ArchUnit: capas | ArchUnit: ciclos | ArchUnit: concentración | Mecanismo: hallazgos | Mecanismo: CR | Mecanismo: estado |
| --- | --- | --- | --- | --- | --- | --- |
| c0-base | pasa | pasa | pasa | 0 | 100.00 % | Aceptable |
| c1-capas | falla (3 líneas) | pasa | pasa | 1 | 90.91 % | En observación |
| c2-ciclo | pasa | falla (1 líneas) | pasa | 1 | 83.33 % | No conforme |
| c3-concentracion | pasa | pasa | falla: com.caso.util (Ce = 4) | 1 | 91.67 % | En observación |
| c4-combinado | falla (4 líneas) | falla (2 líneas) | falla: com.caso.util (Ce = 4) | 4 | 57.14 % | No conforme |

Detección por regla coincidente en los cinco escenarios: **sí**.

| Forma | Líneas de especificación | Lenguaje | Resultado que entrega |
| --- | --- | --- | --- |
| ArchUnit | 33 | Java (API fluida y código propio para Ce) | pasa/falla por regla, con la lista de accesos infractores |
| Catálogo del mecanismo | 14 | propiedades clave=valor | CR, estado con severidad, evidencia por hallazgo, alcance del diagnóstico, tendencia |

Se cuentan las líneas no vacías y sin comentarios: en ArchUnit, las del bloque de especificación de `EquivalenteArchUnit.java`; en el catálogo, las de `rules.properties`.
