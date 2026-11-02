# Entregable 7 - Spring en cloud: Reportes / Dashboard

Este avance parte del último ZIP entregado y conserva los CRUD, Security, sesión, validaciones y Docker existentes.

## Qué se implementó

### 1. Dashboard explícito
Los paneles que ya existían en `/inicio` se mantienen y ahora también responden a una ruta `/dashboard`:

- Administrador: `/admin/inicio` y `/admin/dashboard`
- Project Manager: `/pm/inicio` y `/pm/dashboard`
- Resource Manager: `/resource/inicio` y `/resource/dashboard`

Los dashboards muestran indicadores calculados con datos reales de la base de datos.

### 2. Reportes del Administrador
Nueva vista: `templates/admin/reportes.html`

Nueva ruta: `/admin/reportes`

Reportes disponibles:
- Usuarios y accesos.
- Catálogo de habilidades.
- Auditoría reciente.

Indicadores:
- Total de usuarios.
- Usuarios activos.
- Inactivos/bloqueados.
- Total y cantidad de habilidades activas.
- Distribución de usuarios por rol.
- Distribución de habilidades por categoría.

Descargas CSV:
- `/admin/reportes/exportar?tipo=usuarios`
- `/admin/reportes/exportar?tipo=habilidades`
- `/admin/reportes/exportar?tipo=auditoria`

La ruta sigue protegida por Spring Security mediante `/admin/**` y el Controller también usa `esAdmin(session)`.

### 3. Reportes del Resource Manager
Nueva vista: `templates/resource/reportes.html`

Nueva ruta: `/resource/reportes`

Reportes disponibles:
- Capacidad y carga.
- Asignaciones.
- Talent Matching.
- Proyectos.

Indicadores:
- Total de colaboradores.
- Colaboradores con 50% o más de disponibilidad.
- Colaboradores sin disponibilidad.
- Colaboradores sobrecargados.
- Carga promedio.
- Asignaciones activas.
- Proyectos activos.

Descargas CSV:
- `/resource/reportes/exportar?tipo=capacidad`
- `/resource/reportes/exportar?tipo=asignaciones`
- `/resource/reportes/exportar?tipo=talento`
- `/resource/reportes/exportar?tipo=proyectos`

La carga y disponibilidad reutilizan `CargaColaboradorDTO` y la consulta de `ColaboradorRepository.obtenerCargaColaboradores()`.

### 4. Reportes del Project Manager
El proyecto ya tenía `pm/reportes.html`. Se conservó la vista y se agregó descarga CSV a cada reporte.

Reportes:
- Resumen de proyectos.
- Asignaciones por proyecto.
- Carga de trabajo.
- Historial de proyectos.

Descargas CSV:
- `/pm/reportes/exportar?tipo=proyectos`
- `/pm/reportes/exportar?tipo=asignaciones`
- `/pm/reportes/exportar?tipo=carga`
- `/pm/reportes/exportar?tipo=historial`

Los reportes de proyectos y asignaciones se limitan a los proyectos del Project Manager actual.

## Flujo técnico de un reporte

Ejemplo Administrador:

1. El usuario entra a `/admin/reportes`.
2. Spring Security verifica el rol ADMINISTRADOR.
3. `AdminController.reportes()` consulta `UsuarioRepository`, `HabilidadRepository` y `AuditoriaRepository`.
4. El Controller calcula los indicadores con Streams y agrupa datos por rol/categoría.
5. Los datos se colocan en `Model`.
6. Thymeleaf renderiza `admin/reportes.html`.
7. Si se elige "Vista previa", la misma página muestra la tabla correspondiente.
8. Si se elige "Descargar CSV", el Controller genera un `ResponseEntity<String>` con `Content-Disposition: attachment`.

## Por qué CSV

No se agregó una librería nueva. El reporte se construye directamente desde Spring como texto CSV para mantener el proyecto sencillo y defendible. Se incluye UTF-8 y BOM para que Excel reconozca correctamente tildes y caracteres especiales.

Los campos se escapan entre comillas y se evita que valores que empiezan por `=`, `+`, `-` o `@` se interpreten como fórmulas al abrir el CSV.

## Archivos modificados

### Java
- `AdminController.java`
- `ProjectManagerController.java`
- `ResourceManagerController.java`

### Thymeleaf
- `templates/admin/inicio.html`
- `templates/admin/reportes.html` (nuevo)
- `templates/pm/inicio.html`
- `templates/pm/reportes.html`
- `templates/resource/inicio.html`
- `templates/resource/reportes.html` (nuevo)
- `templates/fragments/layout.html`

### CSS
- `static/css/app.css`

## Qué no se cambió

- No se cambió el esquema de las 18 tablas.
- No se añadieron dependencias nuevas al `pom.xml`.
- No se cambiaron los CRUD existentes.
- No se cambió Spring Security.
- No se cambió el manejo actual de HttpSession.
- No se cambió Docker ni la configuración de base de datos.

## Cómo probar

1. Levantar la aplicación normalmente.
2. Iniciar sesión con un usuario del rol correspondiente.
3. Administrador: abrir `Reportes` en el menú y probar vista previa/CSV.
4. Project Manager: abrir `Reportes` y probar los cuatro CSV.
5. Resource Manager: abrir `Reportes` y probar los cuatro CSV.
6. Confirmar que los valores cambien cuando cambien los datos de la base de datos.
7. Probar `/admin/dashboard`, `/pm/dashboard` y `/resource/dashboard` como alias de los paneles de inicio.

## Nota de compilación

En el entorno utilizado para preparar este ZIP no estaba instalado el comando `mvn`, por lo que no se pudo ejecutar `mvn compile`. Se hizo revisión estática de llaves/paréntesis de los Controllers y parsing de los HTML modificados. Al abrir el proyecto en IntelliJ, se recomienda ejecutar `mvn clean package` o iniciar Spring Boot para la validación final en el entorno local del equipo.

## Algo mas
- El resource manager puede ahora assignar y editar asignaciones :
  - se añade `templates/resource/asignaciones-form.html` con seleccion de colaborador con busqueda de texto
  - se añaden en controller/ResourceManagerController :
    - cargarAsignaciones
    - editarAsignacion ("/asignaciones/editar/{id}")
    - prepararFormularioAsignacion
    - guardarAsignacion ("/asignaciones/guardar")
    - cambiarEstadoAsignacion ("/asignaciones/{id}/estado")
    - nuevaAsignacion ("/asignaciones/nueva")
- En entity/Asignacion, se añade @DateTimeFormat(pattern = "yyyy-MM-dd") para que los fechas se colocan cuando se modifica una asignacion.
