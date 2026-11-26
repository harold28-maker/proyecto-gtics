-- Añade tecnologías a instalaciones que ya tenían la tabla de proyectos.
SET @actualizar_proyectos = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'proyectos' AND column_name = 'tecnologias'),
    'SELECT 1',
    'ALTER TABLE proyectos ADD COLUMN tecnologias VARCHAR(300) NULL'
);
PREPARE actualizar_proyectos FROM @actualizar_proyectos;
EXECUTE actualizar_proyectos;
DEALLOCATE PREPARE actualizar_proyectos;
