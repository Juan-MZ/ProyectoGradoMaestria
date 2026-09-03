# Componentes de terceros

Este repositorio redistribuye material que **no es obra del autor del trabajo de
grado**. Cada componente conserva la titularidad y las condiciones de uso de sus
respectivos autores, que no se ven alteradas por las licencias declaradas en
[`LICENSE`](LICENSE) y [`LICENSE-CODE`](LICENSE-CODE).

La lista se elabora a partir de lo que declaran los propios archivos y los sitios
oficiales de cada proyecto. Cuando un dato no consta de forma verificable, se
indica expresamente en lugar de suponerlo.

---

## 1. Plantilla LaTeX de la Maestría

Ubicación: `Maestria_PlantillaProyectoGrado/`

La plantilla se incluye porque es **necesaria para compilar el documento**: sin
ella el archivo `main.tex` no produce el PDF. Se redistribuye sin modificaciones
sustanciales, salvo las indicadas más abajo.

| Archivo | Procedencia y titularidad | Condiciones |
| --- | --- | --- |
| `thesis.cls` | Generado a partir de `book.cls` del sistema base de LaTeX. Copyright 1993–2004 The LaTeX3 Project y autores individuales listados en el archivo. Adaptado posteriormente para la Pontificia Universidad Javeriana (el propio archivo indica «Edited by colarte»). | LaTeX Project Public License (LPPL), versión 1.3 o posterior, según declara la cabecera del archivo. |
| `format.tex` | Configuración de formato de la plantilla de la Maestría en Ingeniería de Software, Pontificia Universidad Javeriana. Elaborada por **Angela Villota** y **Luisa Rincón** (v1.0, abril de 2022), según la cabecera de `main.tex`. | La plantilla no se distribuye con un archivo de licencia. Su uso aquí es el previsto por sus autoras: la presentación de un trabajo de grado de la Maestría. |
| `slashbox.sty` | «slashbox style by K. Yasuoka, May 1993», según declara la primera línea del archivo. | Paquete LaTeX de distribución pública. No incluye un archivo de licencia propio. |
| `biblio.bib` | Bibliografía de ejemplo distribuida con la plantilla (véase el apartado 3). No la utiliza el documento de este trabajo, que emplea `referencias.bib`. | Igual que la plantilla. |

> **Modificaciones realizadas sobre la plantilla.** En `format.tex` se sustituyó
> `\usepackage{color}` por `\usepackage{xcolor}`, requisito de TikZ para las
> figuras del documento. `thesis.cls` y `slashbox.sty` se conservan sin cambios.

---

## 2. Emblema institucional

Ubicación: `Maestria_PlantillaProyectoGrado/img/pujlogo.png`

Logotipo de la **Pontificia Universidad Javeriana**. Es un signo distintivo
protegido, propiedad de la Universidad. Se incluye únicamente porque la portada
que genera `thesis.cls` lo requiere, y con el solo fin de identificar la
institución ante la cual se presenta el trabajo de grado. Su presencia aquí no
constituye una cesión de derechos ni autoriza su reutilización: cualquier otro
uso del emblema requiere autorización expresa de la Universidad.

---

## 3. Imágenes de ejemplo de la plantilla

Ubicación: `Maestria_PlantillaProyectoGrado/img/` y `.../img/prototipo/`

La plantilla se distribuye con las capturas de pantalla de un trabajo de grado
anterior, que sirven de ejemplo de maquetación. **Ninguna de estas imágenes es
obra del autor de este trabajo y ninguna es utilizada por el documento**: la
única imagen que `main.tex` referencia es `pujlogo.png`. Se conservan en el
repositorio para no alterar la plantilla original.

**Archivos afectados (22):**

- En `img/`: `Animation.PNG`, `Character.png`, `Comparative1Es.png`,
  `Comparative2Es.png`, `Comparative3Es.png`, `CookingFever.png`, `DCLP.jpg`.
- En `img/prototipo/`: `bienvenida.png`, `crearusuario.png`,
  `iniciodesesion.png`, `lobby.png`, `ni2.png`, `ni3.png`, `nivelbasico1.png`,
  `nivelbasico2.png`, `nivelbasico3.png`, `rc1.png`, `rc2.png`, `rc3.png`,
  `selecciondecoleccionables.png`, `selecciondeservidores.png`,
  `slecciondeniveles.png`.

**Sobre su autoría.** Los archivos de la plantilla no declaran quién es el autor
de estas imágenes. Por su contenido, y por la bibliografía de ejemplo que las
acompaña en `biblio.bib` —centrada en ludificación y en líneas de producto de
software—, corresponden a un trabajo de grado previo sobre un videojuego
educativo. **No es posible atribuirlas con certeza a una persona concreta a
partir del material disponible**, y este documento no lo hace para no imputar
una autoría sin fundamento. Los derechos pertenecen a su autor o autores
originales y, en su caso, a la Pontificia Universidad Javeriana.

Adicionalmente, `CookingFever.png` parece corresponder a una captura de un
videojuego comercial de un tercero, incluida en la plantilla con fines
ilustrativos y comparativos. Sus derechos pertenecen a su titular.

> Quien reutilice este repositorio debe tener presente que **estas imágenes están
> excluidas de la licencia CC BY-NC-ND 4.0** declarada para el documento.
> Si se desea un repositorio libre de material de terceros, basta con eliminar el
> contenido de `img/` salvo `pujlogo.png`; el documento compila igual.

---

## 4. Bibliotecas del prototipo

Ubicación: `prototipo/lib/`

Se incluyen los archivos `.jar` para que el prototipo pueda compilarse y
ejecutarse sin conexión a internet ni repositorio de artefactos, tal como exige
su requisito de ligereza (RNF2).

### ArchUnit 1.3.0 — `archunit-1.3.0.jar`

- **Titular:** TNG Technology Consulting GmbH (según el manifiesto del archivo:
  `Specification-Vendor: TNG Technology Consulting GmbH`).
- **Licencia:** Apache License, Version 2.0.
- **Sitio del proyecto:** <https://www.archunit.org/> ·
  <https://github.com/TNG/ArchUnit>
- **Uso en este trabajo:** importación del *bytecode* y resolución de las
  dependencias entre clases, en `co.javeriana.msw.arquitecta.grafo.ConstructorGrafo`.
- **Componentes que a su vez redistribuye,** empaquetados dentro del `.jar` bajo
  el prefijo `com.tngtech.archunit.thirdparty`:
  - **ASM** (<https://asm.ow2.org>) — licencia BSD, según el archivo
    `com/tngtech/archunit/thirdparty/org/objectweb/asm/asm.license` incluido en el `.jar`.
  - **Google Guava** (<https://github.com/google/guava>) — Apache License 2.0.

### SLF4J API 2.0.12 — `slf4j-api-2.0.12.jar`

- **Titular:** Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland), según
  `META-INF/LICENSE.txt` incluido en el archivo.
- **Licencia:** MIT License (el manifiesto declara
  `Bundle-License: http://www.opensource.org/licenses/mit-license.php`).
- **Sitio del proyecto:** <https://www.slf4j.org/>
- **Uso en este trabajo:** dependencia transitiva de ArchUnit. El prototipo no
  la invoca directamente y no configura ningún proveedor de *logging*, de ahí el
  aviso benigno que emite en la salida de error.

---

## 5. Imagen de compilación del documento

El documento se compila con la imagen de contenedor `xucheng/texlive-full`, que
empaqueta la distribución **TeX Live**. La imagen no se redistribuye en este
repositorio: los guiones `compilar.bat` y `compilar.sh` la descargan cuando hace
falta. TeX Live y los paquetes que contiene conservan sus respectivas licencias.

---

## Cómo reportar una atribución incorrecta

Si usted es titular de alguno de los materiales listados y considera que la
atribución es errónea o insuficiente, o que el material no debería figurar en
este repositorio, abra una *issue* en el repositorio o escriba al autor. La
corrección o retirada se atenderá con prontitud.
