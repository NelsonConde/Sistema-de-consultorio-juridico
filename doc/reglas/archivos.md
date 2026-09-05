# Reglas de negocio - Gestión Documental y Archivos

> Documento ajustado contra el código fuente actual y las migraciones Flyway V23, V25 y V26. Describe las reglas de negocio, integridad, autorización y versionamiento del almacenamiento documental.

---

## 1. Regla general de almacenamiento

Los archivos se gestionan como activos documentales (`FileAsset`) asociados exclusivamente a entidades de negocio del dominio (`CONSULTA`, `SEGUIMIENTO`, `RESPUESTA`, `PROCESO`, `CONCILIACION`).
Bajo ninguna circunstancia se permite el uso de rutas libres del sistema operativo, claves de bucket arbitrarias suministradas por el cliente o almacenamiento sin un recurso funcional propietario.

---

## 2. Autenticación, autorización y alcance institucional

1. Toda operación documental requiere usuario autenticado con sesión activa.
2. `FileResourceAuthorizationService` delega la validación de alcance en el servicio correspondiente al recurso:
   - `CONSULTA`: `ConsultaAccessService.validarPuedeVerConsulta(consultaId)`.
   - `SEGUIMIENTO`: Verificación contra la consulta raíz del seguimiento.
   - `RESPUESTA`: Verificación contra el seguimiento y la consulta raíz asociada.
   - `PROCESO`: `ProcesoAccessService` verificando la consulta origen.
   - `CONCILIACION`: Verificación contra la consulta jurídica remitida a conciliación.
3. Se rechaza con `403 Forbidden` cualquier intento de carga, descarga, listado o versionamiento sobre expedientes donde el usuario no participe como estudiante asignado, asesor supervisor, monitor o rol directivo.
4. No existen listados globales de archivos: todo acceso está delimitado por el expediente o el recurso funcional.

---

## 3. Versionamiento e inmutabilidad documental

1. **Documento Lógico**: Cada documento posee un identificador inmutable (`documento_logico` tipo UUID) que agrupa sus distintas versiones a lo largo del tiempo.
2. **Numeración secuencial**:
   - La primera carga de un documento se registra con `version = 1`.
   - Cada reemplazo o nueva versión incrementa estrictamente el contador a $N+1$.
   - El número de versión se calcula exclusivamente en el servidor bajo bloqueo transaccional.
3. **Unicidad de versión vigente**:
   - En todo momento debe existir a lo sumo **una única versión en estado `VIGENTE`** para un mismo `documento_logico`.
   - Esta regla está garantizada a nivel de base de datos mediante el índice único parcial PostgreSQL `uk_file_asset_doc_vigente` (`WHERE status = 'VIGENTE'`).
4. **Inmutabilidad histórica**:
   - Al activarse una versión $N+1$, la versión anterior $N$ pasa automáticamente a estado `HISTORICO`.
   - Las versiones en estado `HISTORICO` son inmutables: no pueden ser editadas ni sobrescritas.
   - Se mantiene la referencia a la versión inmediatamente anterior mediante `referencia_anterior_id`.
5. **Baja lógica**:
   - La eliminación de un documento no borra físicamente el registro histórico ni destruye la evidencia procesal.
   - Se marca `active = false` y `status = 'ANULADO'` bajo registro de auditoría.

---

## 4. Trazabilidad de autoría y origen

1. **Autoría protegida en servidor**:
   - El usuario responsable de la carga (`uploaded_by_id`) se toma obligatoriamente del contexto de seguridad de Spring Security (`SecurityUtils.getCurrentUser()`).
   - El cliente no puede inyectar, falsear ni manipular el autor del documento.
2. **Origen del documento**:
   - Se clasifica estrictamente en `CARGA_USUARIO` (interactivo), `SISTEMA` (generado por servicios internos) o `MIGRADO` (cargas heredadas).
   - Es calculado por la lógica de negocio del servidor.

---

## 5. Idempotencia y control de concurrencia

1. **Flujo de carga en dos fases**:
   - Fase 1 (`initiate`): Se crea el registro en estado `PENDING` y se genera una clave física no colisionable en staging.
   - Fase 2 (`complete`): Se verifica la presencia física del objeto, se valida su tamaño y se calcula/compara el checksum SHA-256 antes de promover a `VIGENTE`.
2. **Idempotencia de finalización**:
   - Si el cliente reintenta enviar la petición `/complete` con un `uploadId` que ya fue completado previamente, el servidor devuelve el mismo resultado exitoso sin duplicar registros ni alterar el estado.
3. **Control de concurrencia**:
   - Al iniciar una nueva versión, el backend adquiere un bloqueo pesimista sobre el documento lógico vigente para evitar que dos cargas concurrentes compitan por la versión $N+1$.
4. **Reconciliación y limpieza**:
   - Las cargas en estado `PENDING` que no se confirmen dentro de la ventana de expiración o que fallen son marcadas como `FAILED` o `DELETE_PENDING` por el servicio reconciliador (`FileAssetReconciliationService`), eliminando los bytes huérfanos del almacenamiento.

---

## 6. Consulta documental agregada por expediente

1. Endpoint oficial: `GET /api/consultas/{consultaId}/expediente/archivos`.
2. Consolida en un solo acervo los documentos de `CONSULTA`, `SEGUIMIENTO`, `RESPUESTA`, `PROCESO` y `CONCILIACION`.
3. Ejecuta una única consulta optimizada (evitando el problema de consulta N+1).
4. Filtros admitidos:
   - `tipoDocumental`
   - `resourceType`
   - `origen`
   - `autor` (username o ID)
   - `fechaDesde` y `fechaHasta` (validadas para que `fechaDesde <= fechaHasta`).
5. Ordenamiento obligatorio: `createdAt DESC, id DESC`.
6. Respuestas transparentes y seguras: nunca se revelan `bucket` ni `objectKey`.

---

## 7. Preservación documental ante cierre de consulta (RF19)

1. Al ejecutarse el cierre formal de una consulta jurídica:
   - Se valida que no existan procesos judiciales, conciliaciones, seguimientos o respuestas en estado pendiente.
   - El acervo probatorio del expediente queda congelado: no se admiten nuevas cargas o versiones sobre la consulta cerrada, garantizando la inmutabilidad de las pruebas del caso.
