# ADR-0005. Catálogo de tres Fitness Functions (FF1-FF3) seleccionadas con criterios C1-C5

- **Estado:** Aceptada
- **Fecha:** 2026-09-02 (retrospectiva)
- **Relacionadas:** ADR-0012

## Contexto

El anteproyecto fija tres dimensiones de deriva: integridad de capas, ciclos de
dependencia y concentración excesiva de dependencias salientes. Había que
justificar por qué esas y no otras de las que la literatura propone.

## Decisión

Se definen cinco criterios de selección (C1 relación directa con la deriva
respecto de una arquitectura prevista; C2 detectabilidad estática sobre el
grafo; C3 criterio de incumplimiento explícito; C4 parámetros mínimos; C5
respaldo bibliográfico) y se aplican a las candidatas (tabla `tab:candidatas`
de la monografía). Se incluyen FF1 integridad de capas, FF2 ciclos (Tarjan) y
FF3 concentración eferente; se excluyen, entre otras, la dependencia inestable
y la distancia a la secuencia principal.

## Alternativas consideradas

- Incluir la dependencia inestable (SDP): requiere calcular la inestabilidad de
  cada paquete y su interpretación como deriva depende de la intención de
  diseño, que el catálogo no declara.
- Incluir la distancia a la secuencia principal: mide calidad de diseño y no
  tiene un umbral de consenso.

## Consecuencias

- El catálogo cubre las tres dimensiones aprobadas y nada más (alcance).
- Las reglas son genéricas y declarativas: el mismo binario evalúa sistemas
  distintos.

## Respaldo

Perry y Wolf (1992); Murphy et al. (2001); Knodel y Popescu (2007); de Silva y
Balasubramaniam (2012); Martin (2003); Sas et al. (2019); Ford et al. (2017,
2022); Tarjan (1972). Li et al. (2022) identifican la violación del patrón de
capas y las dependencias cíclicas como los síntomas estructurales de erosión
más frecuentes en la literatura.
