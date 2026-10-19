# ADR-0025. Reemplazar la literatura gris por fuentes arbitradas en afirmaciones centrales

- **Estado:** Aceptada
- **Fecha:** 2026-10-19

## Contexto

Varias afirmaciones del marco descansaban en artículos de Medium, páginas
comerciales o entradas `@misc` sin sede de publicación, y la cifra económica
del anteproyecto (65 % del presupuesto de TI) se atribuía a una tesis doctoral
cuyo texto no pudo verificarse (el repositorio institucional rechaza la
descarga).

## Decisión

Toda afirmación sustantiva lleva una fuente arbitrada: Ford et al. (2022, 2.ª
ed.) para *Fitness Functions*; Li et al. (2022, JSEP) para erosión; Besker et
al. (2019, JSS) para el costo de la deuda técnica (23 % del tiempo de
desarrollo); Alves et al. (2010) y Lanza y Marinescu (2006) para umbrales;
Arcelli Fontana et al. (2016) para *smells*; Tyree y Akerman (2005) para
decisiones arquitectónicas. Se actualizan Sas y Avgeriou a su versión publicada
en IEEE TSE (2023) y San Martín et al. se cita como preprint de arXiv. La
literatura gris que se conserva aparece solo como ejemplo de práctica
industrial, nunca como respaldo único. La cifra del 65 % no se repite en la
monografía.

## Alternativas consideradas

- Eliminar toda la literatura gris: algunas fuentes ilustran el uso industrial
  y forman parte del anteproyecto aprobado.

## Consecuencias

- El marco resiste la objeción «se apoya en blogs».
- Queda como pendiente del autor verificar en Pigazzini (2021) la cifra que cita
  el anteproyecto.

## Respaldo

Las propias referencias citadas.
