# Backend - Auditoría

> Documento ajustado contra el código fuente actual. Describe la auditoría implementada mediante anotación, aspecto AOP y consulta paginada.

## 1. Propósito

El módulo de auditoría registra acciones relevantes ejecutadas sobre entidades del sistema. Su objetivo es conservar trazabilidad técnica de operaciones funcionales y administrativas sin acoplar esa lógica a los servicios de negocio.

---

## 2. Componentes principales

| Componente | Responsabilidad |
|---|---|
| `@Auditable` | Anotación para marcar métodos auditables. |
| `AuditAspect` | Intercepta métodos anotados y construye el evento de auditoría. |
| `AuditLogService` | Guarda auditoría de forma asíncrona y consulta registros. |
| `AuditLogRepository` | Acceso a persistencia de auditoría. |
| `AuditLog` | Entidad JPA del registro histórico. |
| `AuditLogDTO` | DTO expuesto por la API. |
| `AuditLogController` | Expone `GET /api/audit`. |

---

## 3. Registro con `@Auditable`

Los métodos de escritura que requieren trazabilidad se anotan con `@Auditable`, indicando:

- acción;
- nombre de entidad.

El aspecto `AuditAspect` usa `@AfterReturning`, por lo que registra los métodos marcados explícitamente con `@Auditable` cuando terminan correctamente y retornan control al flujo.

---

## 4. Registro asíncrono

`AuditLogService.logAction` está anotado con `@Async` y `@Transactional`. Esto permite enviar el registro de auditoría sin bloquear el hilo principal del servicio de negocio.

El registro contiene:

- usuario autenticado o `SISTEMA` cuando no hay autenticación válida;
- acción;
- entidad;
- identificador de entidad;
- fecha y hora;
- detalles del método ejecutado y argumentos.

---

## 5. Extracción del identificador de entidad

`AuditAspect` extrae `entityId` de forma heurística:

1. intenta leer `getId()` del resultado;
2. intenta leer getters terminados en `Id` del resultado;
3. revisa argumentos `Long` o `String`;
4. intenta leer `getId()` o getters terminados en `Id` de los argumentos.

Si no encuentra identificador, el registro se guarda sin `entityId`.

---

## 6. Consulta de auditoría

El controller expone:

```http
GET /api/audit?page=0&size=20&username=usuario&sortBy=timestamp&sortDir=desc
```

La consulta requiere `ACCEDER_ADMINISTRACION`.

Parámetros:

| Parámetro | Descripción | Valor por defecto |
|---|---|---|
| `page` | Página solicitada, base cero | `0` |
| `size` | Tamaño de página | `20` |
| `username` | Filtro opcional por usuario | no requerido |
| `sortBy` | Campo de ordenamiento | `timestamp` |
| `sortDir` | Dirección de ordenamiento | `desc` |

---

## 7. Protección de registros

El paquete incluye el script:

```text
backend/db/triggers/audit_immutable.sql
```

Este recurso permite reforzar el carácter histórico de la tabla de auditoría a nivel de base de datos cuando se aplica en el ambiente correspondiente. El registro de eventos se realiza desde el módulo backend de auditoría y su consulta HTTP se ofrece mediante `GET /api/audit`.
