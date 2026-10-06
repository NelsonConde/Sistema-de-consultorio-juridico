# Evidencia de Pruebas QA — Consolidar Datos y Generar Ficha PDF Autorizada (PB-35 / CC-OPT-004)

- **Solicitud / Épica:** CC-OPT-004 — Consolidación de datos y generación de expediente en formato PDF
- **Ticket Jira:** SCRUM-307 — BE — Consolidar datos y generar ficha PDF autorizada (PB-35)
- **Rama:** `feature/cc-opt-004-expediente-pdf`
- **Fecha de ejecución:** 03/10/2026
- **Responsable:** Equipo de Desarrollo Backend y QA

---

## 1. Alcance y Requisitos del Ticket

Implementación y verificación de la consulta de datos vigentes y generación en memoria de la ficha de expediente PDF bajo demanda para el caso legal (PB-35).

### Invariantes y Criterios de Aceptación
1. **Consolidación completa de datos vigentes:**
   - **Identificación:** Código de caso/radicado, título, fecha de recepción/solicitud, sede de atención.
   - **Estado y resultado:** Estado actual (`RECIBIDA`, `EN_PROCESO`, `CERRADA`), fecha de cierre, motivo de cierre y observaciones.
   - **Partes:** Solicitante principal (con datos sociodemográficos y de contacto), personas adicionales y contrapartes/demandados.
   - **Clasificación:** Área de derecho, tema, subtema y descripción de los hechos.
   - **Responsables:** Estudiante asignado, Asesor docente asignado y Monitor de apoyo.
   - **Seguimientos:** Registro cronológico de actuaciones, tipo de actuación, descripción, fecha y respuestas asociadas con su estado de aprobación y observaciones.
   - **Proceso judicial y conciliación:** Radicado judicial, juzgado/despacho, tipo de proceso, estado procesal, o bien radicado de conciliación, fecha de audiencia y resultado de la conciliación.
   - **Índice documental:** Inventario cronológico de anexos y documentos vigentes asociados a la consulta y sus actuaciones, reflejando identificador seguro, nombre de archivo, tamaño legible, fecha de carga y remitente.
2. **Validación de acceso previa a la lectura (Fail-Fast):**
   - Se valida la autorización de acceso mediante `ConsultaAccessService.validarPuedeVerConsulta(consultaId)` antes de realizar cualquier lectura en la base de datos o almacenamiento.
   - Intentos por parte de terceros no autorizados son rechazados con HTTP 403 (`Forbidden`).
   - Peticiones no autenticadas son rechazadas con HTTP 401 (`Unauthorized`).
3. **Seguridad y almacenamiento en memoria (Zero-Storage Leakage):**
   - La generación del PDF se realiza estrictamente en memoria (utilizando `ByteArrayOutputStream` con iText 7).
   - No se persiste ninguna copia física temporal ni permanente en disco ni en buckets de almacenamiento (S3/Supabase Storage).
   - No se exponen rutas internas de almacenamiento, nombres de bucket ni object keys (`objectKey`).
4. **Auditoría inmutable:**
   - Se registra evento de auditoría con la acción `GENERAR_FICHA_EXPEDIENTE_PDF` y `CONSULTAR_FICHA_EXPEDIENTE`, registrando resultado exitoso (`SUCCESS`) o denegado (`DENIED`).

---

## 2. Matriz de Pruebas

| ID | Escenario de Prueba | Nivel | Entrada / Contexto | Resultado Esperado | Estado |
| :--- | :--- | :---: | :--- | :--- | :---: |
| **TC-PDF-01** | Generación exitosa de PDF con datos completos | Unit / Service | Consulta existente (ID: 10) con todas las secciones pobladas (partes, seguimientos, proceso, conciliación, índice documental). | PDF generado en memoria (`byte[]`), encabezado `%PDF-1.7`, sin excepciones. | **PASSED** |
| **TC-PDF-02** | Generación exitosa de PDF con campos opcionales nulos | Unit / Service | Consulta existente con campos opcionales nulos (sin proceso, sin conciliación, sin monitor, sin seguimientos). | PDF generado correctamente mostrando etiquetas de fallback ("No asignado", "No registra"). | **PASSED** |
| **TC-PDF-03** | Rechazo de acceso a tercero no autorizado | Unit / Security | Usuario autenticado sin permiso sobre la consulta (`validarPuedeVerConsulta` arroja `AccessDeniedException`). | Se detiene la ejecución antes de leer datos; se propaga `AccessDeniedException` (HTTP 403). | **PASSED** |
| **TC-PDF-04** | Consulta inexistente | Unit / Service | ID de consulta inexistente (ID: 999). | Se lanza `RecursoNoEncontradoException` (HTTP 404). | **PASSED** |
| **TC-PDF-05** | ID de consulta nulo o inválido | Unit / Service | ID `null`. | Se lanza `ValidacionNegocioException` (HTTP 400). | **PASSED** |
| **TC-PDF-06** | Integración con aspecto de auditoría (`AuditAspect`) | Integration / AOP | Ejecución de `generarFichaPdf` y generación de evento de auditoría. | `AuditAspect` intercepta la llamada y persiste log con acción `GENERAR_FICHA_EXPEDIENTE_PDF` y resultado `SUCCESS`. | **PASSED** |
| **TC-PDF-07** | Auditoría en intento denegado | Integration / AOP | Intento de generación de ficha con acceso denegado. | `AuditAspect` intercepta la excepción y persiste log con resultado `DENIED`. | **PASSED** |
| **TC-PDF-08** | Endpoint REST `GET /api/consultas/{id}/pdf` | WebMvc / MockMvc | Usuario con autoridad `VER_CONSULTAS` solicita PDF. | HTTP 200 OK, `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="ficha-expediente-{id}.pdf"`, contenido binario `%PDF-`. | **PASSED** |
| **TC-PDF-09** | Alias REST `GET /api/consultas/{id}/ficha-pdf` | WebMvc / MockMvc | Solicitud al endpoint canónico alternativo. | HTTP 200 OK, `Content-Type: application/pdf`. | **PASSED** |
| **TC-PDF-10** | Endpoint JSON `GET /api/consultas/{id}/ficha` | WebMvc / MockMvc | Usuario con autoridad `VER_CONSULTAS` solicita DTO consolidado. | HTTP 200 OK, `Content-Type: application/json`, payload con las 7 secciones estructuradas. | **PASSED** |
| **TC-PDF-11** | Endpoint REST rechaza tercero (HTTP 403) | WebMvc / Security | Petición sobre consulta no autorizada. | HTTP 403 Forbidden con formato estándar `ErrorResponseDTO`. | **PASSED** |
| **TC-PDF-12** | Seguridad por método con autoridad `GESTIONAR_CONSULTAS` | WebMvc / Security | Usuario con autoridad `GESTIONAR_CONSULTAS`. | HTTP 200 OK permitido. | **PASSED** |
| **TC-PDF-13** | Seguridad por método rechaza usuario sin autoridad | WebMvc / Security | Usuario con autoridad ajena `ROLE_USER`. | HTTP 403 Forbidden. | **PASSED** |
| **TC-PDF-14** | Seguridad por método rechaza sesión anónima | WebMvc / Security | Petición sin token ni cookie de sesión. | HTTP 401 Unauthorized. | **PASSED** |

---

## 3. Evidencia de Ejecución de Pruebas

### 3.1. Pruebas Unitarias y de Servicio (`FichaExpedienteServiceTest`)
```text
[INFO] Running co.edu.ufps.legal_cases.business.service.consulta.pdf.FichaExpedienteServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.941 s -- in co.edu.ufps.legal_cases.business.service.consulta.pdf.FichaExpedienteServiceTest
```
- `generarFichaPdf_conDatosCompletos_retornaBytesPdfValidos()`: **PASSED**
- `generarFichaPdf_conCamposOpcionalesNulos_generaPdfCorrectamente()`: **PASSED**
- `generarFichaPdf_accesoDenegado_noLeeDatosYLanzaExcepcion()`: **PASSED**
- `generarFichaPdf_consultaNoExiste_lanzaRecursoNoEncontradoException()`: **PASSED**
- `generarFichaPdf_idNulo_lanzaValidacionNegocioException()`: **PASSED**
- `generarFichaPdf_tieneAnotacionAuditable()`: **PASSED**
- `obtenerFichaExpediente_tieneAnotacionAuditable()`: **PASSED**
- `generarFichaPdf_conAuditAspect_registraEventoAuditoria()`: **PASSED**
- `generarFichaPdf_conAuditAspect_cuandoAccesoDenegado_registraAuditoriaDenied()`: **PASSED**

### 3.2. Pruebas de Integración Web MVC (`ConsultaControllerFichaPdfTest`)
```text
[INFO] Running co.edu.ufps.legal_cases.business.controller.consulta.ConsultaControllerFichaPdfTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.354 s -- in co.edu.ufps.legal_cases.business.controller.consulta.ConsultaControllerFichaPdfTest
```
- `descargarFichaPdf_usuarioAutorizado_retornaPdfAttachment()`: **PASSED** (200 OK, application/pdf, header Content-Disposition)
- `descargarFichaPdf_aliasEndpoint_retornaPdfAttachment()`: **PASSED** (200 OK)
- `obtenerFicha_usuarioAutorizado_retornaJsonFicha()`: **PASSED** (200 OK, application/json)
- `descargarFichaPdf_accesoDenegado_retorna403()`: **PASSED** (403 Forbidden)
- `descargarFichaPdf_errorValidacion_retorna400()`: **PASSED** (400 Bad Request)

### 3.3. Pruebas de Autorización y Seguridad (`ConsultaControllerAuthorizationTest`)
```text
[INFO] Running co.edu.ufps.legal_cases.business.controller.consulta.ConsultaControllerAuthorizationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.165 s -- in co.edu.ufps.legal_cases.business.controller.consulta.ConsultaControllerAuthorizationTest
```
- `descargarFichaPdf_conPermisoVerConsultas_permiteAcceso()`: **PASSED**
- `descargarFichaPdf_conPermisoGestionarConsultas_permiteAcceso()`: **PASSED**
- `descargarFichaPdf_sinPermisosRequeridos_retorna403()`: **PASSED**
- `descargarFichaPdf_sinAutenticacion_retorna401()`: **PASSED**

---

## 4. Evidencia de Respuestas HTTP y Headers

### 4.1. Respuesta Exitosa de Descarga de PDF (`application/pdf`)
```http
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="ficha-expediente-10.pdf"
Content-Length: 14820
X-Content-Type-Options: nosniff
Cache-Control: no-cache, no-store, max-age=0, must-revalidate

%PDF-1.7
%
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
...
```

### 4.2. Respuesta de Rechazo ante Acceso no Autorizado (HTTP 403 Forbidden)
```http
HTTP/1.1 403 FORBIDDEN
Content-Type: application/json

{
  "estado": 403,
  "error": "Forbidden",
  "mensaje": "No tiene permisos para ver o generar la ficha del caso legal solicitado."
}
```

### 4.3. Respuesta sin Autenticación (HTTP 401 Unauthorized)
```http
HTTP/1.1 401 UNAUTHORIZED
Content-Type: application/json

{
  "estado": 401,
  "error": "Unauthorized",
  "mensaje": "Acceso no autenticado"
}
```

---

## 5. Verificación de Invariantes de Seguridad y No Exposición

1. **Almacenamiento:**
   - La clase `FichaExpedientePdfService` opera directamente sobre un `java.io.ByteArrayOutputStream`.
   - Ningún `File`, `FileOutputStream`, ni cliente de almacenamiento remoto (`SupabaseStorageService`) es invocado durante la generación o entrega de la ficha.
2. **Índice Documental Protegido:**
   - El DTO `DocumentoIndiceDTO` solo expone:
     - `id`: Identificador numérico del FileAsset.
     - `nombreArchivo`: Nombre visible del archivo.
     - `recursoOrigen`: Tipo de recurso asociado (`CONSULTA_ANEXO`, `SEGUIMIENTO_ANEXO`).
     - `tamañoFormateado`: Representación legible para humanos (e.g. `2.50 MB`, `150.00 KB`).
     - `fechaCarga`: Fecha y hora de carga.
     - `cargadoPor`: Nombre o identificador del usuario que realizó la carga.
   - **No se exponen:** Ni el nombre del bucket (`bucket`), ni la clave de objeto interna (`objectKey`), ni URLs pre-firmadas en la ficha.
3. **Auditoría:**
   - Evento registrado:
     - `action`: `GENERAR_FICHA_EXPEDIENTE_PDF` / `CONSULTAR_FICHA_EXPEDIENTE`
     - `entityName`: `Consulta`
     - `entityId`: `#consultaId`
     - `outcome`: `SUCCESS` (en descarga) / `DENIED` (en rechazo)

---

## 6. Conclusión

El ticket **SCRUM-307 (PB-35 / CC-OPT-004)** cumple al 100% con todos los criterios de aceptación técnicos y de seguridad:
- Consulta y consolidación ordenada de todos los datos vigentes del expediente.
- Generación de PDF en memoria con maquetación institucional y manejo robusto de valores nulos/opcionales.
- Rechazo inmediato de terceros y usuarios anónimos antes de cualquier operación de I/O.
- Cero persistencia física y cero exposición de rutas/claves internas.
- Cobertura de pruebas unitarias, de integración, de seguridad y auditoría en estado **PASSED**.
