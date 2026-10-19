# Propuesta del asistente para Jackson Databind 2.17.2

Modelo: deepseek-v4-pro. Sin contexto: solo el perfil estructural.

Reconozco una arquitectura de biblioteca por subsistemas: motores de serialización/deserialización, introspección y tipos como estratos inferiores, y núcleos transversales sin estratificar (`cfg`, `annotation`, `util`, `node`, `jsontype`, etc.). La raíz actúa como fachada pública. Dejo fuera de capas los paquetes que hoy muestran acoplamiento bidireccional con el núcleo para no legitimar sus ciclos.
