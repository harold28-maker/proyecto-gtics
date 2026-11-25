# SkillBridge AI — Ajustes Colaborador V4

Esta versión parte de `skillbridge-ai-colaborador-corregido-v3` y aplica dos correcciones solicitadas para la presentación.

## 1. Foro de conocimiento

- Los filtros vuelven a mostrarse en una sola fila en pantallas de escritorio: búsqueda, categoría, estado y acciones.
- En resoluciones intermedias las acciones bajan a una segunda línea para no deformar el contenido.
- En móvil los controles sí se apilan para conservar legibilidad.
- Se conserva la paginación y el panel lateral de categorías.

## 2. Chat de proyectos

- Se reforzó la validación del proyecto seleccionado: el colaborador solo puede escribir en proyectos en los que está asignado.
- Se eliminan proyectos duplicados en la lista lateral y se ignoran asignaciones canceladas.
- El envío valida mensajes vacíos y un máximo de 500 caracteres.
- Se utiliza `saveAndFlush()` antes de recargar la conversación para que el nuevo mensaje quede persistido y visible inmediatamente.
- Se muestran mensajes de éxito y error en la misma pantalla, sin `redirect:`.
- Se mejoró la interfaz del chat: proyecto activo, conversación seleccionada, estados vacíos y campo de envío uniforme.

## Base de datos

No se agregaron tablas ni columnas en esta versión. Los scripts V3 incluidos en `src/main/resources/database` siguen siendo válidos.
