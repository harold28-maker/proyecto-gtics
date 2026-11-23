-- Si se ejecuta manualmente, selecciona primero la base de SkillBridge.

CREATE TABLE IF NOT EXISTS mensajes_chatbot (
    id_mensaje_chatbot BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    remitente VARCHAR(12) NOT NULL,
    contenido TEXT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chatbot_usuario_mensaje (id_usuario, id_mensaje_chatbot),
    CONSTRAINT fk_chatbot_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT chk_chatbot_remitente
        CHECK (remitente IN ('USUARIO', 'ASISTENTE'))
);
