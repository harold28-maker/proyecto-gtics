CREATE TABLE IF NOT EXISTS categorias_habilidades (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    CONSTRAINT uk_categoria_habilidad_nombre UNIQUE (nombre)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci;

-- El nombre de la primera habilidad de cada grupo conserva la grafía del catálogo existente.
INSERT INTO categorias_habilidades (nombre)
SELECT TRIM(REGEXP_REPLACE(h.categoria, '[[:space:]]+', ' '))
FROM habilidades h
JOIN (
    SELECT MIN(id_habilidad) AS id_habilidad
    FROM habilidades
    WHERE categoria IS NOT NULL
      AND TRIM(REGEXP_REPLACE(categoria, '[[:space:]]+', ' ')) <> ''
    GROUP BY TRIM(REGEXP_REPLACE(categoria, '[[:space:]]+', ' ')) COLLATE utf8mb4_0900_as_ci
) origen ON origen.id_habilidad = h.id_habilidad
WHERE NOT EXISTS (
    SELECT 1
    FROM categorias_habilidades c
    WHERE c.nombre = TRIM(REGEXP_REPLACE(h.categoria, '[[:space:]]+', ' ')) COLLATE utf8mb4_0900_as_ci
)
ON DUPLICATE KEY UPDATE nombre = categorias_habilidades.nombre;

UPDATE habilidades h
JOIN categorias_habilidades c
  ON c.nombre = TRIM(REGEXP_REPLACE(h.categoria, '[[:space:]]+', ' ')) COLLATE utf8mb4_0900_as_ci
SET h.categoria = c.nombre
WHERE CAST(h.categoria AS BINARY) <> CAST(c.nombre AS BINARY);
