CREATE TABLE IF NOT EXISTS permisos_deshabilitados (
    id_permiso_deshabilitado BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL,
    codigo VARCHAR(40) NOT NULL,
    CONSTRAINT uk_permiso_deshabilitado_rol UNIQUE (id_rol, codigo),
    CONSTRAINT fk_permiso_deshabilitado_rol FOREIGN KEY (id_rol) REFERENCES roles(id_rol) ON DELETE CASCADE
);
