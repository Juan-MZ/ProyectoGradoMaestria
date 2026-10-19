# ADR-0023. Compilar la monografía con Docker (TeX Live completo)

- **Estado:** Aceptada
- **Fecha:** 2026-10-19 (retrospectiva)

## Contexto

La plantilla de la universidad requiere paquetes que no siempre están en una
instalación local de LaTeX.

## Decisión

`compilar.bat|.sh` ejecuta pdflatex → bibtex → pdflatex ×2 dentro de la imagen
`xucheng/texlive-full`.

## Alternativas consideradas

- MiKTeX/TeX Live local: resultado dependiente de la instalación.

## Consecuencias

- PDF reproducible en cualquier equipo con Docker.

## Respaldo

—
