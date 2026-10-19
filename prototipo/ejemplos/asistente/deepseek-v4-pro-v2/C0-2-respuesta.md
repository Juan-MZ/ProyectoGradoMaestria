# C0 · repetición 2 · deepseek-v4-pro

Reconozco una arquitectura en capas clásica: `presentacion -> negocio -> persistencia`. El paquete `com.caso.dominio` actúa como núcleo compartido sin capa: tiene Ce=0 y es usado por las tres capas; queda vigilado por ciclos y concentración. Las dependencias actuales no muestran saltos ni inversiones. El umbral de concentración lo propongo en 6 porque los Ce sanos actuales son 0-2 y deja margen para dependencias legítimas sin ocultar acoplamientos futuros.
