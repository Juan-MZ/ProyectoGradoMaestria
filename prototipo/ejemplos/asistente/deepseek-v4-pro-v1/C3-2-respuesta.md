# C3 · repetición 2 · deepseek-v4-pro

Reconozco una arquitectura de capas horizontal: `presentacion -> negocio -> persistencia -> dominio`. El paquete `util` es transversal y concentra dependencias hacia todas las capas, lo que sugiere deriva; no lo declaro como capa, así se evalúa por concentración y ciclos. Asumo que presentación no debe saltar a dominio y que negocio puede usar persistencia y dominio; si pretenden inversión de dependencias, hay que ajustar la matriz.
