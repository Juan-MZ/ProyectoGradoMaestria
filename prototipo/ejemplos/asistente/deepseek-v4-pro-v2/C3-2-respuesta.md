# C3 · repetición 2 · deepseek-v4-pro

Reconozco una estratificación en capas: presentación → negocio → persistencia → dominio, con dominio como núcleo compartido. Dejo `com.caso.util` fuera de capas porque no es una capa de aplicación; su Ce=4 lo dejará señalado por concentración. Las dependencias de `util` hacia presentación, negocio y persistencia parecen deriva. El umbral se fija en 2 porque los paquetes sanos no superan ese Ce.
