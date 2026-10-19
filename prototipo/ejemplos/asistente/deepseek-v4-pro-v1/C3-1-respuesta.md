# C3 · repetición 1 · deepseek-v4-pro

Reconozco una arquitectura en capas clásica: presentación → negocio → persistencia → dominio. El paquete `com.caso.util` queda fuera de la integridad de capas, pero preocupa: depende de todas las capas, incluida `presentacion`, y concentra el mayor acoplamiento saliente (Ce=4). No lo declaro capa para no legitimar esas dependencias y fijo el umbral de concentración en 3 para que la FF3 lo señale. También dejo como deriva probable la dependencia `presentacion -> dominio`, que salta la capa de negocio.
