USE skillbridge_ai;

-- ============================================================
-- SkillBridge AI - Actualización incremental V3
-- Ejecutar SOLO si ya tienes creada la BD de una versión anterior.
-- No elimina tablas ni recrea la base de datos.
-- ============================================================

SET SQL_SAFE_UPDATES = 0;

-- Ajustar datos demo que excedían los nuevos límites visuales del perfil.
UPDATE colaboradores SET cargo = 'Desarrolladora Java' WHERE cargo = 'Desarrolladora Backend';
UPDATE colaboradores SET cargo = 'Desarrollador Java' WHERE cargo = 'Desarrollador Backend';
UPDATE colaboradores SET cargo = 'Desarrolladora Móvil' WHERE cargo = 'Desarrolladora Mobile';
UPDATE colaboradores SET area = 'Gestión Talento' WHERE area = 'People & Delivery';

-- Garantizar que cualquier dato previo no impida reducir las longitudes.
UPDATE usuarios SET nombres = LEFT(nombres,20), apellidos = LEFT(apellidos,20);
UPDATE usuarios SET telefono = LEFT(telefono,9) WHERE telefono IS NOT NULL;
UPDATE colaboradores SET cargo = LEFT(cargo,20) WHERE cargo IS NOT NULL;
UPDATE colaboradores SET area = LEFT(area,20) WHERE area IS NOT NULL;
UPDATE certificaciones SET nombre = LEFT(nombre,40), entidad_emisora = LEFT(entidad_emisora,40);

ALTER TABLE usuarios
    MODIFY nombres VARCHAR(20) NOT NULL,
    MODIFY apellidos VARCHAR(20) NOT NULL,
    MODIFY telefono VARCHAR(9);

ALTER TABLE colaboradores
    MODIFY cargo VARCHAR(20),
    MODIFY area VARCHAR(20);

ALTER TABLE certificaciones
    MODIFY nombre VARCHAR(40) NOT NULL,
    MODIFY entidad_emisora VARCHAR(40);

-- Normalizar años de experiencia antes de aplicar la restricción.
UPDATE colaborador_habilidad SET anios_experiencia = 0 WHERE anios_experiencia < 0;
UPDATE colaborador_habilidad SET anios_experiencia = 50 WHERE anios_experiencia > 50;

-- Agregar el CHECK solo si todavía no existe (MySQL 8+).
SET @check_existe = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'skillbridge_ai'
      AND TABLE_NAME = 'colaborador_habilidad'
      AND CONSTRAINT_NAME = 'chk_ch_anios_experiencia'
);
SET @sql_check = IF(
    @check_existe = 0,
    'ALTER TABLE colaborador_habilidad ADD CONSTRAINT chk_ch_anios_experiencia CHECK (anios_experiencia BETWEEN 0 AND 50)',
    'SELECT 1'
);
PREPARE stmt_check FROM @sql_check;
EXECUTE stmt_check;
DEALLOCATE PREPARE stmt_check;

-- Se agrega CHAT como tipo propio de notificación.
ALTER TABLE notificaciones
    MODIFY tipo ENUM('INFO','PROYECTO','ASIGNACION','FORO','CHAT','SISTEMA') DEFAULT 'INFO';

UPDATE notificaciones
SET tipo = 'CHAT'
WHERE titulo LIKE '%mensaje%' OR mensaje LIKE '%chat%';

SET SQL_SAFE_UPDATES = 1;
