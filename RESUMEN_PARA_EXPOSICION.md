# Repaso rápido: código de SkillBridge para GTICS

## Lo anterior (Entregable 7)

| Si pregunta por... | Archivo | Qué responder |
|---|---|---|
| Arranque Spring Boot | `SkillbridgeAiApplication.java` | Inicia la aplicación y sus componentes. |
| Dependencias | `pom.xml` | Maven gestiona librerías. |
| Entidad JPA | `entity/Proyecto.java` / `entity/Usuario.java` | `@Entity`, `@Id`, `@ManyToOne`: representa tablas y relaciones. |
| Acceso a MySQL | `repository/UsuarioRepository.java` | `JpaRepository` expone `findAll`, `findById`, `save`. |
| Seguridad web | `config/SecurityConfig.java` | Login con formulario, BCrypt, restricciones por rol. |
| Usuario autenticado | `service/GestionUsuarioService.java` | Carga correo, contraseña hash y autoridad `ROLE_...`. |
| Sesión y redirección | `controller/LoginSuccessHandler.java` | Guarda ID/rol en sesión y redirige según perfil. |
| Administrador CRUD | `controller/AdminController.java` | Usuarios, habilidades, roles, auditoría. |
| PM | `controller/ProjectManagerController.java` | Proyectos, asignaciones, habilidades, reportes. |
| Resource Manager | `controller/ResourceManagerController.java` | Carga, disponibilidad, talento y reportes. |
| Colaborador | `controller/ColaboradorController.java` | Perfil, certificaciones, habilidades, notificaciones, chat. |
| Foros | `controller/ForoController.java` | Publicaciones y respuestas. |
| Reportes | `AdminController.java`, `ProjectManagerController.java`, `ResourceManagerController.java` | Indicadores, agrupaciones y exportación CSV con `ResponseEntity`. |
| Chat | `ColaboradorController.java` + `MensajeChat.java` | Mensajes guardados por proyecto; **no chatbot ni AJAX**. |

## Lo agregado (API DNI + Postman)

| Si pregunta por... | Archivo | Qué responder |
|---|---|---|
| REST / JSON | `IdentidadRestController.java` | `@RestController`: devuelve JSON, no HTML. |
| Validar parámetros | `DniConsultaRequest.java` | `@NotBlank`, `@Pattern`, `@Valid`: ocho dígitos. |
| Transporte de datos | `Dni*Response.java` | DTO: pasa datos sin usar una entidad JPA. |
| Consumo externo | `ConsultaDniService.java` | `RestTemplateBuilder` + `exchange`, HTTP POST y Bearer token a VerificaID. |
| Seguridad API | `ApiDniSecurityConfig.java` | HTTP Basic + STATELESS; solo ADMINISTRADOR / RESOURCE_MANAGER. |
| Probar servicio | `postman/*.json` | GET estado y POST consultar/validar, pruebas de error. |

## Las rutas nuevas

- `GET /api/identidad/estado` → estado API.
- `POST /api/identidad/consultar` → consulta un DNI en VerificaID.
- `POST /api/identidad/validar` → compara nombres y apellidos.

## Cinco diferencias fundamentales

1. **MVC + Thymeleaf**: devuelve HTML, mientras que **REST** devuelve JSON.
2. **`@Controller`**: pantallas; **`@RestController`**: datos de una API.
3. **Sesión web**: navegación de usuario; **Basic STATELESS**: cada llamada API autentica por separado.
4. **Basic Auth**: Postman → SkillBridge; **Bearer**: SkillBridge → VerificaID.
5. **Postman**: herramienta para probar APIs; no es una parte de la interfaz de usuario final.

## Estado que hay que comunicar sin exagerar

- Conservado: CRUD, dashboard, reportes, chat, roles, Spring Security web.
- Añadido: API DNI, protección HTTP Basic, DTO, consumo de VerificaID y Postman.
- Por probar: compilación Maven, MySQL, token sandbox y respuesta externa real.
- Aún no se añadió: chatbot, chat AJAX, Docker, API directa oficial de RENIEC.
