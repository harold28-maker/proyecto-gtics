# Documentación de APIs REST e Integraciones Externas — SkillBridge AI

## 1. Arquitectura de Servicios Web y Seguridad

SkillBridge AI combina vistas web tradicionales (Spring MVC + Thymeleaf) con **APIs REST JSON** y conectores hacia **servicios externos**. Cada grupo de endpoints REST cuenta con una política de seguridad específica configurada en Spring Security:

| Módulo / Ruta Base | Controlador | Autenticación y Sesión | Roles Autorizados |
| :--- | :--- | :--- | :--- |
| `/api/identidad/**` | `IdentidadRestController` | **HTTP Basic (`STATELESS`)**, CSRF deshabilitado (`ApiDniSecurityConfig`) | `ADMINISTRADOR`, `RESOURCE_MANAGER` |
| `/api/chatbot/**` | `ChatbotController` | **Sesión Web Autenticada** (`SecurityConfig` + token CSRF) | Todos los roles autenticados (`ADMINISTRADOR`, `PROJECT_MANAGER`, `RESOURCE_MANAGER`, `COLABORADOR`) |
| `/actuator/**` | Spring Boot Actuator | Público (`/actuator/health`) | Monitoreo operativo del sistema |

> **Colección de pruebas Postman:** Disponible en `postman/SkillBridge_DNI_Solo_Postman.postman_collection.json` con variables preconfiguradas (`base_url`, `correo_skillbridge`, `clave_skillbridge`).

---

## 2. Endpoints REST Propios (Exposición de Servicios)

### 2.1. API de Identidad y Consulta DNI (`/api/identidad`)
* **Controlador:** `pe.edu.pucp.skillbridge.controller.IdentidadRestController`
* **Formato de intercambio:** `application/json` (los datos sensibles como el DNI viajan en el cuerpo `POST` y nunca en la URL).

#### A. Verificar estado del servicio
* **Método y Ruta:** `GET /api/identidad/estado`
* **Autenticación:** HTTP Basic (Credenciales de cuenta SkillBridge con rol `ADMINISTRADOR` o `RESOURCE_MANAGER`).
* **Parámetros:** Ninguno.
* **Respuesta exitosa (`200 OK`):**

```json
{
  "servicio": "identidad",
  "estado": "ok"
}
```

* **Códigos de error:** `401 Unauthorized` (credenciales inválidas), `403 Forbidden` (rol sin permisos).

#### B. Consultar datos por número de DNI
* **Método y Ruta:** `POST /api/identidad/consultar`
* **Autenticación:** HTTP Basic (`ADMINISTRADOR` o `RESOURCE_MANAGER`).
* **Cuerpo de solicitud (`DniConsultaRequest`)** *(Validación: exactamente 8 dígitos numéricos `[0-9]{8}`)*:

```json
{
  "dni": "12345678"
}
```

* **Respuesta exitosa (`200 OK` — `DniConsultaResponse`):**

```json
{
  "dni": "12345678",
  "nombres": "JUAN CARLOS",
  "apellidoPaterno": "PEREZ",
  "apellidoMaterno": "GOMEZ",
  "nombreCompleto": "JUAN CARLOS PEREZ GOMEZ"
}
```

* **Códigos de error:**
  * `400 Bad Request`: Formato de DNI inválido (distinto de 8 dígitos).
  * `404 Not Found`: DNI no encontrado en el proveedor externo.
  * `503 Service Unavailable`: Token `APIPERU_TOKEN` no configurado o fallo de conexión con el proveedor externo.

#### C. Validar coincidencia de identidad (DNI vs Nombres declarados)
* **Método y Ruta:** `POST /api/identidad/validar`
* **Autenticación:** HTTP Basic (`ADMINISTRADOR` o `RESOURCE_MANAGER`).
* **Cuerpo de solicitud (`DniValidacionRequest`):**

```json
{
  "dni": "12345678",
  "nombres": "Juan Carlos",
  "apellidoPaterno": "Pérez",
  "apellidoMaterno": "Gómez"
}
```

* **Respuesta exitosa (`200 OK` — `DniValidacionResponse`):**

```json
{
  "dni": "12345678",
  "coincide": true,
  "nombresOficiales": "JUAN CARLOS",
  "apellidoPaternoOficial": "PEREZ",
  "apellidoMaternoOficial": "GOMEZ",
  "detalle": "Los datos coinciden con el registro consultado."
}
```

* **Nota de privacidad:** El servicio normaliza tildes/mayúsculas para comparar, pero **no persiste** datos personales externos en la base de datos de SkillBridge.

---

### 2.2. API del Asistente Conversacional IA (`/api/chatbot`)
* **Controlador:** `pe.edu.pucp.skillbridge.controller.ChatbotController`
* **Formato de intercambio:** `application/json`.

#### A. Consultar modo activo del asistente
* **Método y Ruta:** `GET /api/chatbot/estado`
* **Autenticación:** Sesión autenticada.
* **Respuesta exitosa (`200 OK`)** *(Valores posibles: `"gemini"` en nube/producción o `"local"` en modo guía sin API Key)*:

```json
{
  "modo": "gemini"
}
```

#### B. Enviar consulta al asistente inteligente
* **Método y Ruta:** `POST /api/chatbot`
* **Autenticación:** Sesión autenticada + cabecera `X-CSRF-TOKEN`.
* **Cuerpo de solicitud (`ChatbotRequest`):**

```json
{
  "mensaje": "¿Qué colaboradores tienen experiencia en Spring Boot y disponibilidad mayor al 50%?"
}
```

* **Respuesta exitosa (`200 OK` — `ChatbotResponse`):**

```json
{
  "exito": true,
  "respuesta": "Actualmente cuentan con disponibilidad...",
  "fecha": "2026-11-25T12:15:30"
}
```

* **Códigos de error:**
  * `400 Bad Request`: Mensaje vacío o en blanco (`@NotBlank`).
  * `503 Service Unavailable`: Error de comunicación con el proveedor de IA (`exito: false`, sin exponer trazas internas ni API keys).

#### C. Obtener historial de conversación del usuario
* **Método y Ruta:** `GET /api/chatbot/historial`
* **Autenticación:** Sesión autenticada (filtra automáticamente por `authentication.getName()`).
* **Respuesta exitosa (`200 OK` — `List<ChatbotMessageResponse>`):**

```json
[
  {
    "emisor": "USUARIO",
    "contenido": "¿Qué proyectos están activos?",
    "fecha": "2026-11-25T12:10:00"
  },
  {
    "emisor": "ASISTENTE",
    "contenido": "Actualmente existen 3 proyectos en estado ACTIVE...",
    "fecha": "2026-11-25T12:10:02"
  }
]
```

#### D. Limpiar historial de conversación
* **Método y Ruta:** `DELETE /api/chatbot/historial`
* **Autenticación:** Sesión autenticada + cabecera `X-CSRF-TOKEN`.
* **Respuesta exitosa:** `204 No Content`.

---

### 2.3. Endpoints de Recursos Binarios y Monitoreo
| Método | Ruta | Controlador | Descripción y Respuesta |
| :--- | :--- | :--- | :--- |
| `GET` | `/colaborador/perfil/foto` | `FotoPerfilController` | Devuelve la imagen de perfil (`image/jpeg`) del colaborador autenticado con `Cache-Control: no-store` (`404` si no tiene foto). |
| `GET` | `/pm/colaboradores/fotos/{id}`<br>`/resource/colaboradores/fotos/{id}` | `FotoPerfilController` | Devuelve la foto de perfil (`image/jpeg`) de un colaborador por su ID para los directorios de gestión. |
| `GET` | `/actuator/health` | Spring Actuator | Verifica disponibilidad de la aplicación y conexión a MySQL (`{"status":"UP"}`) sin exponer detalles internos (`show-details=never`). |

---

## 3. Integraciones con Servicios Externos (Consumo de APIs)

SkillBridge AI consume **tres servicios externos** configurados exclusivamente mediante **variables de entorno** para evitar la exposición de credenciales en el código fuente:

### 3.1. Inteligencia Artificial Generativa — Google Gemini (Spring AI)
* **Componentes:** `GeminiChatGateway.java`, `ChatbotService.java`, interfaz `AiChatGateway.java` y respaldo `GuiaLocalChatGateway.java`.
* **Propósito:** Proveer un asistente contextual que responde consultas sobre proyectos, colaboradores, habilidades y asignaciones reales almacenadas en SkillBridge AI.
* **Flujo de datos:**
  1. `ChatbotService` recopila el contexto operativo actualizado desde los repositorios JPA (proyectos, cargas laborales, skills y rol del usuario).
  2. `GeminiChatGateway` envía el `systemPrompt` (reglas y contexto de la plataforma) junto con el `userPrompt` mediante `ChatClient` de **Spring AI** (`spring-ai-starter-model-google-genai`).
  3. Si `skillbridge.chatbot.provider=local` o no existe `GEMINI_API_KEY`, el sistema conmuta automáticamente a `GuiaLocalChatGateway` para operar sin fallos en entornos locales.
* **Variables de entorno requeridas:** `SKILLBRIDGE_CHATBOT_PROVIDER`, `SPRING_AI_MODEL_CHAT`, `GEMINI_API_KEY`, `GEMINI_MODEL` (por defecto `gemini-3.5-flash-lite`).

### 3.2. Validación de Identidad — API Perú (RENIEC)
* **Componentes:** `ConsultaDniService.java`, `IdentidadRestController.java`, `ConsultaDniWebController.java`.
* **Propósito:** Consultar y validar nombres y apellidos oficiales a partir de un DNI peruano de 8 dígitos durante el registro o auditoría de usuarios.
* **Flujo de datos:**
  1. El cliente realiza una petición interna a `/api/identidad/consultar` o desde la vista web `/identidad/consulta-dni`.
  2. `ConsultaDniService` utiliza `RestTemplateBuilder` (con timeouts de 5s de conexión y 12s de lectura) para enviar una petición `POST` JSON hacia `https://api.apiperu.pe/dni` con cabecera `Authorization: Bearer <APIPERU_TOKEN>`.
  3. El servidor transforma la respuesta externa en `DniConsultaResponse` sin exponer el token al navegador.
* **Variables de entorno requeridas:** `APIPERU_TOKEN`, `APIPERU_URL`.

### 3.3. Notificaciones y Recuperación de Cuentas — Servidor SMTP (Gmail)
* **Componentes:** `CorreoCuentaService.java`, `RecuperacionContrasenaService.java`, `RecuperacionConfig.java`.
* **Propósito:** Enviar correos electrónicos transaccionales en formato HTML (plantilla Thymeleaf `correos/cuenta.html` con logo embebido) para entrega de credenciales iniciales y códigos temporales de recuperación de contraseña.
* **Flujo de datos:**
  1. Al crear una cuenta o solicitar recuperación de contraseña, `CorreoCuentaService` renderiza la plantilla HTML y la despacha mediante `JavaMailSender` (STARTTLS en el puerto `587`).
  2. El servicio captura excepciones (`MailAuthenticationException`, `MailException`) y retorna estados controlados (`ACEPTADO`, `SIN_CONFIGURACION`, `ERROR_AUTENTICACION`, `ERROR_ENVIO`) sin interrumpir la aplicación.
* **Variables de entorno requeridas:** `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SKILLBRIDGE_MAIL_FROM`, `SKILLBRIDGE_PUBLIC_URL`.