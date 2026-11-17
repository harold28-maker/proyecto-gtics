# SKILLBRIDGE AI – GUÍA COMPLETA PARA EXPLICAR EL PROYECTO (GTICS)

**Incluye el código anterior del Entregable 7 y la ampliación API DNI + Postman.**

Esta guía está basada en los archivos Java y vistas reales de `proyecto-gtics-entregable7.2.zip`, las ocho clases de `ENTREGABLECONPOSTMAN.zip` y los ejemplos de las PPT de la profesora (clases 9.1, 9.2 y 10.1). No supone que hayan implementado todavía AJAX, chatbot o Docker.

> **Importante:** La consulta de identidad se realiza a través de **VerificaID** (proveedor externo explicado en la PPT), **no** mediante una API oficial abierta de RENIEC. El código está integrado, pero requiere token válido, MySQL y pruebas de ejecución para demostrar una consulta real.

---

## 1. ¿Qué es SkillBridge AI? ¿Qué está programado?

Es una aplicación web para gestionar usuarios, roles, colaboradores, habilidades, proyectos, asignaciones, foros, notificaciones, certificaciones, chat y reportes. Utiliza **Spring Boot (Java)**, **Spring MVC**, **Thymeleaf**, **Spring Data JPA**, **MySQL** y **Spring Security**.

**Antes (Entregable 7):** aplicación web para personas que devuelve páginas HTML.

**Ahora (complemento):** además hay un **Web Service REST propio** (`/api/identidad/**`) que devuelve **JSON**, consume un servicio externo de identidad y se prueba desde **Postman**.

### Flujo web que ya tenían

1. Una persona abre una pantalla en su navegador.
2. Spring Security comprueba si inició sesión y tiene el rol correcto.
3. Un controlador `@Controller` recibe la petición GET o POST.
4. El controlador consulta un `Repository` (Spring Data JPA); este lee o escribe en MySQL.
5. El controlador agrega datos al `Model` y devuelve una vista Thymeleaf (`.html`).

### Flujo nuevo de DNI

1. Postman envía un JSON por HTTP POST a `/api/identidad/consultar`.
2. Spring Security exige usuario de prueba con rol ADMINISTRADOR o RESOURCE_MANAGER mediante HTTP Basic.
3. `IdentidadRestController` recibe el JSON (`@RequestBody`) y valida el DNI (`@Valid`).
4. `ConsultaDniService` usa `RestTemplateBuilder` / `RestTemplate` para enviar un POST multipart con Bearer token a VerificaID.
5. Si el proveedor devuelve JSON válido, el servicio extrae DNI, nombres y apellidos.
6. El `@RestController` devuelve un DTO convertido a JSON. **No modifica las tablas.**

---

## 2. Estructura del proyecto: clases y responsabilidad

| Carpeta / clase | Para qué sirve |
|---|---|
| `pom.xml` | Define el proyecto Maven y las dependencias de Spring Boot. |
| `SkillbridgeAiApplication.java` | Punto de entrada de la aplicación, arranca Spring Boot. |
| `config/SecurityConfig.java` | Seguridad **original** del login web: roles, rutas, contraseña BCrypt, logout. |
| `service/GestionUsuarioService.java` | Busca el usuario por correo y le asigna sus autoridades (`ROLE_...`). |
| `controller/LoginSuccessHandler.java` | Tras iniciar sesión, guarda datos de sesión y redirige según el rol. |
| `entity/` | Entidades JPA que representan registros de tablas MySQL. |
| `repository/` | Interfaces `JpaRepository` que consultan/guardan entidades. |
| `controller/AdminController.java` | Administración de usuarios, roles, habilidades, auditoría y reportes. |
| `controller/ProjectManagerController.java` | Proyectos, asignaciones, habilidades por proyecto, foros, reportes del PM. |
| `controller/ResourceManagerController.java` | Colaboradores, asignaciones, carga, disponibilidad, talento y reportes. |
| `controller/ColaboradorController.java` | Perfil, certificados, habilidades, proyectos, chat y notificaciones. |
| `controller/ForoController.java` | Publicaciones y respuestas de foros. |
| `templates/` | Vistas HTML Thymeleaf para cada rol. |
| `static/css/` | Estilos, diseño y logo. |
| `resources/database/` | Scripts SQL originales del proyecto. |
| `config/ApiDniSecurityConfig.java` | **Nuevo:** seguridad independiente para la ruta `/api/identidad/**`. |
| `controller/IdentidadRestController.java` | **Nuevo:** endpoints REST que reciben/devuelven JSON. |
| `service/ConsultaDniService.java` | **Nuevo:** consume VerificaID y compara datos. |
| `dto/Dni*` | **Nuevos:** objetos que estructuran y validan los datos JSON. |
| `postman/*.json` | **Nuevo:** colección de solicitudes para probar el Web Service. |

---

## 3. CÓDIGO ANTERIOR: cómo arranca Spring Boot y qué hace Maven

**Archivo:** `src/main/java/pe/edu/pucp/skillbridge/SkillbridgeAiApplication.java`.

La clase principal se encarga de iniciar Spring Boot. `@SpringBootApplication` permite realizar configuración automática y descubrir componentes (`@Controller`, `@Service`, `@Repository`).

**Archivo:** `pom.xml`.

- `spring-boot-starter-web`: controladores web y HTTP.
- `spring-boot-starter-thymeleaf`: plantillas HTML.
- `spring-boot-starter-security`: autenticación y autorización.
- `spring-boot-starter-validation`: anotaciones como `@NotBlank` y `@Valid`.
- `spring-boot-starter-data-jpa`: persistencia mediante repositorios y entidades.
- `mysql-connector-j`: conexión JDBC a MySQL.
- **Nueva dependencia:** `spring-boot-starter-restclient` para `RestTemplateBuilder` en Spring Boot 4.

**Si preguntan:** «¿Para qué sirve Maven?» — «Administra las dependencias, versiones y compilación del proyecto mediante `pom.xml`.»

---

## 4. CÓDIGO ANTERIOR: entidades y base de datos (JPA)

**Ejemplo real: `entity/Proyecto.java`:**

```java
@Entity
@Table(name = "proyectos")
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proyecto")
    private Integer idProyecto;

    @ManyToOne
    @JoinColumn(name = "id_project_manager", nullable = false)
    private Usuario projectManager;
}
```

**Qué hace cada anotación:**

- `@Entity`: la clase se mapea con una tabla de la base de datos.
- `@Table(name="proyectos")`: indica la tabla correspondiente.
- `@Id`: identifica la clave primaria.
- `@GeneratedValue(...)`: el identificador se genera en la BD.
- `@ManyToOne`: varios proyectos pueden estar asociados a un Project Manager.
- `@JoinColumn`: define la columna de clave foránea.
- `@Column`: mapea una propiedad a una columna.

**Validaciones reales de `Proyecto`:** `@NotBlank` en el nombre, `@Size` en longitud, `@Min(0)` y `@Max(100)` para el progreso, y `@AssertTrue` para comprobar que la fecha fin no sea anterior a la de inicio.

**Si preguntan:** «¿JPA es MySQL?» — «No. JPA es la especificación de persistencia Java; en nuestro proyecto Hibernate la implementa para trabajar con las tablas de MySQL.»

---

## 5. CÓDIGO ANTERIOR: Repository y consulta a la base de datos

**Ejemplo real: `repository/UsuarioRepository.java`:**

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    List<Usuario> findByRolNombre(String nombre);
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
```

- `JpaRepository<Usuario, Integer>` proporciona operaciones como `findAll()`, `findById()`, `save()` y `deleteById()`.
- `findByCorreoIgnoreCase`: busca un usuario por correo sin distinguir mayúsculas y minúsculas.
- `Optional<Usuario>`: expresa que puede o no encontrarse un usuario.
- En la mayoría de los casos no tienen que escribir manualmente SQL para estas operaciones: Spring Data JPA genera las consultas.

**Si preguntan:** «¿Cuál es la diferencia entre Entity y Repository?» — «La Entity representa los datos de una tabla; el Repository es la interfaz que consulta y guarda esos datos.»

---

## 6. CÓDIGO ANTERIOR: Spring Security, roles y login

**Archivo:** `config/SecurityConfig.java`.

```java
.requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
.requestMatchers("/pm/**").hasRole("PROJECT_MANAGER")
.requestMatchers("/resource/**").hasRole("RESOURCE_MANAGER")
.requestMatchers("/colaborador/**").hasRole("COLABORADOR")
.requestMatchers("/foros/**").hasAnyRole("COLABORADOR", "PROJECT_MANAGER")
```

- `requestMatchers`: selecciona las URL a proteger.
- `hasRole`: deja pasar solo a usuarios con ese rol.
- `formLogin`: configura el formulario web de inicio de sesión.
- `loginProcessingUrl("/acceder")`: recibe los datos del formulario.
- `usernameParameter("correo")`: utiliza el correo como usuario.
- `passwordParameter("password")`: recibe la contraseña.
- `BCryptPasswordEncoder`: permite verificar contraseñas guardadas como hashes BCrypt.
- `logout`: invalida la sesión y redirige al login.

**Archivo:** `service/GestionUsuarioService.java`.

```java
@Override
@Transactional(readOnly = true)
public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
    Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim())
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    // Luego construye UserDetails con passwordHash y ROLE_<rol>.
}
```

Spring Security llama a este método para buscar al usuario en MySQL y validar el login.

**Archivo:** `controller/LoginSuccessHandler.java`.

```java
HttpSession session = request.getSession();
session.setAttribute("usuarioSesionId", usuario.getIdUsuario());
session.setAttribute("rolVista", rolVista(authentication));
response.sendRedirect(inicioSegunRol(authentication));
```

- `HttpSession`: permite mantener información del usuario durante la navegación.
- Al autenticarlo, guarda ID y rol para las vistas.
- Redirige según el rol: administrador, PM, Resource Manager o colaborador.

**Pregunta típica:** «¿Qué diferencia hay entre autenticación y autorización?» — «Autenticación es comprobar quién inicia sesión; autorización es comprobar a qué rutas y funciones puede acceder según su rol.»

---

## 7. CÓDIGO ANTERIOR: controladores web y CRUD

### Administrador — `AdminController.java`

Rutas reales:

- `GET /admin/usuarios`: lista usuarios.
- `GET /admin/usuarios/nuevo`: muestra formulario de registro.
- `GET /admin/usuarios/editar/{id}`: muestra datos existentes para editar.
- `POST /admin/usuarios/guardar`: crea o actualiza un usuario.
- `POST /admin/usuarios/{id}/estado`: cambia el estado.
- `GET /admin/roles`: muestra roles.
- `GET /admin/habilidades`: lista habilidades.
- `POST /admin/habilidades/guardar`: guarda una habilidad.
- `GET /admin/auditoria`: muestra eventos de auditoría.

**Qué hacen las anotaciones:**

- `@Controller`: devuelve una vista HTML mediante Thymeleaf.
- `@RequestMapping("/admin")`: prefijo de rutas.
- `@GetMapping`: atiende solicitudes GET, normalmente lectura/pantallas.
- `@PostMapping`: atiende formularios que envían datos al servidor.
- `@PathVariable`: lee un valor que viene en la ruta (`{id}`).
- `@RequestParam`: lee un parámetro de la URL o un formulario.
- `@ModelAttribute`: convierte valores de un formulario a un objeto Java.
- `Model.addAttribute(...)`: pasa variables desde Java a la plantilla HTML.

**Si preguntan «¿Qué es CRUD?»** — «Create, Read, Update, Delete: crear, consultar, modificar y eliminar registros. En algunos módulos el proyecto usa cambios de estado en vez de borrado físico.»

### Project Manager — `ProjectManagerController.java`

Rutas reales:

- `GET /pm/proyectos`, `GET /pm/proyectos/nuevo`, `POST /pm/proyectos/guardar`.
- `GET /pm/proyectos/{id}/habilidades`, `POST /pm/proyectos/{id}/habilidades/guardar`.
- `GET /pm/asignaciones`, `POST /pm/asignaciones/guardar`.
- `GET /pm/recomendaciones`, `/pm/disponibilidad`, `/pm/foros`, `/pm/reportes`.

**Explicación:** el PM gestiona proyectos, asigna colaboradores y consulta las habilidades asociadas al trabajo. Los datos son consultados por repositorios y enviados a Thymeleaf.

### Resource Manager — `ResourceManagerController.java`

Rutas reales: `/resource/colaboradores`, `/resource/asignaciones`, `/resource/carga`, `/resource/disponibilidad`, `/resource/talent`, `/resource/reportes`.

**Explicación:** muestra información sobre colaboradores, asignaciones, carga y disponibilidad; puede actualizar asignaciones y generar reportes.

### Colaborador — `ColaboradorController.java`

Rutas reales: `/colaborador/perfil`, `/colaborador/certificaciones`, `/colaborador/habilidades`, `/colaborador/proyectos`, `/colaborador/asignaciones`, `/colaborador/notificaciones`, `/colaborador/chat`.

**Explicación:** permite gestionar el perfil y las credenciales, ver proyectos y asignaciones, recibir notificaciones y usar el chat de proyectos.

### Foros — `ForoController.java`

- `GET /foros/{id}`: detalle de una publicación.
- `GET /foros/nuevo`: formulario de nueva publicación.
- `POST /foros/guardar`: guarda foro/publicación.
- El módulo maneja categorías y respuestas almacenadas en BD.

---

## 8. CÓDIGO ANTERIOR: dashboard, reportes y exportación CSV

**Archivo:** `AdminController.java`.

```java
List<Usuario> usuarios = usuarioRepository.findAll();
List<Habilidad> habilidades = habilidadRepository.findAll();
model.addAttribute("totalUsuarios", usuarios.size());
```

El dashboard se alimenta de la información de MySQL, calcula indicadores y los pasa a la vista HTML. En el módulo de reportes también agrupa usuarios por rol y habilidades por categoría utilizando `Collectors.groupingBy`.

**Exportación real:**

```java
@GetMapping(value = "/reportes/exportar", produces = "text/csv")
public ResponseEntity<String> exportarReporte(@RequestParam("tipo") String tipo,
                                               HttpSession session) {
    // Construye CSV de usuarios, habilidades o auditoría.
}
```

- `ResponseEntity<String>`: permite personalizar cuerpo, código y cabeceras HTTP.
- `produces="text/csv"`: la respuesta es un CSV, no una página HTML.
- `Content-Disposition: attachment`: indica que el navegador debe descargar un archivo.
- Los PM y Resource Manager también tienen reportes/exportación CSV en sus controladores.

**Pregunta típica:** «¿El dashboard es un Web Service REST?» — «No. El dashboard que ya teníamos usa Spring MVC y devuelve una vista Thymeleaf. La API DNI que acabamos de añadir sí devuelve JSON desde un `@RestController`.»

---

## 9. CÓDIGO ANTERIOR: chat entre colaboradores

**Archivo:** `ColaboradorController.java`.

```java
@GetMapping("/chat")
public String chat(@RequestParam(value = "idProyecto", required = false)
                   Integer idProyecto, Model model) {
    cargarChat(model, colaboradorDemo(), idProyecto);
    return "colaborador/chat";
}

@PostMapping("/chat/enviar")
public String enviarMensaje(@RequestParam("idProyecto") Integer idProyecto,
                            @RequestParam("contenido") String contenido,
                            Model model) {
    // Comprueba la asignación, valida mensaje y guarda en la base.
}
```

**Lógica importante:**

1. Obtiene los proyectos a los que está asignado el colaborador.
2. No permite enviar mensajes a proyectos ajenos.
3. Comprueba que el mensaje no esté vacío y que no supere el máximo configurado.
4. Guarda mensajes mediante `MensajeChatRepository`.
5. Obtiene los mensajes del proyecto por fecha para mostrar el historial.

**Entidad:** `MensajeChat.java` contiene relaciones `@ManyToOne` con `Proyecto` y `Usuario`, además de contenido y fecha de envío.

**Importante decirlo correctamente:** es **chat entre usuarios**, **no chatbot con respuestas automáticas**. El envío actual es un formulario POST normal: **todavía no es AJAX**.

---

## 10. CÓDIGO NUEVO: `IdentidadRestController.java`

```java
@RestController
@RequestMapping(path = "/api/identidad", produces = MediaType.APPLICATION_JSON_VALUE)
public class IdentidadRestController {

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("servicio", "identidad", "estado", "ok");
    }

    @PostMapping(path = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DniConsultaResponse consultar(@Valid @RequestBody DniConsultaRequest request) {
        return consultaDniService.consultar(request.dni());
    }

    @PostMapping(path = "/validar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DniValidacionResponse validar(@Valid @RequestBody DniValidacionRequest request) {
        return consultaDniService.validar(request);
    }
}
```

*Este fragmento concentra los métodos para estudiar; en el archivo real también está el constructor y el atributo del servicio.*

- `@RestController`: devuelve datos, no vistas.
- `@RequestMapping`: define la ruta base `/api/identidad`.
- `produces=application/json`: indica el formato de la respuesta.
- `consumes=application/json`: el cuerpo enviado debe ser JSON.
- `@Valid`: ejecuta restricciones del DTO.
- `@RequestBody`: convierte JSON de Postman en objeto Java.
- El controlador delega la consulta al `ConsultaDniService` en lugar de hacer toda la lógica allí.

**Las tres rutas:**

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/identidad/estado` | Confirmar que el servicio REST responde (con autenticación). |
| POST | `/api/identidad/consultar` | Consultar un DNI en el proveedor externo. |
| POST | `/api/identidad/validar` | Comparar nombres/apellidos declarados con los devueltos. |

**¿Por qué POST en la consulta?** Porque se envían datos personales en el cuerpo JSON y además el proveedor externo documenta una consulta POST. Esto no garantiza confidencialidad por sí solo: también se requiere HTTPS y controles de acceso.

---

## 11. CÓDIGO NUEVO: DTO y validaciones

**Archivo:** `dto/DniConsultaRequest.java`.

```java
public record DniConsultaRequest(
    @NotBlank(message = "Ingresa un DNI")
    @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener exactamente 8 digitos")
    String dni
) {}
```

- `record`: estructura compacta para transportar datos sin escribir getters manuales.
- `@NotBlank`: rechaza valores vacíos.
- `@Pattern`: exige exactamente ocho dígitos numéricos.
- **Formato válido no significa DNI existente.** Son comprobaciones distintas.

**Otros DTO:**

- `DniConsultaResponse`: DNI, nombres, apellido paterno y materno.
- `DniValidacionRequest`: DNI y nombres/apellidos que desea comparar.
- `DniValidacionResponse`: `coincide` (booleano) y `mensaje`.

**¿Qué es DTO?** — «Data Transfer Object: estructura para recibir o enviar datos entre las partes de una aplicación sin exponer directamente entidades JPA.»

**¿Qué es JSON?** — «JavaScript Object Notation: formato ligero de intercambio de datos organizado en pares clave-valor.»

Ejemplo que envía Postman:

```json
{"dni":"00000000"}
```

Ejemplo de **estructura ilustrativa** de respuesta exitosa (no es una consulta real):

```json
{"dni":"00000000","nombres":"NOMBRES EJEMPLO","apellidoPaterno":"PATERNO","apellidoMaterno":"MATERNO"}
```

---

## 12. CÓDIGO NUEVO: `ConsultaDniService.java` (lo más preguntable)

```java
@Service
public class ConsultaDniService {
    private final RestTemplate restTemplate;
    private final String token;
    private final String apiUrl;

    public ConsultaDniService(RestTemplateBuilder builder, /* token y URL */) {
        this.restTemplate = builder
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(12))
            .build();
        // Asigna token y apiUrl desde la configuración.
    }
}
```

- `@Service`: Spring administra una clase que contiene la lógica de consulta.
- `RestTemplateBuilder`: construye el cliente HTTP para comunicarse con otro servidor.
- `connectTimeout`: tiempo máximo para establecer la conexión.
- `readTimeout`: máximo para recibir datos.
- Token y URL vienen de variables externas, no se escriben secretos en Java.

**Cabeceras y envío de formulario al proveedor:**

```java
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(token);
headers.setAccept(List.of(MediaType.APPLICATION_JSON));
headers.setContentType(MediaType.MULTIPART_FORM_DATA);

MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
form.add("dni", dni);
HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(form, headers);
ResponseEntity<Map> respuesta = restTemplate.exchange(
    apiUrl, HttpMethod.POST, request, Map.class);
```

- `Authorization: Bearer ...`: autoriza a **SkillBridge frente a VerificaID**, usando el token del proveedor.
- `multipart/form-data`: formato que **VerificaID** espera para recibir el atributo `dni`.
- `exchange`: ejecuta el POST HTTP y permite leer la respuesta externa.
- La respuesta del proveedor contiene un bloque JSON `data`; de allí el servicio extrae `dni`, `nombres`, `apellido_paterno` y `apellido_materno`.

**Comparación de nombres:** `validar(...)` llama a `consultar(...)` y compara nombres y apellidos tras normalizar tildes, mayúsculas y espacios. Devuelve `coincide: true/false`.

**Manejo de errores:**

- DNI con longitud/formato incorrectos → 400.
- Token no configurado → 503.
- Proveedor responde 404 → 404 en nuestra API.
- Límite del proveedor (429) → nuestra API responde 503.
- Fallo de comunicación o JSON inesperado → 502.

*Estos son los códigos implementados; no indican que la conexión externa ya esté probada.*

---

## 13. CÓDIGO NUEVO: `ApiDniSecurityConfig.java`

```java
@Bean
@Order(1)
public SecurityFilterChain apiDniSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/identidad/**")
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .anyRequest().hasAnyRole("ADMINISTRADOR", "RESOURCE_MANAGER"))
        .httpBasic(Customizer.withDefaults());
    return http.build();
}
```

- `@Bean`: registra una configuración para Spring.
- `@Order(1)`: esta cadena específica se evalúa antes que la general.
- `securityMatcher`: **solo** afecta la ruta de identidad; no cambia el login web existente.
- `STATELESS`: la API no utiliza sesión HTTP para autenticar cada consulta.
- `httpBasic`: Postman envía usuario y contraseña de SkillBridge en la cabecera Authorization.
- `hasAnyRole`: solo roles ADMINISTRADOR o RESOURCE_MANAGER pueden acceder.
- `csrf.disable`: se aplica a esta API específica autenticada con HTTP Basic, sin cookies de sesión; el sistema web mantiene su configuración anterior.

**Pregunta fundamental:** «¿Por qué hay dos tipos de autenticación?» — «Para entrar a la aplicación web usamos formulario y sesión. Para consumir nuestra API desde Postman usamos HTTP Basic. Por otro lado, nuestro servidor usa Bearer token para autenticarse ante el proveedor externo.»

---

## 14. Postman: qué hay que mostrar

Importa `postman/SkillBridge_DNI_Solo_Postman.postman_collection.json`.

En la colección cambia:

- `base_url`: `http://localhost:8080` si ejecutas local.
- `correo_skillbridge`: correo de un usuario de **prueba** que sea ADMINISTRADOR o RESOURCE_MANAGER.
- `clave_skillbridge`: contraseña del usuario de prueba.
- `dni_prueba`: DNI de sandbox autorizado.

**Pruebas recomendadas (sin token externo):**

1. `GET /api/identidad/estado` con Basic Auth correcto → esperado 200.
2. La misma API sin credenciales → esperado 401.
3. `POST /api/identidad/consultar` con DNI inválido → esperado 400.
4. `POST /api/identidad/consultar` con DNI de ocho dígitos, pero **sin token del proveedor** → esperado 503.

**Pruebas adicionales (requieren token real de sandbox):**

5. `POST /api/identidad/consultar` → estructura JSON con datos reales del sandbox si el proveedor admite el DNI.
6. `POST /api/identidad/validar` → compara los datos y devuelve `coincide`.

**No deben afirmar un resultado 200 de la consulta externa si no lo han ejecutado con su token.**

---

## 15. Códigos HTTP para memorizar

| Código | Explicación |
|---|---|
| 200 OK | Petición atendida exitosamente. |
| 400 Bad Request | El cuerpo o el DNI no cumple las validaciones. |
| 401 Unauthorized | No hay autenticación correcta ante nuestra API. |
| 403 Forbidden | El usuario se autenticó, pero no tiene el rol requerido. |
| 404 Not Found | El recurso o la persona consultada no se encuentra (según respuesta del proveedor). |
| 502 Bad Gateway | Problema al comunicarse con el proveedor o JSON no esperado. |
| 503 Service Unavailable | La consulta externa no está configurada o alcanzó un límite contemplado por el servicio. |

---

## 16. Relación con las PPT de la profesora

| PPT | Aplicación en el código |
|---|---|
| 9.1 Fundamentos de servicios web | Diferencia entre aplicación web, Web Service REST, JSON, solicitud y respuesta. |
| 9.2 Creación de REST | `@RestController`, `@RequestMapping`, `GET`, `POST`, JSON, `ResponseEntity`, códigos HTTP y pruebas Postman. |
| 9.2 Documentación REST | Explicar URL, método, parámetros de entrada, salida y errores. |
| 10.1 Consumo de servicios | Consumir endpoint externo de identidad usando DNI y Bearer token. |
| 10.1 Spring Security REST | HTTP Basic, rutas protegidas, `STATELESS`, `RestTemplateBuilder`, DTO. |
| 10.2 JavaScript y 10.3 jQuery | **No son parte de este complemento**. Existen como clases del curso, pero no se añadieron nuevas pantallas JS/jQuery. |
| 11 AJAX | **No implementado en el complemento**: el chat original sigue siendo POST tradicional. |

La PPT utiliza un ejemplo de RENIEC mediante proveedores externos: el código elegido utiliza específicamente **VerificaID**. No confundir al exponer.

---

## 17. Posibles preguntas de la profesora: respuestas cortas

1. **¿Para qué sirve Spring Boot?** Para desarrollar la lógica y los controladores de nuestro sistema Java, incluidos los Web Services.
2. **¿Qué hace `@Controller`?** Atiende rutas web y normalmente devuelve una vista HTML.
3. **¿Qué hace `@RestController`?** Atiende rutas HTTP y devuelve datos, normalmente JSON.
4. **¿Qué es REST?** Un estilo de arquitectura para exponer recursos mediante una interfaz HTTP.
5. **¿Qué es JSON?** Formato de datos en pares clave-valor; no es una BD ni una página HTML.
6. **¿Qué es Postman?** Cliente HTTP usado para enviar peticiones y revisar respuestas y estados.
7. **¿Qué es `@RequestBody`?** Convierte el cuerpo JSON de la solicitud a un objeto Java.
8. **¿Qué es `@Valid`?** Ejecuta las anotaciones de validación del objeto.
9. **¿Qué es un DTO?** Un objeto para transportar información sin exponer las entidades JPA.
10. **¿Qué es Spring Data JPA?** Capa que simplifica consultas y persistencia de entidades Java en MySQL.
11. **¿Para qué sirve `JpaRepository`?** Para operaciones de consulta y actualización como `findAll`, `findById` y `save`.
12. **¿Qué hace `@Entity`?** Representa una tabla de base de datos mediante una clase Java.
13. **¿Qué hace `@ManyToOne`?** Define una relación donde varios registros se vinculan con un mismo objeto, por ejemplo proyectos–PM.
14. **¿Qué hace BCrypt?** Verifica contraseñas mediante hashes; no requiere guardar la clave en texto plano.
15. **¿Cómo restringen roles?** Con Spring Security y `requestMatchers(...).hasRole(...)`.
16. **¿Qué es Session?** Almacena datos asociados a una sesión web; se usa al iniciar sesión en las vistas.
17. **¿Por qué `STATELESS` en la API?** Para que cada llamada de Postman se autentique independientemente, sin depender de cookies de sesión.
18. **¿Qué es HTTP Basic?** Cabecera de autenticación de usuario y contraseña; debe usarse sobre HTTPS fuera de desarrollo local.
19. **¿Qué es Bearer token?** Credencial que nuestro servidor envía a VerificaID para autorizar la consulta externa.
20. **¿Qué hace `RestTemplateBuilder`?** Configura el cliente `RestTemplate` que consume servicios REST.
21. **¿Qué hace `exchange`?** Envía una solicitud HTTP y recibe una respuesta del servicio remoto.
22. **¿Cómo saben que el DNI es válido?** Primero comprobamos formato (ocho dígitos) y, con token y permiso, consultamos al proveedor; no hay verificación biométrica.
23. **¿Qué hace el dashboard?** Calcula indicadores con datos del repositorio y los muestra en las vistas Thymeleaf.
24. **¿Cómo exportan reportes?** Generan texto CSV con `ResponseEntity` y cabecera de descarga.
25. **¿El chat es un chatbot?** No. Actualmente es un chat de mensajes entre colaboradores por proyecto.
26. **¿El chat tiene AJAX?** No todavía. Usa GET y POST tradicionales y Thymeleaf.
27. **¿El proyecto ya está dockerizado?** En el material original recibido no hay `Dockerfile` o `docker-compose.yml`; el nombre de la carpeta no lo convierte en Docker.
28. **¿La API DNI consulta RENIEC directamente?** No. Consulta un proveedor externo llamado VerificaID, como el enfoque enseñado en clase.
29. **¿Se guardan datos de identidad en BD?** No en esta integración; se consulta y devuelve el resultado mediante DTO.
30. **¿Qué pueden demostrar sin token?** Que la API responde, verifica autenticación y valida entradas, pero no una consulta efectiva de identidad.

---

## 18. Qué decir en una exposición de 60 segundos

> “Nuestro proyecto SkillBridge AI está hecho en Spring Boot, con Spring MVC, Thymeleaf, Spring Data JPA y MySQL. Implementamos módulos por rol: administrador, Project Manager, Resource Manager y colaborador, cada uno con sus funciones, validaciones y reportes. La seguridad usa Spring Security con usuarios y roles almacenados en la base de datos; el login web utiliza sesión. También desarrollamos un chat entre colaboradores por proyecto y exportación de reportes CSV. Como complemento de las últimas PPT, agregamos un Web Service REST para consultar y comparar los datos de un DNI a través de VerificaID. El endpoint recibe y devuelve JSON, utiliza DTO y validaciones, consume la API externa con RestTemplateBuilder y Bearer token, y está protegido con HTTP Basic para probarlo en Postman. La consulta externa requiere un token válido, por lo que no la damos por verificada hasta ejecutar una prueba real.”

---

## 19. Riesgos y mejoras pendientes (NO se cambiaron tus módulos originales)

- **Riesgo original:** `pmDemo()`, `colaboradorDemo()` y `autorDemo()` contienen un *fallback* que puede seleccionar otro usuario de la base si falla la sesión. Es mejor obligar a obtener el usuario autenticado, pero **no lo modifiqué** para no romper avances existentes.
- **Chat:** existe y guarda mensajes, pero todavía no es AJAX ni chatbot.
- **Datos y URL de cloud:** el despliegue cloud final no fue probado en esta entrega.
- **Docker:** no se agregaron archivos de contenedores.
- **Consulta DNI:** requiere token de VerificaID; no se grabaron datos en MySQL.
- **Seguridad:** se retiró una contraseña cloud **antigua y comentada** del archivo `application.properties` para no distribuirla en el ZIP. Si la clave aún existe, corresponde rotarla; la configuración de conexión **activa** conserva las variables de entorno.
- **Pruebas:** el complemento incluye una clase de tests de validación sin llamadas externas, pero **no se ejecutó Maven en este entorno** porque no está instalado. Hay que compilar en IntelliJ y probarlo con MySQL y Postman.

---

## 20. Orden recomendado para ensayar la demostración

1. Iniciar la aplicación con MySQL y entrar al login.
2. Mostrar una función del Administrador (usuarios/habilidades), una del PM (proyectos/asignaciones) y una del Resource Manager (carga/disponibilidad).
3. Mostrar la vista del Colaborador: perfil o chat por proyecto.
4. Abrir el dashboard y descargar un reporte CSV.
5. En Postman: probar `/api/identidad/estado` con Basic Auth y mostrar `200`.
6. Repetir sin autorización y mostrar `401`.
7. Consultar un DNI con formato erróneo y mostrar `400`.
8. Si hay token sandbox y autorización de prueba, consultar un DNI válido y después hacer la comparación de nombres.

**Clave de la exposición:** diferenciar claramente lo que ya funcionaba como web (Spring MVC + Thymeleaf) de lo recién integrado como Web Service (Spring REST + JSON + Postman).
