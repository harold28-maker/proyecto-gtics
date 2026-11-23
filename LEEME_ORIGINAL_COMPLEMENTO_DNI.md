# SkillBridge - SOLO lo que se pidio: API DNI + Postman (PPT GTICS)

## QUE CAMBIE (y QUE NO)

**Hecho para el proyecto ORIGINAL `proyecto-gtics-entregable7.2.zip`.**

- ✅ Agregados 1 controlador REST, 1 servicio que consume una API externa, 4 DTO, 1 configuracion de seguridad exclusiva para la API y 1 clase de pruebas.
- ✅ Agregada coleccion de Postman con pruebas GET, POST, validacion de entrada y acceso no autorizado.
- ✅ `pom.xml`: unica modificacion a archivo preexistente, agregar `spring-boot-starter-restclient` para usar `RestTemplateBuilder` en Spring Boot 4.0.0.
- ✅ Las credenciales de la API externa se configuran **solo con variables de entorno**, sin cambiar `application.properties`.
- ⛔ NO se modificaron `SecurityConfig.java`, `AdminController.java`, login, menus, templates, JPA, tablas MySQL, dashboard, reportes, chat o Docker.
- ⛔ No se agrego una pantalla AJAX, un chatbot ni una API REST CRUD completa: eso seria adelantarse a tu pedido.

Solo te entrego los **ARCHIVOS PARA AGREGAR/REEMPLAZAR**, no una copia del proyecto completo; asi no sobreescribes ni pierdes otros avances de tu equipo.

## COMO INSTALAR SIN PERDER AVANCES

1. Haz una **copia** de tu carpeta `proyecto-Gtics` actual.
2. Abre el ZIP: copia **el contenido de la carpeta `proyecto-Gtics/`** sobre la carpeta `proyecto-Gtics/` que ya usas. Se agregaran las nuevas clases.
3. **Antes de reemplazar el `pom.xml`**, comprueba si tu equipo ha modificado dependencias desde el Entregable 7. Si lo han editado, NO reemplaces: solo agrega esta dependencia a `<dependencies>` del POM actual:

```xml
<!-- PPT de consumo REST: RestTemplateBuilder -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-restclient</artifactId>
</dependency>
```

4. En IntelliJ, recarga el proyecto Maven. Mantén MySQL y configuraciones como estaban.
5. En **Run > Edit Configurations > Environment variables** puedes configurar:
   - `APIPERU_TOKEN=TOKEN_DE_TU_CUENTA`
   - `APIPERU_URL=https://api.apiperu.pe/dni` (opcional; ese es el valor por defecto)
6. Inicia tu app con normalidad (`http://localhost:8080`).
7. Importa `postman/SkillBridge_DNI_Solo_Postman.postman_collection.json` en Postman.
8. En las variables de la coleccion, coloca `correo_skillbridge` y `clave_skillbridge` de un **usuario de PRUEBA** con rol `ADMINISTRADOR` o `RESOURCE_MANAGER` que ya exista en la BD. No guardes ni compartas tu clave en la coleccion exportada.
9. Corre primero la solicitud `1 - Estado API` (esperado **200**) y luego `5 - Sin autenticar` (**401**) y `4 - DNI invalido` (**400**). Estas no requieren token del proveedor.
10. Ejecuta `2 - Consultar DNI` con datos de prueba autorizados y token del proveedor (200 si el proveedor devuelve datos; puede devolver 404 o 502 si la consulta falla). Sin token la respuesta sera **503**.
11. Ejecuta `3 - Comparar DNI y nombres` para comprobar el resultado `coincide: true/false` cuando la consulta externa tenga exito.

## RUTAS IMPLEMENTADAS

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| GET | `/api/identidad/estado` | Verificar que API responde; requiere Basic Auth |
| POST | `/api/identidad/consultar` | Body JSON `{ "dni": "00000000" }`; consulta proveedor |
| POST | `/api/identidad/validar` | Body JSON con DNI, nombres y apellidos; compara datos |

**Autenticacion A:** Postman -> SkillBridge: HTTP **Basic** (correo y contrasena de una cuenta de prueba del proyecto). La API solo admite roles ADMINISTRADOR o RESOURCE_MANAGER. Se usa HTTP Basic STATELESS segun PPT de Spring Security; el login web original no se cambia.

**Autenticacion B:** SkillBridge -> API Perú: `Authorization: Bearer` con `APIPERU_TOKEN`, configurado en el servidor y NUNCA enviado desde Postman a SkillBridge ni al navegador.

**Ejemplo de exito si el proveedor responde:**

```json
{"dni":"00000000","nombres":"NOMBRES DE EJEMPLO","apellidoPaterno":"PATERNO","apellidoMaterno":"MATERNO"}
```

*Es solo un JSON de estructura ilustrativa, no es una consulta real ni un resultado de RENIEC.*

## BASADO EN LAS PPT

- **Clase 9.1 - Fundamentos:** comunicacion cliente/servidor por JSON y HTTP.
- **Clase 9.2 - Creacion:** `@RestController`, rutas, `@RequestBody`, codigos HTTP y pruebas con Postman.
- **Clase 10.1 - Consumo p2:** consumo de un proveedor externo con Bearer. SkillBridge aplica ese patrón con API Perú, usando un POST JSON a `/dni`.
- **Clase 10.1 - Web Services REST con Spring Security:** `RestTemplateBuilder` y uso de DTO; proteger una API con HTTP Basic, `STATELESS`. La profe menciona `PersonaResponse` y `PersonaData`; se siguio ese concepto, devolviendo un DTO resumido.

No se incluyeron **jQuery/AJAX** ni **Docker** porque son temas aparte que no solicitaste agregar ahora.

## LIMITACIONES REALES

- No es una conexion directa con un servicio oficial de RENIEC: se usa el **proveedor API Perú**. Para una API oficial RENIEC se necesita acceso y documentacion autorizada.
- El endpoint predeterminado de API Perú es `https://api.apiperu.pe/dni`.
- La cuenta de API Perú debe estar activa y tener acceso. Sin token, no funcionara una consulta real (HTTP 503).
- No se graban DNIs ni nombres en tu BD. Se usa un DTO como en la PPT.
- Esta implementacion comprueba la coincidencia de los nombres devueltos; no verifica posesion del documento ni identidad biometrica.
- En produccion usa HTTPS, limites de consultas y manejo adecuado de datos personales; no uses DNIs de terceros sin autorizacion.
- La estructura del ZIP fue verificada y los Java revisados; **no se corrio Maven ni se pudo probar la API externa con token real en este entorno**. La compilacion y pruebas en IntelliJ/Postman son pasos pendientes.

## ¿QUE DEBERIAS MOSTRARLE A LA PROFESORA?

1. En Postman, **GET /api/identidad/estado -> 200** tras autenticarte (demuestra API protegida).
2. **Sin credenciales -> 401**.
3. **POST /api/identidad/consultar** con DNI invalido -> 400 (validacion).
4. Con una clave de API Perú y un DNI autorizado, **POST /api/identidad/consultar** -> JSON con los datos devueltos por el proveedor.
5. **POST /api/identidad/validar** -> JSON con `coincide`.

Si no tienen token, solo pueden demostrar localmente seguridad, REST y validacion de formato; **no deben afirmar que se verifico el DNI con RENIEC**.
