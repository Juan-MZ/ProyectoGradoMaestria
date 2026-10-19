# ADR-0012. FF3 mide solo la concentración eferente (Ce), no el *hub-like* completo

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

La monografía respaldaba FF3 en el *smell* *hub-like dependency*, que combina
dependencias entrantes y salientes, mientras que FF3 usa solo las salientes.

## Decisión

FF3 se presenta con su nombre preciso, **concentración de dependencias
salientes (Ce)**, que es la dimensión que fijó el anteproyecto; el *hub-like*
se cita como antecedente del que se toma solo el componente eferente. El
reporte sigue mostrando Ca como contexto.

## Alternativas consideradas

- Implementar el *hub-like* completo (Ce y Ca por encima de la mediana): cambia
  la dimensión aprobada y señala bibliotecas base legítimamente muy usadas.

## Consecuencias

- Elimina una atribución bibliográfica imprecisa.
- Un paquete con muchas dependencias entrantes no se señala; queda como trabajo
  futuro.

## Respaldo

Martin (2003) (Ce); Sas et al. (2019) y Arcelli Fontana et al. (2016)
(*hub-like*).
