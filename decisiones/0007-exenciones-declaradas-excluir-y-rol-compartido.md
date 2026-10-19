# ADR-0007. Exenciones declaradas: `excluir` y `rol.compartido`

- **Estado:** Aceptada
- **Fecha:** 2026-10-01 (retrospectiva)

## Contexto

Al aplicar el mecanismo a ArchUnit, casi la mitad del grafo era código de
terceros reempaquetado bajo su prefijo, y las raíces de composición o fachadas
aparecían como concentraciones aunque su papel lo justifica.

## Decisión

Dos exenciones explícitas: `excluir` retira un subárbol de paquetes del
análisis (código ajeno o generado) y `rol.compartido` exime a un paquete exacto
de FF2 y FF3 por su papel. Ambas se enumeran siempre en el reporte.

## Alternativas consideradas

- Detectar el papel de un paquete automáticamente: la topología no distingue
  un componente dios de una fachada legítima.
- No permitir exenciones: el ruido haría inutilizable la herramienta en
  sistemas reales.

## Consecuencias

- En ArchUnit, eliminar las exenciones quitó 9 de 14 hallazgos de
  concentración sin hacer desaparecer ningún ciclo.
- Riesgo declarado: una exención mal concedida puede ocultar una deriva real; la
  salvaguarda es de procedimiento (declarada, versionada y reportada).

## Respaldo

Riesgo de «formalismo vacío» del anteproyecto (sección de riesgos éticos):
registro de excepciones justificadas.
