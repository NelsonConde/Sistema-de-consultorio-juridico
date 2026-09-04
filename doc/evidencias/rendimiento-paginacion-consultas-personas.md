# Evidencia de rendimiento de paginación en Consultas y Personas

Entorno:
- PostgreSQL Testcontainers
- postgres:16-alpine
- Java 21
- dataset sintético
- warmups: 5
- iteraciones: 30

| Módulo | Variante | Dataset | Filas respuesta | Payload bytes | SQL | p50 ms | p95 ms |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Consultas | Implementación anterior | 240 | 240 | 40333 | 241 | 270.91 | 454.634 |
| Consultas | Implementación actual | 240 | 10 | 1743 | 2 | 4.733 | 7.421 |
| Personas | Implementación anterior | 240 | 10 | 1729 | 2 | 2.069 | 3.408 |
| Personas | Implementación actual | 240 | 10 | 1729 | 2 | 3.947 | 12.171 |

## Interpretación

Los valores anteriores corresponden a esta ejecución local y deben interpretarse comparativamente dentro de las mismas condiciones.

- Consultas — implementación actual vs anterior: bytes delta_pct -95.678%, p50 delta_pct -98.253%, p95 delta_pct -98.368%, SQL delta_pct -99.17%.
- Personas — implementación actual vs anterior: bytes delta_pct 0%, p50 delta_pct 90.736%, p95 delta_pct 257.101%, SQL delta_pct 0%.

response_rows representa el número de elementos incluidos en la respuesta JSON, no filas leídas físicamente por PostgreSQL.
p50 y p95 se calculan ordenando las mediciones de la misma ejecución y tomando el índice ceil(percentil * n) - 1.
