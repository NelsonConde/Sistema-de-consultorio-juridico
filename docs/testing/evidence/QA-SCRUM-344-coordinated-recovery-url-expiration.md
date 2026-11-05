# SCRUM-344 — Recuperación coordinada y vencimiento real de URL en proveedor aislado

## Identificación

- **Incidencia:** [SCRUM-344](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-344)
- **Historia / Incidencia principal:** [SCRUM-341](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-341)
- **Predecesoras verificadas:** [SCRUM-332](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-332) (mergeada en `develop` commit `a332f96`) y [SCRUM-343](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-343) (mergeada en `develop` commit `d614ee9`).
- **Rama:** `test/scrum-344-coordinated-recovery-verification`
- **Responsable:** José Manuel Pérez Rodríguez (Operación y QA)
- **Fecha de ejecución:** 01/11/2026, zona `America/Bogota`
- **Ambiente:** Linux x86_64; OpenJDK 21; entorno aislado temporal externo al repositorio de trabajo; servidor HTTP aislado con control estricto de timestamps y expiración de S3 Presigned URLs (`X-Amz-Expires`); almacenamiento de respaldo material transitorio en `/tmp`.
- **Revisores designados:** Edwin Geanfranco Torres Zapata; Nelson Enrrique Conde Tarazona.

---

## Alcance y criterios evaluados

Conforme a la definición de [SCRUM-344](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-344) y el procedimiento de recuperación coordinada de [SCRUM-343](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-343), se ejecutó la validación independiente en un ambiente desechable y aislado:

1. **Respaldo material externo:** Se generó un respaldo material coordinado (metadatos BD en `database_metadata.jsonl`, objetos binarios en `objects/`, manifiesto de integridad con SHA-256 en `manifest.json` y marcador de cierre `backup.complete`) almacenado fuera del repositorio de código.
2. **Muestra sintética representativa:**
   - Consulta con dos versiones: Versión 1 (`HISTORICO`, `demanda_v1.pdf`) y Versión 2 (`VIGENTE`, `demanda_v2.pdf`, con puntero `referenciaAnteriorId` a V1).
   - Seguimiento (`SEGUIMIENTO`, `acta.txt`, `VIGENTE`).
   - Respuesta (`RESPUESTA`, `respuesta.txt`, `VIGENTE`).
   - Conciliación (`CONCILIACION`, `acuerdo.pdf`, sintético validado con firma `%PDF-1.4`, `VIGENTE`).
3. **Simulación de pérdida catastrófica:** Vaciado total de las tablas y del almacenamiento de objetos activo.
4. **Restauración coordinada:** Reconstrucción exclusiva a partir del respaldo material y verificación de integridad byte a byte, metadatos y tiempos de RPO/RTO.
5. **Controles negativos:** Detección de alteración de bytes (bit-flip) y detección de objetos ausentes.
6. **Vencimiento de URL firmada (QA-DOC-13 / Criterio 3):**
   - En proveedor aislado con validación de expiración: HTTP 200 antes de vencer, HTTP 403 (*Request has expired*) tras transcurrir el TTL, y HTTP 200 con bytes idénticos tras solicitar URL renovada con re-autorización.
   - En LocalStack Community 4.4.0: Se mantiene reportado fielmente como **INCONCLUSIVE / BLOCKED**, dado que dicho emulador comunitario no valida expiración en solicitudes GET, cumpliendo con el criterio de no convertir fallos en PASS por alterar el reporte.

---

## Matriz de casos ejecutados

| ID | Caso de prueba | Criterio de aceptación | Resultado obtenido | Estado |
|---|---|---|---|:---:|
| **QA-REC-01** | Respaldo material coordinado externo | Manifiesto con hashes SHA-256, metadatos BD y copias binarias en directorio externo fuera del repo | Generado en directorio temporal independiente; 5 objetos y metadata serializada; `backup.complete` presente | **PASS** |
| **QA-REC-02** | Simulación de pérdida total | Vaciado completo de tablas activas y almacenamiento de objetos | Confirmado: 0 filas en BD activa y 0 objetos en almacenamiento antes de restaurar | **PASS** |
| **QA-REC-03** | Restauración coordinada de muestra nominal | Restaurar 5 documentos relacionando claves, tipos, tamaños y cadena de versionado V1/V2 | 5 objetos restaurados; Consulta V2 (`VIGENTE`) referencia a V1 (`HISTORICO`) con mismo `documentoLogico` | **PASS** |
| **QA-REC-04** | Integridad byte a byte y SHA-256 | Bytes y hashes coinciden exactamente con los originales antes de la pérdida | `assertArrayEquals` superado al 100% en los 5 archivos; hashes SHA-256 verificados | **PASS** |
| **QA-REC-05** | Cumplimiento de RPO y RTO acordados | RPO <= 24 horas; RTO <= 4 horas | Antigüedad del punto: 5 horas (RPO OK); Tiempo de restauración: 19 ms frente al límite de 4 horas (RTO OK) | **PASS** |
| **QA-REC-06** | Control negativo: alteración de bytes | Detección de discrepancia de checksum y aborto antes de validar | Bit alterado en bytes genera hash distinto; rechazo verificado | **PASS** |
| **QA-REC-07** | Control negativo: archivo faltante en respaldo | Detención de restauración ante ausencia de artefacto del manifiesto | Archivo ausente lanza excepción y no marca el respaldo como íntegro | **PASS** |
| **QA-REC-08** | URL firmada antes de vencimiento | HTTP 200 y entrega de bytes íntegros | HTTP 200 recibido; 100% bytes coincidentes y SHA-256 verificado | **PASS** |
| **QA-REC-09** | URL firmada tras vencimiento en proveedor aislado | HTTP 403 Forbidden con mensaje de expiración | HTTP 403 recibido con código `RequestTimeTooSkewed` / `Request has expired` | **PASS** |
| **QA-REC-10** | Renovación de URL con nueva autorización | HTTP 200 y bytes idénticos a la URL original | HTTP 200 recibido; bytes y SHA-256 idénticos al original | **PASS** |
| **QA-REC-11** | Evaluación en LocalStack Community | LocalStack ignora expiración; no alterar resultado | Se registra formalmente como **INCONCLUSIVE** por limitación del emulador | **INCONCLUSIVE** |
| **QA-REC-12** | Limpieza de ambiente desechable | Eliminación total de directorios y archivos de prueba | Limpieza automática en `tearDown` verificada; cero artefactos residuales | **PASS** |

**Resumen de ejecución:** 11 PASS, 1 INCONCLUSIVE, 0 FAIL.

---

## Manifiesto coordinado de prueba (Sanitizado)

```json
{
  "backup_id": "8fa538e1-5c4a-4e2b-91f8-0f1e828ad941",
  "created_at": "2026-11-02T02:51:19.510Z",
  "object_count": 5,
  "database_dump": "database_metadata.jsonl",
  "objects": [
    {
      "key": "consulta/101/demanda_v1.pdf",
      "sha256": "81f1bcfdf1972b947c664b3df3ba749fe1c6ee38d38cb0973a11306b8da46e7f"
    },
    {
      "key": "consulta/101/demanda_v2.pdf",
      "sha256": "df0c81523d4e8c15ce110ce2ab2ba2cbe67cb1a9959f6fb878db33cf37422fec"
    },
    {
      "key": "seguimiento/202/acta.txt",
      "sha256": "82f808770b2ec3b7337feadba74ff1ef3ae30e224e75d0b98eb7934bb6186b8f"
    },
    {
      "key": "respuesta/303/respuesta.txt",
      "sha256": "5c43d922fc5be5ef80572b9370cb469956fe4549175d40db901170d761665a95"
    },
    {
      "key": "conciliacion/404/acuerdo.pdf",
      "sha256": "3cb0703e3518e2be3fb2623d8c11bb864c3c3a9366e60b2d69957df38c532454"
    }
  ]
}
```

---

## Registro de telemetría y ejecución

```
QA_SCRUM_344_RECOVERY_SUCCESS start=2026-11-02T02:51:19.520753300Z end=2026-11-02T02:51:19.540049684Z durationMs=19 restoredObjects=5 rtoCompliance=PASS
QA_SCRUM_344_URL_EXPIRATION statusBefore=200 statusAfter=403 statusRenewed=200 bytesMatch=true
```

## Reproducibilidad

El ensayo es 100% reproducible y autónomo mediante el script:
```sh
bash docs/testing/ops/run-scrum-344-recovery-qa.sh
```
O directamente con Maven:
```sh
./mvnw -Dtest=DocumentCoordinatedRecoveryQaTest test
```
No requiere servicios externos activos ni expone secretos de configuración.
