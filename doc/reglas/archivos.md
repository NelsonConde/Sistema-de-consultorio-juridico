# Reglas de negocio - Archivos

> Documento ajustado contra el código fuente actual. Describe reglas funcionales del almacenamiento documental.

## 1. Regla general

Los archivos se almacenan como recursos documentales asociados a flujos funcionales del sistema. El backend conserva rutas relativas y evita exponer rutas físicas internas como contrato de negocio.

---

## 2. Autenticación y autorización

Los endpoints de `/api/files` requieren usuario autenticado y validan permiso y alcance mediante el servicio de acceso del recurso asociado. Se reconocen estas claves lógicas:

- consulta: `{consultaId}/...`;
- documentos de tarea: `tareas-{seguimientoId}-documentos/...`;
- documentos de respuesta: `tareas-{seguimientoId}-respuestas-{respuestaId}/...`;
- conciliación: `conciliacion/{conciliacionId}/...`.

Las operaciones sin recurso identificable, como listar todos los directorios, se rechazan.

---

## 3. Rutas relativas

El almacenamiento trabaja con rutas relativas bajo una raíz configurada. El servicio limpia nombres de archivo, normaliza rutas y rechaza nombres o subdirectorios que contengan `..`.

---

## 4. Carga individual y múltiple

La carga puede ser individual o múltiple. En carga múltiple, cada archivo genera un resultado propio. Una respuesta puede contener archivos cargados exitosamente y errores individuales.

La persistencia documental usa estados `PENDING`, `ACTIVE`, `FAILED` y `DELETE_PENDING`. Un objeto solo se considera disponible cuando su metadato está en `ACTIVE`.

---

## 5. Validaciones documentales específicas

El módulo genérico de archivos no impone validación de tipo de documento. Cuando un flujo requiere restricciones específicas, la validación se implementa en el módulo correspondiente.

Ejemplo: conciliación exige PDF para solicitud inicial y acta de finalización mediante `ConciliacionDocumentoService`.

---

## 6. Rutas usadas por módulos

Rutas documentales relevantes:

- consulta: directorio asociado a la consulta;
- tarea de seguimiento: `tareas-{seguimientoId}-documentos`;
- respuesta de seguimiento: `tareas-{seguimientoId}-respuestas-{respuestaId}`;
- solicitud de conciliación: `conciliacion/{id}/solicitud.pdf`;
- acta de conciliación: `conciliacion/{id}/acta.pdf`.

---

## 7. Descarga y listado

La descarga se realiza por clave lógica asociada a un recurso autorizado. Si el recurso no existe, el backend responde como no encontrado. El listado solo permite consultar el prefijo del recurso autorizado.
