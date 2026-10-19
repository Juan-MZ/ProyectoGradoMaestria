# C4 · repetición 1 · deepseek-v4-pro

Reconozco una arquitectura en capas: presentación → negocio → persistencia → dominio, con utilidades como base transversal. El perfil muestra inversiones fuertes: persistencia depende de presentación y de negocio (genera ciclo), y utilidades depende de todas las capas, lo que contradice su papel. Fijo umbral Ce=3 para marcar utilidades (Ce=4) sin afectar al resto. Dudas: si presentación puede usar dominio directamente y el papel exacto de utilidades.
