USE skillbridge_ai;

-- ============================================================
-- DATOS DEMO PARA PRESENTAR LAS VISTAS HTML
-- Ejecutar DESPUÉS de skillbridge_ai_18_tablas_corregida.sql
-- Puede volver a ejecutarse: primero limpia solamente los datos demo.
-- ============================================================

SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM mensajes_chat;
DELETE FROM notificaciones;
DELETE FROM auditoria;
DELETE FROM respuestas_foro;
DELETE FROM foro_etiqueta;
DELETE FROM foros;
DELETE FROM recomendaciones_ia;
DELETE FROM asignaciones;
DELETE FROM proyecto_habilidad;
DELETE FROM proyectos;
DELETE FROM certificaciones;
DELETE FROM colaborador_habilidad;
DELETE FROM colaboradores;
DELETE FROM usuarios;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO usuarios (id_rol,nombres,apellidos,correo,password_hash,telefono,estado,ultimo_acceso) VALUES
((SELECT id_rol FROM roles WHERE nombre='COLABORADOR'),'María','González','maria.gonzalez@skillbridge.com','demo','999111001','ACTIVO',NOW()),
((SELECT id_rol FROM roles WHERE nombre='COLABORADOR'),'Carlos','Ramírez','carlos.ramirez@skillbridge.com','demo','999111002','ACTIVO',NOW()),
((SELECT id_rol FROM roles WHERE nombre='COLABORADOR'),'Ana','Torres','ana.torres@skillbridge.com','demo','999111003','ACTIVO',NOW()),
((SELECT id_rol FROM roles WHERE nombre='COLABORADOR'),'Luis','Hernández','luis.hernandez@skillbridge.com','demo','999111004','ACTIVO',NOW()),
((SELECT id_rol FROM roles WHERE nombre='COLABORADOR'),'Sofía','Lima','sofia.lima@skillbridge.com','demo','999111005','ACTIVO',NOW()),
((SELECT id_rol FROM roles WHERE nombre='COLABORADOR'),'Diego','Morales','diego.morales@skillbridge.com','demo','999111006','ACTIVO',NOW()),
((SELECT id_rol FROM roles WHERE nombre='PROJECT_MANAGER'),'Paula','Silva','paula.silva@skillbridge.com','demo','999111007','ACTIVO',NOW()),
((SELECT id_rol FROM roles WHERE nombre='RESOURCE_MANAGER'),'Jorge','Medina','jorge.medina@skillbridge.com','demo','999111008','ACTIVO',NOW()),
((SELECT id_rol FROM roles WHERE nombre='ADMINISTRADOR'),'Administrador','SkillBridge','admin@skillbridge.com','demo',NULL,'ACTIVO',NOW());

INSERT INTO colaboradores (id_usuario,cargo,area,seniority,biografia,intereses_profesionales,disponibilidad_base) VALUES
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Desarrolladora Java','Desarrollo','SENIOR','Desarrolladora backend con experiencia en APIs y sistemas empresariales.','Backend, Cloud, DevOps',80),
((SELECT id_usuario FROM usuarios WHERE correo='carlos.ramirez@skillbridge.com'),'Desarrollador Java','Desarrollo','SEMI_SENIOR','Especialista en Java y Spring Boot.','Microservicios, Docker, AWS',100),
((SELECT id_usuario FROM usuarios WHERE correo='ana.torres@skillbridge.com'),'Desarrolladora Móvil','Desarrollo','SEMI_SENIOR','Desarrolladora enfocada en aplicaciones móviles.','Mobile, UX, APIs',90),
((SELECT id_usuario FROM usuarios WHERE correo='luis.hernandez@skillbridge.com'),'DevOps Engineer','Cloud','SENIOR','Ingeniero DevOps con experiencia en automatización y despliegue.','Docker, CI/CD, Cloud',70),
((SELECT id_usuario FROM usuarios WHERE correo='sofia.lima@skillbridge.com'),'Desarrolladora Java','Desarrollo','JUNIOR','Desarrolladora backend en crecimiento.','Java, MySQL, Spring Boot',100),
((SELECT id_usuario FROM usuarios WHERE correo='diego.morales@skillbridge.com'),'QA Engineer','Calidad','SEMI_SENIOR','Ingeniero de calidad con experiencia en pruebas funcionales.','QA, APIs, Automatización',80),
((SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'Project Manager','Gestión','LEAD','Project Manager de equipos de desarrollo.','Gestión, Agile, Arquitectura',60),
((SELECT id_usuario FROM usuarios WHERE correo='jorge.medina@skillbridge.com'),'Resource Manager','Gestión Talento','LEAD','Gestiona capacidad, asignaciones y disponibilidad del equipo.','Talento, Capacity Planning, Gestión',70);

INSERT INTO colaborador_habilidad (id_colaborador,id_habilidad,nivel,anios_experiencia) VALUES
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='maria.gonzalez@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='Java'),'AVANZADO',4.0),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='maria.gonzalez@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='Spring Boot'),'AVANZADO',3.5),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='maria.gonzalez@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='MySQL'),'INTERMEDIO',3.0),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='carlos.ramirez@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='Java'),'AVANZADO',4.0),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='carlos.ramirez@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='Docker'),'INTERMEDIO',2.0),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='luis.hernandez@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='Docker'),'EXPERTO',5.0),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='sofia.lima@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='Spring Boot'),'INTERMEDIO',1.5),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='sofia.lima@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='MySQL'),'INTERMEDIO',1.5),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='ana.torres@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='React'),'INTERMEDIO',2.5),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='diego.morales@skillbridge.com'),(SELECT id_habilidad FROM habilidades WHERE nombre='Python'),'INTERMEDIO',2.0);

INSERT INTO certificaciones (id_colaborador,nombre,entidad_emisora,fecha_emision,fecha_expiracion,url_credencial,estado) VALUES
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='maria.gonzalez@skillbridge.com'),'AWS Certified Developer - Associate','Amazon Web Services','2024-03-10','2027-03-10','https://example.com/aws','VIGENTE'),
((SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='maria.gonzalez@skillbridge.com'),'Docker Certified Associate','Docker','2023-11-01',NULL,'https://example.com/docker','SIN_EXPIRACION');

INSERT INTO proyectos (id_project_manager,nombre,descripcion,fecha_inicio,fecha_fin,vacantes,prioridad,estado,progreso) VALUES
((SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'Plataforma Web','Desarrollo de nueva plataforma web corporativa.','2026-08-19','2026-11-30',3,'ALTA','ACTIVE',65),
((SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'App Móvil Clientes','Aplicación móvil para seguimiento de solicitudes.','2026-09-01','2026-12-05',2,'MEDIA','ACTIVE',40),
((SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'Migración a la Nube','Migración de infraestructura y servicios a AWS.','2026-07-15','2026-10-31',1,'ALTA','ACTIVE',82),
((SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'Integración API','Integración de servicios internos y APIs externas.','2026-09-10','2026-12-10',2,'MEDIA','PLANNING',15),
((SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'Dashboard Analítico','Panel de indicadores de proyectos y talento.','2026-06-01','2026-08-30',0,'BAJA','COMPLETED',100);

INSERT INTO proyecto_habilidad (id_proyecto,id_habilidad,nivel_requerido,vacantes) VALUES
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_habilidad FROM habilidades WHERE nombre='Java'),'AVANZADO',2),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_habilidad FROM habilidades WHERE nombre='Spring Boot'),'AVANZADO',2),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_habilidad FROM habilidades WHERE nombre='MySQL'),'INTERMEDIO',1),
((SELECT id_proyecto FROM proyectos WHERE nombre='Migración a la Nube'),(SELECT id_habilidad FROM habilidades WHERE nombre='Docker'),'AVANZADO',1);

INSERT INTO asignaciones (id_proyecto,id_colaborador,rol_proyecto,fecha_inicio,fecha_fin,porcentaje_dedicacion,estado) VALUES
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='maria.gonzalez@skillbridge.com'),'Desarrolladora Backend','2026-08-19','2026-11-30',40,'ACTIVA'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='carlos.ramirez@skillbridge.com'),'Desarrollador Backend','2026-08-19','2026-11-30',60,'ACTIVA'),
((SELECT id_proyecto FROM proyectos WHERE nombre='App Móvil Clientes'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='ana.torres@skillbridge.com'),'Desarrolladora Mobile','2026-09-01','2026-12-05',70,'ACTIVA'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Migración a la Nube'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='luis.hernandez@skillbridge.com'),'DevOps Engineer','2026-07-15','2026-10-31',60,'ACTIVA'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Integración API'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='sofia.lima@skillbridge.com'),'Desarrolladora Backend','2026-09-15','2026-12-10',30,'PLANIFICADA'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='diego.morales@skillbridge.com'),'QA Engineer','2026-09-01','2026-11-30',30,'ACTIVA');

INSERT INTO recomendaciones_ia (id_proyecto,id_colaborador,porcentaje_match,justificacion,recomendado_por) VALUES
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='carlos.ramirez@skillbridge.com'),95.00,'Coincide con Java y Spring Boot; experiencia backend y disponibilidad compatible.',(SELECT id_usuario FROM usuarios WHERE correo='jorge.medina@skillbridge.com')),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='maria.gonzalez@skillbridge.com'),89.00,'Perfil senior con experiencia directa en tecnologías del proyecto.',(SELECT id_usuario FROM usuarios WHERE correo='jorge.medina@skillbridge.com')),
((SELECT id_proyecto FROM proyectos WHERE nombre='Migración a la Nube'),(SELECT id_colaborador FROM colaboradores c JOIN usuarios u ON u.id_usuario=c.id_usuario WHERE u.correo='luis.hernandez@skillbridge.com'),92.00,'Nivel experto en Docker y experiencia en automatización de despliegues.',(SELECT id_usuario FROM usuarios WHERE correo='jorge.medina@skillbridge.com'));

INSERT INTO foros (id_proyecto,id_autor,titulo,contenido,categoria,estado) VALUES
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Mejores prácticas para autenticación JWT','Necesitamos definir la estrategia de autenticación para el módulo web. ¿Qué recomendaciones tienen?','Backend','ABIERTO'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Integración API'),(SELECT id_usuario FROM usuarios WHERE correo='carlos.ramirez@skillbridge.com'),'Error de conexión con API externa','El servicio responde con timeout en pruebas. Comparto el caso para revisar posibles causas.','API e Integraciones','ABIERTO'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_usuario FROM usuarios WHERE correo='diego.morales@skillbridge.com'),'Pruebas de integración','Propongo una lista inicial de escenarios para la integración de módulos.','Testing / QA','RESUELTO');

INSERT INTO foros (id_proyecto,id_autor,titulo,contenido,categoria,estado) VALUES
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_usuario FROM usuarios WHERE correo='ana.torres@skillbridge.com'),'Organización de componentes reutilizables','Propongo centralizar los componentes compartidos para reducir duplicidad en las vistas.','Frontend','ABIERTO'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Migración a la Nube'),(SELECT id_usuario FROM usuarios WHERE correo='luis.hernandez@skillbridge.com'),'Estrategia para despliegues repetibles','Documentemos un flujo de despliegue que pueda repetirse sin depender de pasos manuales.','DevOps / Cloud','ABIERTO'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Integración API'),(SELECT id_usuario FROM usuarios WHERE correo='sofia.lima@skillbridge.com'),'Convenciones para consultas SQL','Quiero acordar nombres y criterios para las consultas compartidas del proyecto.','Base de Datos','CERRADO'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_usuario FROM usuarios WHERE correo='carlos.ramirez@skillbridge.com'),'Separación de responsabilidades en controladores','Revisemos cómo mantener los controladores simples siguiendo lo visto en clase.','Arquitectura','ABIERTO'),
((SELECT id_proyecto FROM proyectos WHERE nombre='App Móvil Clientes'),(SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'Acuerdos para la revisión semanal','Dejo los acuerdos de coordinación y seguimiento para el equipo del proyecto.','Gestión de Proyecto','RESUELTO');

INSERT INTO respuestas_foro (id_foro,id_autor,contenido,es_solucion) VALUES
((SELECT id_foro FROM foros WHERE titulo='Mejores prácticas para autenticación JWT'),(SELECT id_usuario FROM usuarios WHERE correo='carlos.ramirez@skillbridge.com'),'Podemos separar autenticación y autorización y documentar primero los endpoints.',FALSE),
((SELECT id_foro FROM foros WHERE titulo='Error de conexión con API externa'),(SELECT id_usuario FROM usuarios WHERE correo='luis.hernandez@skillbridge.com'),'Revisaría timeout, DNS y reglas de red antes de modificar el código.',TRUE);

INSERT INTO foro_etiqueta (id_foro,id_etiqueta) VALUES
((SELECT id_foro FROM foros WHERE titulo='Mejores prácticas para autenticación JWT'),(SELECT id_etiqueta FROM etiquetas WHERE nombre='Backend')),
((SELECT id_foro FROM foros WHERE titulo='Error de conexión con API externa'),(SELECT id_etiqueta FROM etiquetas WHERE nombre='DevOps'));

INSERT INTO notificaciones (id_usuario,titulo,mensaje,tipo,leida) VALUES
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Nueva asignación','Fuiste asignada al proyecto Plataforma Web.','ASIGNACION',FALSE),
((SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'Proyecto actualizado','Se actualizó el progreso de Plataforma Web.','PROYECTO',FALSE);

INSERT INTO auditoria (id_usuario,accion,modulo,detalle) VALUES
((SELECT id_usuario FROM usuarios WHERE correo='admin@skillbridge.com'),'Crear usuario','Administración','Se registró un nuevo usuario colaborador.'),
((SELECT id_usuario FROM usuarios WHERE correo='paula.silva@skillbridge.com'),'Crear proyecto','Proyectos','Se creó el proyecto Integración API.'),
((SELECT id_usuario FROM usuarios WHERE correo='jorge.medina@skillbridge.com'),'Consultar carga','Asignaciones','Se revisó la carga actual de colaboradores.');

-- Conversaciones de ejemplo para completar la vista de Chat
INSERT INTO mensajes_chat (id_proyecto,id_usuario,contenido) VALUES
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_usuario FROM usuarios WHERE correo='carlos.ramirez@skillbridge.com'),'Hola equipo, ya dejé lista la estructura inicial del módulo backend.'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Perfecto. Yo revisaré la integración con la base de datos y los endpoints pendientes.'),
((SELECT id_proyecto FROM proyectos WHERE nombre='Plataforma Web'),(SELECT id_usuario FROM usuarios WHERE correo='diego.morales@skillbridge.com'),'Cuando terminen esa parte preparo los casos de prueba de integración.');

INSERT INTO notificaciones (id_usuario,titulo,mensaje,tipo,leida) VALUES
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Nuevo mensaje de proyecto','Carlos dejó una actualización en el chat de Plataforma Web.','CHAT',FALSE),
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Foro actualizado','Hay una nueva respuesta en Mejores prácticas para autenticación JWT.','FORO',TRUE);


-- Notificaciones adicionales para demostrar la paginación del colaborador
INSERT INTO notificaciones (id_usuario,titulo,mensaje,tipo,leida) VALUES
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Revisión de perfil','Tu perfil profesional fue actualizado correctamente.','INFO',TRUE),
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Avance de proyecto','Plataforma Web alcanzó un nuevo porcentaje de progreso.','PROYECTO',TRUE),
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Nueva conversación','Se registró actividad reciente en el chat de Plataforma Web.','CHAT',FALSE),
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Tema resuelto','El tema Pruebas de integración fue marcado como resuelto.','FORO',TRUE),
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Asignación vigente','Tu asignación en Plataforma Web continúa activa.','ASIGNACION',TRUE),
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Actualización del foro','Hay nuevos temas disponibles en el foro de conocimiento.','FORO',FALSE),
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Recordatorio de proyecto','Revisa el progreso y las fechas de tu proyecto actual.','PROYECTO',TRUE),
((SELECT id_usuario FROM usuarios WHERE correo='maria.gonzalez@skillbridge.com'),'Mensaje del equipo','Tienes actividad pendiente de revisar en el chat del proyecto.','CHAT',FALSE);

SET SQL_SAFE_UPDATES = 1;
