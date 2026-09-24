-- ============================================================
-- SkillBridge AI - Base de Datos MySQL 8+
-- VERSION DE AVANCE DEL PROYECTO
-- Diseño simplificado: 18 tablas esenciales
-- ============================================================

DROP DATABASE IF EXISTS skillbridge_ai;
CREATE DATABASE skillbridge_ai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE skillbridge_ai;

-- ============================================================
-- MÓDULO 1: AUTENTICACIÓN, ROLES Y PERMISOS
-- ============================================================

-- 1. ROLES
CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. PERMISOS
-- Para este avance cada permiso se asocia directamente a un rol.
CREATE TABLE permisos (
    id_permiso INT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    modulo VARCHAR(60) NOT NULL,
    descripcion VARCHAR(200),
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

-- 3. USUARIOS
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL,
    nombres VARCHAR(20) NOT NULL,
    apellidos VARCHAR(20) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    telefono VARCHAR(9),
    foto_url VARCHAR(300),
    estado ENUM('ACTIVO','INACTIVO','BLOQUEADO') DEFAULT 'ACTIVO',
    ultimo_acceso DATETIME,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol),
    CHECK (telefono IS NULL OR telefono REGEXP '^[0-9]{9}$')
);

-- ============================================================
-- MÓDULO 2: COLABORADORES, HABILIDADES Y CERTIFICACIONES
-- ============================================================

-- NOTA DE MODELO:
-- PROJECT_MANAGER y RESOURCE_MANAGER también pueden tener un registro en colaboradores,
-- porque siguen siendo personas de la organización con perfil, skills y disponibilidad.
-- Un ADMINISTRADOR técnico puede existir solo en usuarios sin registro en colaboradores.

-- 4. COLABORADORES
-- Relación Usuario-Colaborador:
--   * Cada colaborador pertenece a exactamente un usuario.
--   * Cada usuario puede tener como máximo un registro de colaborador.
-- Esto modela USUARIO 1 ---- 0..1 COLABORADOR y evita duplicar perfiles.
-- Se mantiene id_colaborador como PK independiente para simplificar el mapeo con JPA.
CREATE TABLE colaboradores (
    id_colaborador INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    cargo VARCHAR(20),
    area VARCHAR(20),
    seniority ENUM('JUNIOR','SEMI_SENIOR','SENIOR','LEAD') DEFAULT 'JUNIOR',
    biografia TEXT,
    intereses_profesionales TEXT,
    disponibilidad_base INT DEFAULT 100,
    CONSTRAINT uk_colaboradores_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_colaboradores_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CHECK (disponibilidad_base BETWEEN 0 AND 100),
    CHECK (cargo IS NULL OR CHAR_LENGTH(cargo) <= 20),
    CHECK (area IS NULL OR CHAR_LENGTH(area) <= 20)
);

-- 5. HABILIDADES
CREATE TABLE habilidades (
    id_habilidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    categoria VARCHAR(80),
    descripcion VARCHAR(250),
    estado BOOLEAN DEFAULT TRUE
);

-- 6. COLABORADOR_HABILIDAD
CREATE TABLE colaborador_habilidad (
    id_colaborador INT NOT NULL,
    id_habilidad INT NOT NULL,
    nivel ENUM('BASICO','INTERMEDIO','AVANZADO','EXPERTO') NOT NULL,
    anios_experiencia DECIMAL(4,1) DEFAULT 0,
    CHECK (anios_experiencia BETWEEN 0 AND 50),
    PRIMARY KEY (id_colaborador, id_habilidad),
    FOREIGN KEY (id_colaborador) REFERENCES colaboradores(id_colaborador) ON DELETE CASCADE,
    FOREIGN KEY (id_habilidad) REFERENCES habilidades(id_habilidad) ON DELETE CASCADE
);

-- 7. CERTIFICACIONES
-- Se guarda directamente el colaborador para mantener el modelo sencillo.
CREATE TABLE certificaciones (
    id_certificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_colaborador INT NOT NULL,
    nombre VARCHAR(40) NOT NULL,
    entidad_emisora VARCHAR(40),
    fecha_emision DATE,
    fecha_expiracion DATE,
    url_credencial VARCHAR(300),
    estado ENUM('VIGENTE','VENCIDA','SIN_EXPIRACION') DEFAULT 'VIGENTE',
    FOREIGN KEY (id_colaborador) REFERENCES colaboradores(id_colaborador) ON DELETE CASCADE
);

-- ============================================================
-- MÓDULO 3: PROYECTOS Y REQUERIMIENTOS TÉCNICOS
-- ============================================================

-- 8. PROYECTOS
CREATE TABLE proyectos (
    id_proyecto INT AUTO_INCREMENT PRIMARY KEY,
    id_project_manager INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    fecha_inicio DATE,
    fecha_fin DATE,
    vacantes INT DEFAULT 0,
    prioridad ENUM('BAJA','MEDIA','ALTA') DEFAULT 'MEDIA',
    estado ENUM('PLANNING','ACTIVE','ON_HOLD','COMPLETED','CANCELLED') DEFAULT 'PLANNING',
    progreso INT DEFAULT 0,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_project_manager) REFERENCES usuarios(id_usuario),
    CHECK (progreso BETWEEN 0 AND 100)
);

-- 9. PROYECTO_HABILIDAD
CREATE TABLE proyecto_habilidad (
    id_proyecto INT NOT NULL,
    id_habilidad INT NOT NULL,
    nivel_requerido ENUM('BASICO','INTERMEDIO','AVANZADO','EXPERTO') NOT NULL,
    vacantes INT DEFAULT 1,
    PRIMARY KEY (id_proyecto, id_habilidad),
    FOREIGN KEY (id_proyecto) REFERENCES proyectos(id_proyecto) ON DELETE CASCADE,
    FOREIGN KEY (id_habilidad) REFERENCES habilidades(id_habilidad)
);

-- ============================================================
-- MÓDULO 4: ASIGNACIONES Y CARGA DE TRABAJO
-- ============================================================

-- 10. ASIGNACIONES
CREATE TABLE asignaciones (
    id_asignacion INT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto INT NOT NULL,
    id_colaborador INT NOT NULL,
    rol_proyecto VARCHAR(100),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    porcentaje_dedicacion INT NOT NULL,
    estado ENUM('PLANIFICADA','ACTIVA','FINALIZADA','CANCELADA') DEFAULT 'PLANIFICADA',
    FOREIGN KEY (id_proyecto) REFERENCES proyectos(id_proyecto),
    FOREIGN KEY (id_colaborador) REFERENCES colaboradores(id_colaborador),
    CHECK (porcentaje_dedicacion BETWEEN 1 AND 100)
);

-- ============================================================
-- MÓDULO 5: AI TALENT MATCHING
-- ============================================================

-- 11. RECOMENDACIONES_IA
CREATE TABLE recomendaciones_ia (
    id_recomendacion INT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto INT NOT NULL,
    id_colaborador INT NOT NULL,
    porcentaje_match DECIMAL(5,2),
    justificacion TEXT,
    recomendado_por INT,
    fecha_recomendacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_proyecto) REFERENCES proyectos(id_proyecto),
    FOREIGN KEY (id_colaborador) REFERENCES colaboradores(id_colaborador),
    FOREIGN KEY (recomendado_por) REFERENCES usuarios(id_usuario),
    CHECK (porcentaje_match BETWEEN 0 AND 100)
);

-- ============================================================
-- MÓDULO 6: FOROS Y CONOCIMIENTO
-- ============================================================

-- 12. FOROS
CREATE TABLE foros (
    id_foro INT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto INT,
    id_autor INT NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    contenido TEXT NOT NULL,
    categoria VARCHAR(80),
    estado ENUM('ABIERTO','RESUELTO','CERRADO') DEFAULT 'ABIERTO',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_proyecto) REFERENCES proyectos(id_proyecto),
    FOREIGN KEY (id_autor) REFERENCES usuarios(id_usuario)
);

-- 13. RESPUESTAS_FORO
CREATE TABLE respuestas_foro (
    id_respuesta INT AUTO_INCREMENT PRIMARY KEY,
    id_foro INT NOT NULL,
    id_autor INT NOT NULL,
    contenido TEXT NOT NULL,
    es_solucion BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_foro) REFERENCES foros(id_foro) ON DELETE CASCADE,
    FOREIGN KEY (id_autor) REFERENCES usuarios(id_usuario)
);

-- 14. ETIQUETAS
CREATE TABLE etiquetas (
    id_etiqueta INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE
);

-- 15. FORO_ETIQUETA
CREATE TABLE foro_etiqueta (
    id_foro INT NOT NULL,
    id_etiqueta INT NOT NULL,
    PRIMARY KEY (id_foro, id_etiqueta),
    FOREIGN KEY (id_foro) REFERENCES foros(id_foro) ON DELETE CASCADE,
    FOREIGN KEY (id_etiqueta) REFERENCES etiquetas(id_etiqueta) ON DELETE CASCADE
);

-- ============================================================
-- MÓDULO 7: CHAT, NOTIFICACIONES Y ADMINISTRACIÓN
-- ============================================================

-- 16. MENSAJES_CHAT
CREATE TABLE mensajes_chat (
    id_mensaje INT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto INT NOT NULL,
    id_usuario INT NOT NULL,
    contenido TEXT NOT NULL,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_proyecto) REFERENCES proyectos(id_proyecto) ON DELETE CASCADE,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- 17. NOTIFICACIONES
CREATE TABLE notificaciones (
    id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    tipo ENUM('INFO','PROYECTO','ASIGNACION','FORO','CHAT','SISTEMA') DEFAULT 'INFO',
    leida BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 18. AUDITORIA
CREATE TABLE auditoria (
    id_auditoria INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT,
    accion VARCHAR(100) NOT NULL,
    modulo VARCHAR(80),
    detalle VARCHAR(500),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ============================================================
-- DATOS INICIALES PARA DEMOSTRACIÓN
-- ============================================================

INSERT INTO roles (nombre, descripcion) VALUES
('COLABORADOR', 'Gestiona su perfil, habilidades y proyectos asignados'),
('PROJECT_MANAGER', 'Gestiona proyectos y asignaciones'),
('RESOURCE_MANAGER', 'Gestiona disponibilidad, carga y talento'),
('ADMINISTRADOR', 'Administra usuarios, roles y configuración');

INSERT INTO permisos (id_rol, nombre, modulo) VALUES
(1, 'Consultar perfil propio', 'Perfil'),
(1, 'Gestionar habilidades propias', 'Habilidades'),
(1, 'Consultar proyectos asignados', 'Proyectos'),
(1, 'Participar en foros y chat', 'Colaboracion'),
(2, 'Crear y editar proyectos', 'Proyectos'),
(2, 'Gestionar asignaciones', 'Asignaciones'),
(3, 'Consultar colaboradores', 'Colaboradores'),
(3, 'Gestionar carga de trabajo', 'Asignaciones'),
(3, 'Usar Talent Matching IA', 'IA'),
(4, 'Gestionar usuarios', 'Administracion'),
(4, 'Gestionar roles y permisos', 'Administracion'),
(4, 'Consultar auditoria', 'Administracion');

INSERT INTO habilidades (nombre, categoria, descripcion) VALUES
('Java', 'Backend', 'Programación backend con Java'),
('Spring Boot', 'Backend', 'Desarrollo de servicios con Spring Boot'),
('MySQL', 'Base de datos', 'Gestión de bases de datos MySQL'),
('React', 'Frontend', 'Desarrollo de interfaces web'),
('Python', 'Backend', 'Programación con Python'),
('Docker', 'DevOps', 'Contenedores y despliegue');

INSERT INTO etiquetas (nombre) VALUES
('Java'),
('Spring Boot'),
('MySQL'),
('Frontend'),
('Backend'),
('DevOps');

-- ============================================================
-- VISTAS SQL PRINCIPALES PARA EL AVANCE
-- ============================================================

-- Vista 1: perfil general del colaborador
CREATE VIEW vw_perfil_colaborador AS
SELECT
    c.id_colaborador,
    CONCAT(u.nombres, ' ', u.apellidos) AS colaborador,
    u.correo,
    u.telefono,
    c.cargo,
    c.area,
    c.seniority,
    c.disponibilidad_base,
    u.estado
FROM colaboradores c
JOIN usuarios u ON u.id_usuario = c.id_usuario;

-- Vista 2: habilidades de los colaboradores
CREATE VIEW vw_habilidades_colaborador AS
SELECT
    c.id_colaborador,
    CONCAT(u.nombres, ' ', u.apellidos) AS colaborador,
    h.nombre AS habilidad,
    h.categoria,
    ch.nivel,
    ch.anios_experiencia
FROM colaborador_habilidad ch
JOIN colaboradores c ON c.id_colaborador = ch.id_colaborador
JOIN usuarios u ON u.id_usuario = c.id_usuario
JOIN habilidades h ON h.id_habilidad = ch.id_habilidad;

-- Vista 3: catálogo/resumen de proyectos
CREATE VIEW vw_proyectos AS
SELECT
    p.id_proyecto,
    p.nombre AS proyecto,
    CONCAT(u.nombres, ' ', u.apellidos) AS project_manager,
    p.fecha_inicio,
    p.fecha_fin,
    p.vacantes,
    p.prioridad,
    p.estado,
    p.progreso
FROM proyectos p
JOIN usuarios u ON u.id_usuario = p.id_project_manager;

-- Vista 4: asignaciones y carga
CREATE VIEW vw_asignaciones AS
SELECT
    a.id_asignacion,
    CONCAT(u.nombres, ' ', u.apellidos) AS colaborador,
    p.nombre AS proyecto,
    a.rol_proyecto,
    a.fecha_inicio,
    a.fecha_fin,
    a.porcentaje_dedicacion,
    a.estado
FROM asignaciones a
JOIN colaboradores c ON c.id_colaborador = a.id_colaborador
JOIN usuarios u ON u.id_usuario = c.id_usuario
JOIN proyectos p ON p.id_proyecto = a.id_proyecto;

-- Vista 5: carga total de trabajo por colaborador
-- La disponibilidad se calcula desde disponibilidad_base y no desde 100 fijo.
CREATE VIEW vw_carga_colaboradores AS
SELECT
    c.id_colaborador,
    CONCAT(u.nombres, ' ', u.apellidos) AS colaborador,
    c.disponibilidad_base,
    COALESCE(SUM(
        CASE WHEN a.estado IN ('PLANIFICADA','ACTIVA')
             THEN a.porcentaje_dedicacion ELSE 0 END
    ), 0) AS carga_porcentaje,
    GREATEST(
        c.disponibilidad_base - COALESCE(SUM(
            CASE WHEN a.estado IN ('PLANIFICADA','ACTIVA')
                 THEN a.porcentaje_dedicacion ELSE 0 END
        ), 0),
        0
    ) AS disponibilidad_porcentaje
FROM colaboradores c
JOIN usuarios u ON u.id_usuario = c.id_usuario
LEFT JOIN asignaciones a ON a.id_colaborador = c.id_colaborador
GROUP BY c.id_colaborador, u.nombres, u.apellidos, c.disponibilidad_base;

-- Vista 6: Talent Matching
CREATE VIEW vw_talent_matching AS
SELECT
    r.id_recomendacion,
    p.nombre AS proyecto,
    CONCAT(u.nombres, ' ', u.apellidos) AS colaborador,
    r.porcentaje_match,
    r.justificacion,
    r.fecha_recomendacion
FROM recomendaciones_ia r
JOIN proyectos p ON p.id_proyecto = r.id_proyecto
JOIN colaboradores c ON c.id_colaborador = r.id_colaborador
JOIN usuarios u ON u.id_usuario = c.id_usuario;

-- Vista 7: foros
CREATE VIEW vw_foros AS
SELECT
    f.id_foro,
    f.titulo,
    p.nombre AS proyecto,
    CONCAT(u.nombres, ' ', u.apellidos) AS autor,
    f.categoria,
    f.estado,
    COUNT(r.id_respuesta) AS respuestas,
    f.fecha_creacion
FROM foros f
LEFT JOIN proyectos p ON p.id_proyecto = f.id_proyecto
JOIN usuarios u ON u.id_usuario = f.id_autor
LEFT JOIN respuestas_foro r ON r.id_foro = f.id_foro
GROUP BY
    f.id_foro, f.titulo, p.nombre,
    u.nombres, u.apellidos,
    f.categoria, f.estado, f.fecha_creacion;

-- Vista 8: administración de usuarios
CREATE VIEW vw_admin_usuarios AS
SELECT
    u.id_usuario,
    CONCAT(u.nombres, ' ', u.apellidos) AS usuario,
    u.correo,
    r.nombre AS rol,
    u.estado,
    u.ultimo_acceso,
    u.fecha_registro
FROM usuarios u
JOIN roles r ON r.id_rol = u.id_rol;

-- ============================================================
-- CONSULTAS DE EJEMPLO
-- ============================================================

-- SELECT * FROM vw_perfil_colaborador;
-- SELECT * FROM vw_habilidades_colaborador;
-- SELECT * FROM vw_proyectos;
-- SELECT * FROM vw_asignaciones;
-- SELECT * FROM vw_carga_colaboradores;
-- SELECT * FROM vw_talent_matching;
-- SELECT * FROM vw_foros;
-- SELECT * FROM vw_admin_usuarios;

-- Ver las 18 tablas:
-- SHOW TABLES;
