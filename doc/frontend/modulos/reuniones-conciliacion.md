# Frontend - Módulo de reuniones de conciliación

## Propósito

El módulo de reuniones permite programar y reprogramar la reunión vigente de una conciliación. Está integrado en la ruta `/conciliaciones` como pestaña de trabajo independiente.

Componente principal:

```text
src/components/forms/conciliacion/ReunionesConciliacionForm.jsx
```

---

## Ubicación

Página:

```text
src/app/(dashboard)/conciliaciones/page.js
```

Ruta:

```text
/conciliaciones
```

---

## Permisos usados

| Permiso | Uso frontend |
|---|---|
| `Acceder conciliaciones` | Permite entrar al módulo. |
| `Ver conciliaciones` | Permite listar y consultar detalle. |
| `Programar reuniones de conciliación` | Habilita programación cuando no existe reunión. |
| `Reprogramar reuniones de conciliación` | Habilita reprogramación cuando ya existe reunión. |

El componente evita habilitar programación o reprogramación para estudiantes. El backend conserva la autorización definitiva mediante permisos y alcance.

---

## Carga inicial

El componente carga:

```text
GET /api/auth/me
GET /api/conciliaciones
GET /api/sedes
```

Las conciliaciones alimentan el listado de selección. Las sedes activas alimentan el selector de sede de reunión.

---

## Endpoints consumidos

| Acción | Endpoint |
|---|---|
| Listar conciliaciones | `GET /api/conciliaciones` |
| Obtener detalle | `GET /api/conciliaciones/{id}` |
| Programar reunión | `POST /api/conciliaciones/{id}/reunion` |
| Reprogramar reunión | `PUT /api/conciliaciones/{id}/reunion` |
| Cargar sedes | `GET /api/sedes` |

---

## Formulario de reunión

El formulario trabaja con:

| Campo | Regla en interfaz |
|---|---|
| `fechaReunion` | Fecha y hora obligatoria. |
| `sedeId` | Sede obligatoria. |
| `observaciones` | Texto opcional. |

El backend valida adicionalmente fecha futura, sede activa y máximo de 300 caracteres en observaciones.

---

## Programación

Cuando la conciliación no tiene reunión registrada, el componente usa:

```text
POST /api/conciliaciones/{id}/reunion
```

El backend crea una reunión uno a uno con la conciliación, cambia o asegura estado `REUNION_PROGRAMADA`, registra historial de programación y genera notificaciones.

---

## Reprogramación

Cuando la conciliación ya tiene reunión, el componente usa:

```text
PUT /api/conciliaciones/{id}/reunion
```

El backend actualiza la reunión vigente. La reprogramación exige cambio real en fecha, sede u observaciones. Si no hay cambios, el backend rechaza la operación.

Al reprogramar, se cancelan notificaciones pendientes anteriores y se crean nuevas notificaciones y recordatorios.

---

## Reunión vigente e historial

El frontend trabaja con la reunión vigente que llega en el detalle de conciliación. El backend registra historial de programación y reprogramación, pero la pantalla actual no consume un endpoint independiente ni muestra tabla histórica completa de eventos.

---

## Restricciones visuales

La pantalla informa cuando la conciliación no está en condición de programar o reprogramar, por ejemplo:

- conciliación finalizada;
- conciliación inactiva;
- falta de permiso;
- falta de selección de conciliación;
- ausencia de sedes disponibles.

La validación definitiva se mantiene en backend.

---

## Relación con notificaciones

La interfaz solicita la programación y la reprogramación mediante endpoints backend que registran notificaciones inmediatas y recordatorios. Cuando no se encuentran destinatarios con correo para notificar la reunión, el backend registra alertas administrativas.
