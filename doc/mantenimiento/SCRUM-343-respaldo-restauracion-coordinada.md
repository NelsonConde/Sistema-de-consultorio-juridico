# SCRUM-343 — Respaldo y restauración coordinada de PostgreSQL y Storage

**Estado: propuesta controlada; aceptación INCONCLUSA.**
Fecha de propuesta: 15/10/2026. Actualización: 01/11/2026, America/Bogota.
Fuente técnica actual inspeccionada: `develop` en
`21d752b66ed574f1be2f646cb21052fc74242bcd`.
Este documento prepara el procedimiento para revisión; no acredita un respaldo
material nuevo, una política aprobada ni una restauración ejecutada.

## Dependencias y responsables

SCRUM-362 figura Finalizada en Jira al 01/11/2026. Se elimina la espera por
su ejecución. El estado de esa subtarea no aprueba los valores de política de
este documento; el formato entregado debe cotejarse al consolidar el paquete. SCRUM-332 definirá el
artefacto migrable usado en la restauración; no se ensaya una estrategia de
migración sin la validación previa de SCRUM-331.

Ejecutor y custodio propuesto: José Manuel Pérez Rodríguez (operación/BD).
Revisores: Nelson Enrrique Conde Tarazona y Edwin Geanfranco Torres Zapata.
El custodio debe identificar un suplente y el dueño debe aprobar destino,
retención y objetivos antes de operar. No se suplantan aprobaciones.

## Política propuesta para aprobación

| Control | Valor propuesto | Estado |
|---|---|---|
| Frecuencia | Respaldo coordinado diario, 02:00 America/Bogota; adicional antes de migraciones | Pendiente |
| Retención | 7 diarios, 4 semanales y 3 mensuales completos y verificables | Pendiente |
| RPO | Máximo 24 horas desde el último punto coordinado recuperable | Pendiente |
| RTO | Máximo 4 horas desde declarar la pérdida hasta validar y habilitar el servicio | Pendiente |
| Destino | Cuenta/proyecto de respaldo independiente del PostgreSQL y bucket activos; almacenamiento cifrado y acceso restringido | Proveedor, identificador y permisos pendientes |
| Custodia | Ejecutor de operación/BD, suplente designado y revisión independiente | Aceptación del custodio y suplente pendientes |
| Alertas | Alertar al fallar un paso; vencimiento del respaldo si no hay uno íntegro en 24 h; verificar por monitor independiente | Receptor y canal aprobados pendientes |
| Expurgo | Solo conjuntos completos vencidos; conservar al menos un conjunto íntegro recuperable; no propagar borrados del origen | Pendiente |

Los valores son una propuesta concreta para evaluar costo, tamaño y ventana de
indisponibilidad. No constituyen una promesa de capacidad del proveedor ni de
restauración de producción. Si no cabe el respaldo consistente en la ventana
admisible, reestimar y adoptar snapshot/PITR coordinado; no omitir la consistencia.

## Punto consistente y contenido del respaldo

`pg_dump` y una copia de objetos concurrente no garantizan por sí solos un
estado coordinado. Propuesta inicial: ventana de mantenimiento con exclusión
de escritores (API, jobs, reconciliación y cargas directas mediante URLs ya
emitidas). Confirmar que las transferencias iniciadas terminaron o fueron
canceladas; bloquear nuevas escrituras en el proveedor durante la copia. No
dar por suficiente detener solo el backend.

1. Registrar `backup_id`, commit, ambiente sintético/autorizado, versiones de
   PostgreSQL, `pg_dump`, `pg_restore`, cliente S3 y runtime, hora UTC y Bogotá.
2. Establecer y verificar la barrera de escritura; registrar inicio del punto
   consistente. Si no se logra, fallar sin publicar el conjunto como válido.
3. Obtener `pg_dump --format=custom` completo y un inventario de metadata dentro
   de la misma ventana. Conservar cuentas y relaciones necesarias para restaurar
   el servicio; el ensayo usa exclusivamente identidades sintéticas.
4. Inventariar todos los objetos de los buckets incluidos, con paginación;
   copiar los bytes y metadata al destino independiente. Comparar contra
   `file_asset`. Incluir versiones históricas retenidas; cualquier objeto sin
   fila o fila sin objeto se informa y clasifica. No ignorarlos silenciosamente.
5. Recalcular SHA-256 de bytes leídos. No usar ETag como sustituto: puede depender
   de multipart u otras características del proveedor.
6. Verificar el dump mediante `pg_restore --list`, verificar manifiestos y
   completar lectura desde el destino externo. Publicar el marcador de respaldo
   íntegro únicamente después de pasar todas las comprobaciones.
7. Liberar la barrera aun si hubo fallo, registrar estado y emitir alerta. Un
   conjunto parcial se conserva identificado para diagnóstico, sin contarlo en
   RPO ni reemplazar el último respaldo íntegro.

La pausa es una propuesta que requiere aprobar su impacto. Una alternativa sin
pausa debe especificar coordinación temporal, versionado de objetos y snapshot
BD compatibles, y demostrarla antes de sustituir este mecanismo.

## Manifiesto mínimo

Un conjunto contiene `database.dump`, `database.sha256`, `objects.jsonl`,
`objects.sha256`, `run.json` y un marcador final de integridad. Las claves de
objetos se almacenan sin interpolarlas como rutas locales, evitando colisiones
y traversal. Un nombre local puede ser SHA-256(bucket + separador + object_key);
el manifiesto conserva la correspondencia exacta.

Por fila/objeto registrar: `file_asset.id`, `bucket`, `object_key`,
`resource_type`, `resource_id`, `documento_logico`, `version`,
`referencia_anterior_id`, `status`, `content_type`, `size`, `checksum`, SHA-256
observado, versión de objeto si el proveedor la expone y estado de copia.
Las columnas proceden de `FileAsset`; las relaciones de recurso necesitan
validación adicional porque no todas son FK físicas.

`run.json` incluye tiempos, conteos, lista de faltantes/extras, versiones de
herramientas, commit, SHA-256 de cada artefacto y resultado. No guarda passwords,
cookies, tokens, URL firmadas ni URLs JDBC con credenciales. Los manifiestos
reales quedan bajo acceso restringido; a Git/PR solo va evidencia sintética y
sanitizada. Las firmas y URLs de acceso nunca se imprimen en comandos/logs.

## Automatización que debe entregarse tras aprobar la política

El job debe tener exclusión mutua, timeout, propagación de códigos de salida,
validación del destino independiente, `umask 077`, secretos inyectados y nunca
`set -x`. No usar `sync --delete` contra el histórico. Un proceso independiente
comprueba antigüedad y presencia del marcador íntegro: la falta de ejecución
también debe alertar.

La programación propuesta es `0 2 * * *` con zona **America/Bogota** expresamente
soportada por el scheduler seleccionado; no asumir que el host usa esa zona.
Registrar el identificador de la tarea, revisión del script, inicio/fin y
ubicación del conjunto. El ejecutable y configuración finales quedan pendientes
del destino y canal aprobados; no se publica una plantilla como job ya desplegado.

Ensayo obligatorio: destino sintético denegado o interrupción deliberada del
comando de copia; salida no cero, sin marcador de integridad, conservación del
último conjunto válido y alerta efectivamente recibida en el canal de prueba.
Una impresión en consola no acredita recepción de alerta.

## Restauración en ambiente desechable — SCRUM-344

1. Registrar proveedor, proyecto/BD/bucket aislados, autorización y lista exacta
   de recursos que se crearán/eliminarán. No reutilizar destinos de producción.
2. Verificar hashes del conjunto externo, permisos de lectura y versión de
   herramientas. Restaurar con `pg_restore --exit-on-error --no-owner` en una
   base vacía creada para el ensayo; ajustar roles mediante un mapeo revisado.
   No ejecutar `--clean` contra un destino compartido.
3. Restaurar objetos con sus claves y `Content-Type`. Si se cambia de bucket,
   aplicar un mapeo explícito y coherente de metadata a **todas** las filas
   restauradas de la muestra; registrar el mapeo y no alterar el origen.
4. Comparar claves, filas, relaciones, versiones, estados, tamaño, tipo y bytes;
   recalcular SHA-256. Comprobar invariantes, triggers de inmutabilidad y
   `ddl-auto=validate` usando el commit restaurado. Ninguna fila/objeto faltante
   puede convertirse en PASS por reducir después la muestra.
5. Medir antigüedad del punto restaurado contra RPO y tiempo completo desde la
   pérdida simulada hasta servicio validado contra RTO, incluyendo descargas,
   verificaciones y arranque. Informar tamaño/muestra y límites.
6. Probar autorización, emitir una URL de TTL corto sin publicarla, consumirla
   antes y después de su vencimiento real en el proveedor aislado. Obtener otra
   URL mediante autorización y contrastar bytes. LocalStack no acredita por sí
   solo el rechazo posterior al vencimiento: mantener ese resultado inconcluso
   si el proveedor no lo aplica.
7. Eliminar solo recursos del ensayo identificados al inicio; verificar ausencia
   de contenedores, BD y objetos temporales. Conservar evidencia sanitizada y el
   respaldo según su política. No borrar copias válidas como limpieza de ensayo.

## Muestra sintética prevista y criterios de fallo

Preparar como mínimo una consulta con dos versiones documentales, un seguimiento,
una respuesta y una conciliación con PDF sintético; referencias válidas a actores
sintéticos. Incluir objeto ausente, bytes alterados y metadata de tipo/tamaño
incorrectos como controles negativos en conjuntos separados. El conjunto nominal
debe restaurarse íntegro; los negativos deben fallar con causa identificable.

La muestra está **pendiente de creación y respaldo material**. Un manifiesto de
ejemplo vacío no se acepta como ejecución. Son fallos: dump inválido, hash
distinto, falta de fila/objeto, relaciones rotas, incompatibilidad de esquema,
destino no independiente, falta de alerta o incumplimiento de RPO/RTO.

## Estado de aceptación y acciones requeridas

| Criterio | Resultado de esta entrega |
|---|---|
| Frecuencia, retención, RPO/RTO, custodio y destino | Propuesta numérica preparada; aprobación y destino pendientes |
| Procedimiento coordinado y manifiesto | Diseño preparado; muestra material pendiente |
| Respaldo programado y alerta ensayada | INCONCLUSO: sin ejecución ni canal aprobado |
| Restauración y limpieza aisladas | Procedimiento preparado; ejecución corresponde a SCRUM-344 tras sus predecesoras |

Para continuar se requiere validación de la política por operación/BD y
revisores, destino independiente con permisos restringidos y canal de alerta
de prueba. SCRUM-362 ya figura Finalizada; no se mantiene como bloqueo de
ejecución. SCRUM-342 también figura Finalizada, pero su evidencia de rotación
se consolida por separado en SCRUM-341. SCRUM-343 no se cierra y SCRUM-344 no se acepta hasta
recibir la evidencia. No se reabren SCRUM-260/285/300/313 ni se atribuye una
recuperación completa a la muestra histórica de un objeto de 91 bytes.

Este documento es una propuesta versionada; no actualiza fuentes oficiales de
Drive ni acredita rotación de credenciales o copias antiguas.


## Hoja de entrega para aprobar y operar — 01/11/2026

La propuesta ya contiene valores numéricos. Los campos siguientes permiten al
responsable aceptar o modificar esos valores sin confundir una propuesta con
una aprobación. No incluir credenciales ni URLs firmadas.

| Dato que debe entregar el equipo | Responsable / aceptación | Situación |
|---|---|---|
| Frecuencia, retención y ventana de escritura | José Manuel; revisión Nelson/Edwin | Valores propuestos arriba; falta aprobación identificable |
| RPO de 24 h y RTO de 4 h, o valores sustitutos | Operación/BD y revisores | Falta aprobación |
| Proveedor/proyecto de respaldo independiente y recursos aislados de ensayo | Custodio técnico autorizado | Falta identificador no secreto y comprobación de acceso |
| Custodio suplente y procedimiento de acceso | Custodio y suplente | Falta designación |
| Receptor y canal aislado de alerta, con acuse verificable | Operación | Falta destino de prueba |
| Scheduler y soporte explícito de zona horaria | Operación | Falta plataforma seleccionada |
| Artefacto de BD y decisión de migraciones aceptada | SCRUM-331/332 | 331 en revisión; 332 no aceptada |

### Secuencia de ejecución preparada

1. Registrar aprobador, fecha, enlace de revisión y valores aceptados.
2. Identificar origen y destino aislados; probar que la cuenta de respaldo no
   depende de la disponibilidad del servicio activo. Una segunda carpeta del
   mismo equipo no acredita independencia del proveedor.
3. Crear la muestra sintética descrita arriba y registrar sus relaciones antes
   del respaldo. No sustituir el esquema de aplicación por una tabla de ejemplo.
4. Ejecutar una vez el job con bloqueo de escritores; conservar dump legible,
   manifiestos no vacíos, hashes y marcador final únicamente si todo pasa.
5. Ejecutar el fallo inducido y demostrar recepción en el canal de prueba.
6. Registrar el scheduler, su zona horaria y el monitor de ausencia de ejecución.
7. Entregar el conjunto y el acta a SCRUM-344 para restauración independiente.

### Acta mínima de cada ejecución

Registrar `backup_id`, SHA del código y script, versión de PostgreSQL y
herramientas, ambiente, inicio/fin UTC, barrera de escritura, destino, conteos,
resultado de lectura de dump y objetos, manifiesto SHA-256, resultado de alerta,
limpieza y revisor. Si un paso falla, registrar FAIL/INCONCLUSO y no emitir el
marcador íntegro. Los logs publicados deben estar sanitizados.

Esta actualización es documental. No se instaló un scheduler ni se operó un
respaldo de servicio: faltan las decisiones y recursos indicados. No se acredita
RPO/RTO, recepción de alerta, restauración ni vencimiento real de URL.
