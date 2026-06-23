# API - Auditoría

> Documento ajustado contra el código fuente actual. Describe únicamente el endpoint expuesto por `AuditLogController`.

## 1. Ruta base

```http
/api/audit
```

La API de auditoría permite consultar registros generados por métodos del backend marcados con `@Auditable`.

---

## 2. Endpoint disponible

| Método | Endpoint | Descripción | Permiso |
|---|---|---|---|
| `GET` | `/api/audit` | Consulta paginada de registros de auditoría | `ACCEDER_ADMINISTRACION` |

La operación expuesta por `AuditLogController` es la consulta paginada `GET /api/audit`, con filtro opcional por `username`.

---

## 3. Parámetros de consulta

| Parámetro | Tipo | Requerido | Valor por defecto | Descripción |
|---|---|---|---|---|
| `page` | entero | no | `0` | Página solicitada, con índice base cero. |
| `size` | entero | no | `20` | Tamaño de página. |
| `username` | texto | no | no aplica | Filtro opcional por usuario. |
| `sortBy` | texto | no | `timestamp` | Campo de ordenamiento. |
| `sortDir` | texto | no | `desc` | Dirección de ordenamiento. |

Ejemplo:

```http
GET /api/audit?page=0&size=20&username=admin&sortBy=timestamp&sortDir=desc
```

---

## 4. Respuesta

La respuesta es un `Page<AuditLogDTO>`.

Ejemplo de elemento:

```json
{
  "id": 1,
  "username": "admin",
  "action": "CREAR_CONSULTA",
  "entityName": "Consulta",
  "entityId": "10",
  "timestamp": "2026-05-31T10:00:00",
  "details": "Método ejecutado: crear. Argumentos: [...]"
}
```

---

## 5. Alcance de consulta

El endpoint permite filtrar por `username`, ordenar y paginar los resultados de auditoría.
