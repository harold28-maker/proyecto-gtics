# SKILLBRIDGE AI – PROYECTO COMPLETO + POSTMAN / DNI

## Qué contiene este ZIP

- `proyecto-Gtics/`: el **proyecto completo** de la entrega 7 (Spring Boot, MySQL, Thymeleaf, todos los módulos de administrador, PM, Resource Manager y colaborador), **más** los ocho archivos Java de `ENTREGABLECONPOSTMAN.zip`.
- `postman/SkillBridge_DNI_Solo_Postman.postman_collection.json`: solicitudes de Postman.
- `GUIA_COMPLETA_CODIGO_Y_EXPOSICION_GTICS.md`: explicación del código anterior y el complemento, para preparar las preguntas de la profesora.
- `RESUMEN_PARA_EXPOSICION.md`: repaso rápido.
- `LEEME_ORIGINAL_COMPLEMENTO_DNI.md`: instrucciones originales del archivo complementario.

## ¿Se reemplazaron los avances originales?

**No.** Se conservaron todos los archivos fuente y recursos originales. Solamente hubo estos dos cambios pequeños a archivos existentes:

1. `proyecto-Gtics/pom.xml`: se agregó `spring-boot-starter-restclient` para `RestTemplateBuilder` en Spring Boot 4.
2. `proyecto-Gtics/src/main/resources/application.properties`: se eliminó **una contraseña antigua comentada**, sin modificar la configuración activa basada en variables de entorno.

**Archivos nuevos:** 8 Java (controller, service, config, 4 DTO, test) y colección Postman.

Se excluyeron los directorios generados o dependientes de la computadora (`target`, `.idea`, `.hallmark`). IntelliJ y Maven pueden regenerarlos. No se eliminó ningún `.java`, `.html`, `.css`, `.sql`, `.mwb` ni archivo de documentación original.

## Cómo abrir

1. Extrae el ZIP y abre **la carpeta `proyecto-Gtics/`** como proyecto Maven en IntelliJ IDEA.
2. Comprueba que tienes **Java 17 o superior** y una instalación local de Maven (o las herramientas de Maven que utilice IntelliJ).
3. Asegura que existe tu BD MySQL y carga los SQL ubicados en `src/main/resources/database/` si todavía no están importados. Usa el procedimiento SQL que ya venía utilizando tu equipo para evitar duplicaciones.
4. Configura la conexión MySQL mediante las variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` cuando sea necesario.
5. Recarga dependencias Maven e inicia `SkillbridgeAiApplication`.
6. Importa la colección del directorio `postman/` en Postman.
7. En las variables de la colección configura `correo_skillbridge` y `clave_skillbridge` de un usuario de prueba con rol ADMINISTRADOR o RESOURCE_MANAGER, y `base_url=http://localhost:8080`.
8. Prueba primero `GET /api/identidad/estado`: con credenciales válidas debe responder 200; sin credenciales, 401.
9. Para consultar DNI: configura `VERIFICAID_TOKEN` con el token de sandbox autorizado y, opcionalmente, `VERIFICAID_URL=https://sandbox-api.verifica.id/v2/consulta/personas`.

## Límites conocidos

- No hay pruebas de integración completas ni compilación Maven ejecutada en este entorno: confirma en IntelliJ y Postman antes de entregar.
- Sin un token autorizado de VerificaID, las consultas externas de DNI no funcionarán (respuesta 503 de nuestra API).
- VerificaID **no es una API oficial directa de RENIEC**.
- No se añadieron pantallas de AJAX, chatbot, Docker ni CRUD REST completo. No se cambiaron los CRUD, login, sesión, dashboards o chat originales.
- No incluyas credenciales ni datos personales de terceros en archivos ZIP compartidos; usa cuentas y DNIs autorizados para pruebas.

## Relación con las PPT

**9.1:** solicitud y respuesta cliente-servidor, JSON. **9.2:** creación de Web Services, HTTP y Postman. **10.1:** consumo de REST, DTO, `RestTemplateBuilder`, Basic Auth y Bearer token. El complemento se limita a esos temas porque eran los que pidió el equipo.
