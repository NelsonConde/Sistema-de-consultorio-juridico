# SCRUM-369 — Revisión de SCRUM-354

Fecha: 15/10/2026, America/Bogota.
**Resultado: INCONCLUSO / BLOQUEADO por falta de entrega de SCRUM-368.**
Esta revisión preliminar no acredita una corrección ni una prueba funcional
nueva. SCRUM-369 conserva su identidad como revisión independiente de la
implementación asignada a SCRUM-368.

## Fuente y ambiente inspeccionados

- Jira: SCRUM-354, SCRUM-368 y SCRUM-369, con sus criterios y comentarios.
- SCRUM-368 figura en «Tareas por hacer», sin comentarios de entrega.
- `origin` actualizado mediante `git fetch origin --prune`.
- `develop`: `9fbd16320dbd944804d303b6cb1af9b607d74674`.
- Búsqueda de PR abiertos y cerrados por título/rama relacionados con
  `368`, `354`, `student`, `estudiante` o `conciliacion`: sin resultados.
  Esta búsqueda no descarta un cambio con otro nombre o un commit no publicado.
- Revisión estática en checkout aislado; sin conexión a una base de datos,
  servidor de aplicación ni navegador. No se atribuye esa versión al despliegue.

## Hallazgo estático vigente

En `backend/app/src/main/java/co/edu/ufps/legal_cases/business/service/perfil/estudiante/EstudianteCommandService.java`,
el método `crear` conserva:

```java
DatosEstudiante datos = prepararDatos(dto, true, false);
```

`prepararDatos` recibe el valor de conciliación como argumento y construye
`DatosEstudiante` con ese valor. `EstudianteMapper.aplicarDatos` lo copia a la
entidad y `convertirADTO` devuelve el valor de la entidad. El DTO sí declara
`Boolean conciliacion`. En esta ruta de código el valor solicitado no gobierna
el alta: continúa la causa descrita en SCRUM-354.

Esto es evidencia estática del código disponible, **no una ejecución HTTP ni
una comprobación de persistencia**. No se presenta el reporte histórico del
compañero como una reproducción nueva.

## Matriz de aceptación pendiente

| Criterio de SCRUM-354 | Situación y verificación requerida | Resultado actual |
|---|---|---|
| Alta autorizada con true | El servicio disponible fuerza false. Tras la entrega, comprobar respuesta, BD, detalle, listado y elegibilidad real para conciliación. | Discrepancia estática; aceptación funcional INCONCLUSA |
| false, ausencia y null | El código inspeccionado fuerza false para todos, pero falta la semántica documentada de la corrección y sus pruebas. Separar los tres payloads. | INCONCLUSO |
| Payload sin permiso | El controller exige permiso de alta/gestión y el servicio restringe la gestión al administrador. Falta ejecutar solicitudes manipuladas y comprobar cero efectos persistentes. | INCONCLUSO; no se declara PASS por lectura |
| Regresiones | La actualización general conserva la conciliación existente; hay una operación específica de cambio. Falta la evidencia de actualización, cambio específico e importación Excel sobre el artefacto corregido. | INCONCLUSO |
| Recorrido FE–API–listado–selector | El listado presenta `estudiante.conciliacion` y el selector usa `/estudiantes/conciliacion/paginados`. Falta ejecutar el recorrido con fixtures del cambio. | INCONCLUSO |

## Casos listos para ejecutar cuando se entregue SCRUM-368

1. Administrador autorizado: alta sintética con true y candidato elegible;
   contrastar solicitud, respuesta, fila, GET de detalle, listado y selector.
2. Repetir con false, propiedad ausente y null según la regla documentada;
   comprobar consistencia y ausencia de habilitación no solicitada.
3. Sin sesión y con usuario sin permiso: enviar manualmente true con el control
   CSRF apropiado; comprobar rechazo y ausencia de cuenta/perfil/efectos.
4. Actualización general, cambio específico de conciliación e importación Excel:
   verificar que la corrección no cambia sus contratos ni registros existentes
   por inferencia.
5. Mantener las demás condiciones de elegibilidad y alcance: true por sí solo
   no debe introducir estudiantes ajenos o inactivos en el selector.

La ejecución deberá registrar commit, versiones, ambiente aislado, comandos,
resultados PASS/FAIL/INCONCLUSO, muestra y límites. Solo datos sintéticos;
evidencia sin contraseñas, cookies, JWT ni credenciales. El equipo implementador
y QA ejecutan estas comprobaciones, sin trasladarlas al solicitante como auditor.

## Bloqueo y acción necesaria

- Causa: falta fuente de la corrección y evidencia de ejecución de SCRUM-368.
- Responsable de la entrega: Edwin Geanfranco Torres Zapata, según Jira.
- Revisión QA: José Manuel Pérez Rodríguez, asistido por Codex en esta revisión.
- Para desbloquear: recibir commit/rama/PR exacto de SCRUM-368, casos y resultados
  de implementación y semántica de false/null/ausencia. Si ya existe con otra
  referencia, identificarla antes de atribuirle esta revisión.
- Nueva previsión: pendiente de recibir y evaluar la entrega; no se inventa fecha
  ni se modifica el calendario oficial.

No se modifica la aplicación para suplir SCRUM-368 desde la revisión QA. No se
ejecutaron suites ni se reutilizaron pruebas/CI anteriores como evidencia del
cambio pendiente. Ni SCRUM-369 ni SCRUM-354 quedan aprobadas o cerradas.
