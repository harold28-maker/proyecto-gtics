CREATE TABLE IF NOT EXISTS recuperaciones_contrasena (
    id_usuario INT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    huella_cuenta VARCHAR(64) NOT NULL,
    solicitado_en DATETIME(6) NOT NULL,
    expira_en DATETIME(6) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);
