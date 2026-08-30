# API - Auditoría

## Consulta

```http
GET /api/audit
```

Requiere el permiso dedicado `Ver auditoría`.

| Parámetro | Tipo | Defecto | Regla |
|---|---|---|---|
| `page` | entero | `0` | mínimo 0 |
| `size` | entero | `20` | entre 1 y 100 |
| `username` | texto | — | contiene, sin distinguir mayúsculas |
| `action` | texto | — | coincidencia exacta |
| `entityName` | texto | — | coincidencia exacta |
| `outcome` | enum | — | `SUCCESS`, `FAILURE`, `DENIED` |
| `correlationId` | texto | — | coincidencia exacta |
| `from`, `to` | instante ISO-8601 | — | `from` no puede superar `to` |
| `sortBy` | texto | `occurredAt` | `occurredAt`, `actorUsername`, `action`, `entityName`, `outcome` |
| `sortDir` | texto | `desc` | `asc` o `desc` |

Un tamaño superior a 100, una página negativa o un campo de ordenamiento desconocido se rechazan con `400`.

## Elemento de respuesta

```json
{
  "id": 81,
  "actorUsername": "operador",
  "action": "CAMBIAR_ESTADO_CONSULTA",
  "entityName": "Consulta",
  "entityId": "10",
  "outcome": "SUCCESS",
  "occurredAt": "2026-08-30T15:10:00Z",
  "source": "HTTP",
  "correlationId": "e8550e42-87ef-47db-a861-c81112ee9673",
  "ipAddress": "192.0.2.10",
  "userAgent": "Mozilla/5.0",
  "reasonCode": null,
  "reason": null,
  "beforeState": { "estado": "EN_ESTUDIO" },
  "afterState": { "estado": "CERRADO" },
  "metadata": { "requestedState": "CERRADO" }
}
```

Los mapas sólo contienen campos escalares declarados expresamente. La API no devuelve argumentos crudos ni detalles técnicos de excepciones.
