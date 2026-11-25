# SkillBridge AI — Avance HTML 100% / CRUD aproximado 60%

## Cambios de identidad y experiencia
- Se conserva **Explorar la plataforma**.
- Se retiró el ingreso superior redundante.
- Se eliminaron indicadores decorativos de la portada como “Operativo” y porcentajes de match sin contexto.
- La portada muestra el ecosistema SkillBridge y módulos visuales desplegables.
- El login ya no muestra una única persona por rol: primero se selecciona el rol y luego se ingresan credenciales.
- La cuenta se valida contra `usuarios` y se comprueba el rol real.
- La cabecera muestra el usuario conectado y su rol.
- El sidebar cambia completamente de color según rol: violeta, azul, teal o navy/dorado.
- El Foro de conocimiento mantiene categorías, filtros, publicaciones, respuestas y solución marcada.

## CRUD funcional aproximado al 60%
- Usuarios: C/R/U + activación/desactivación.
- Habilidades: C/R/U + activación/desactivación.
- Proyectos: C/R/U + cancelación lógica + habilidades requeridas.
- Asignaciones: C/R/U + finalizar/cancelar.
- Certificaciones: C/R/U/D.
- Habilidades de colaborador: C/R/U/D.
- Foros: crear, consultar, editar, cerrar/reabrir y eliminar.
- Respuestas: crear, consultar, marcar solución y eliminar.
- Notificaciones: consultar y marcar como leída.
- Chat: crear y consultar mensajes.

## Pendiente para siguientes avances
- Roles y permisos editables.
- Talent Matching IA real.
- Reportes avanzados.
- Auditoría automática completa.
- WebSockets.
- Spring Security y BCrypt.
