# SkillBridge AI - Colaborador V3

Esta versión incorpora las correcciones visuales y funcionales solicitadas para el módulo **Colaborador**, manteniendo Spring MVC + Thymeleaf + Spring Data JPA, de acuerdo con la metodología trabajada en clase.

## 1. Abrir en IntelliJ
1. Descomprime el ZIP.
2. IntelliJ IDEA -> File -> Open.
3. Abre la carpeta `skillbridge-ai-colaborador-corregido-v3`.
4. Espera que Maven cargue `pom.xml`.
5. Usa JDK 17.

## 2. Configurar MySQL
Para usar MySQL local, configura `SPRING_PROFILES_ACTIVE=local` y
`SPRING_DATASOURCE_PASSWORD` en las variables de entorno de IntelliJ.
El perfil usa `localhost:3306` y el usuario `root` de forma predeterminada.
Puedes cambiarlos con `SPRING_DATASOURCE_URL` y `SPRING_DATASOURCE_USERNAME`.
Para activar Gmail junto con MySQL local, usa `SPRING_PROFILES_ACTIVE=local,gmail`.
En AWS se conserva la configuración de RDS del perfil predeterminado.
El perfil local usa la guía del asistente sin credenciales de Gemini. Para
probar Gemini en local, configura `SKILLBRIDGE_CHATBOT_PROVIDER=gemini`,
`SPRING_AI_MODEL_CHAT=google-genai` y `GEMINI_API_KEY`.

### Consulta DNI

Administración y Resource Management pueden abrir **Consulta DNI** desde el menú.
Define `APIPERU_TOKEN` con la clave de tu cuenta de API Perú. En IntelliJ,
agrégala a las variables de entorno de la configuración de ejecución. Con
Docker Compose puedes ponerla en el `.env` local. La URL predeterminada es
`https://api.apiperu.pe/dni`. Consulta la [documentación de API Perú](https://docs.apiperu.pe/).

## 3. Base de datos
### Si crearás la BD desde cero
Ejecuta en este orden:
1. `src/main/resources/database/skillbridge_ai_18_tablas_corregida.sql`
2. `src/main/resources/database/datos_demo.sql`

### Si YA tienes skillbridge_ai creada con la versión anterior
Ejecuta:
1. `src/main/resources/database/actualizacion_colaborador_v3.sql`
2. `src/main/resources/database/actualizacion_chatbot_gemini.sql`
3. Luego, solo si deseas restaurar todos los datos de presentación, ejecuta `datos_demo.sql`.

**Importante:** `datos_demo.sql` elimina los registros demo y los vuelve a insertar. No elimina las tablas. Ya incluye la corrección de `SQL_SAFE_UPDATES` para evitar el error 1175.

El modelo visual actualizado de MySQL Workbench está en:
`src/main/resources/database/tabla_refinada_gtics_v3.mwb`

## 4. Cambios principales del Colaborador
- Inicio más práctico: se retiraron accesos repetidos, actividad de foro y notificaciones del dashboard.
- Se muestra proyecto principal, carga actual y disponibilidad estimada.
- El logo lateral conserva la sesión y regresa al inicio del rol.
- Perfil completamente en español: `Nivel profesional`, `Junior`, `Semi senior`, `Senior`, `Líder`.
- Validaciones de perfil: nombres/apellidos/cargo/área solo letras y hasta 20 caracteres; teléfono exactamente 9 dígitos; disponibilidad 0-100.
- Certificaciones: nombre y entidad máximo 40 caracteres y formulario visualmente uniforme.
- Habilidades: se retiró la tarjeta Catálogo; agregar solo habilidades aún no registradas; nivel en formato legible; experiencia 0-50 años.
- Asignaciones: filtros uniformes y fechas `dd-MM-yyyy`.
- `Knowledge Hub` fue reemplazado por `Foro` / `Foro de conocimiento`.
- Foro con paginación para que muchos temas no alarguen la vista indefinidamente.
- Chat con fechas legibles y controles uniformes.
- Notificaciones con tipos en español, tipo específico `CHAT`, resumen útil y paginación de 5 elementos.

## 5. Credencial demo principal
- Rol: Colaborador
- Correo: `maria.gonzalez@skillbridge.com`
- Contraseña: `demo`

## 6. Ejecutar
Ejecuta `SkillbridgeAiApplication.java` y abre:
`http://localhost:8080`

## Ajuste V4 — Foro y Chat

- Los filtros del Foro vuelven a estar alineados horizontalmente en escritorio.
- El Chat valida la pertenencia al proyecto, persiste el mensaje con `saveAndFlush()` y recarga la conversación en la misma vista.
- Esta versión no requiere cambios adicionales en la base de datos respecto de V3.

## Correo de bienvenida y recuperación

Configura estas variables de entorno en la configuración de ejecución de IntelliJ o del servidor.

| Variable | Valor |
| --- | --- |
| SPRING_MAIL_HOST | Servidor SMTP del proveedor. |
| SPRING_MAIL_PORT | Puerto SMTP con STARTTLS. El valor predeterminado es 587. |
| SPRING_MAIL_USERNAME | Usuario SMTP que entrega el proveedor. |
| SPRING_MAIL_PASSWORD | Credencial SMTP del proveedor. |
| SKILLBRIDGE_MAIL_FROM | Dirección verificada que aparecerá como remitente. Configúrala si el usuario SMTP es un identificador distinto de esa dirección. |
| SKILLBRIDGE_PUBLIC_URL | URL base HTTP o HTTPS que pueden abrir los destinatarios. Para un despliegue público usa HTTPS cuando esté disponible. El valor predeterminado local es http://localhost:8080. |

Al iniciar, Spring crea la tabla de recuperación si todavía no existe. El script de actualización es src/main/resources/database/actualizacion_recuperacion_contrasena.sql. La cuenta de MySQL necesita permiso CREATE para ejecutar el script. Si la tabla ya fue creada por separado, se puede ejecutar la aplicación con SPRING_SQL_INIT_MODE=never.

El enlace de recuperación vence a los 30 minutos y se consume al guardar la nueva contraseña. Después del cambio, el usuario vuelve al inicio de sesión.

### Envío desde Gmail

Puedes dedicar una cuenta Gmail normal a los avisos de SkillBridge AI. Activa la verificación en dos pasos de esa cuenta y genera una contraseña de aplicación. [Crear una cuenta Gmail](https://support.google.com/mail/answer/56256?hl=es) · [Contraseñas de aplicación](https://support.google.com/accounts/answer/185833?hl=es)

SkillBridge usa smtp.gmail.com en el puerto 587 mediante STARTTLS de forma predeterminada. En Run → Edit Configurations → SkillbridgeAiApplication → Environment variables, o en el entorno del servidor, registra los siguientes valores. [Configuración SMTP de Gmail](https://support.google.com/mail/answer/7104828?hl=es)

| Variable | Valor |
| --- | --- |
| SPRING_MAIL_USERNAME | Dirección completa de la cuenta Gmail remitente. |
| SPRING_MAIL_PASSWORD | Contraseña de aplicación que generó Google. |

Los valores se guardan en la configuración local de ejecución. Reinicia la aplicación y registra un usuario de prueba. El destinatario es la dirección registrada, sea de Gmail, Hotmail, Outlook u otro dominio.

### Remitente administrado para una prueba sin dominio propio

Azure Communication Services Email permite usar un remitente DoNotReply con un dominio asignado por el proveedor. Esta configuración requiere una suscripción de Azure y los envíos están sujetos a sus tarifas y límites. [Remitentes administrados](https://learn.microsoft.com/en-us/azure/communication-services/concepts/email/email-domain-and-sender-authentication) · [Precios de Azure](https://azure.microsoft.com/es-es/pricing/details/communication-services/)

1. Crea un recurso de Email Communication Services y agrega un dominio administrado por Azure. Conecta el dominio a tu recurso de Communication Services. [Conectar el dominio](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/email/connect-email-communication-resource)
2. Crea las credenciales SMTP para la aplicación y configura las variables siguientes en el entorno que ejecuta SkillBridge AI. [Autenticación SMTP de Azure](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/email/send-email-smtp/smtp-authentication)

| Variable | Valor con Azure |
| --- | --- |
| SPRING_MAIL_HOST | smtp.azurecomm.net |
| SPRING_MAIL_PORT | 587 |
| SPRING_MAIL_USERNAME | Usuario SMTP generado en Azure. |
| SPRING_MAIL_PASSWORD | Secreto de la aplicación de Microsoft Entra asociada. |
| SKILLBRIDGE_MAIL_FROM | Dirección completa DoNotReply asignada al dominio administrado. |

3. Reinicia SkillBridge AI. Al crear una cuenta, Administración muestra si el proveedor aceptó el correo de bienvenida. Para una cuenta que ya existe, solicita un enlace con «¿Olvidaste tu contraseña?» desde el login.
