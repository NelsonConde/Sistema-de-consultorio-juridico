# Frontend - Módulo de estadísticas

> Documento ajustado contra el código fuente actual del frontend. Describe las vistas y consumos reales de `EstadisticasForm` e `InicioForm`.

## 1. Propósito

El frontend presenta estadísticas en dos espacios:

- la ruta `/estadisticas`, orientada a reportes institucionales para usuarios con `VER_REPORTES`;
- la ruta `/inicio`, que muestra indicadores del semestre actual según el perfil autenticado.

---

## 2. Vista `/estadisticas`

La pantalla principal de estadísticas se implementa en `EstadisticasForm.jsx`.

Flujo principal:

1. Consulta sesión mediante `/api/auth/me`.
2. Verifica permiso `VER_REPORTES`.
3. Si el usuario no tiene permiso, redirige a `/inicio`.
4. Carga semestres disponibles desde `/api/estadisticas/semestres`.
5. Permite consultar reportes por semestre.
6. Permite consultar reportes por rango libre de fechas.
7. Permite descargar PDF según el modo seleccionado.

---

## 3. Endpoints consumidos en `/estadisticas`

| Uso | Endpoint |
|---|---|
| Semestres disponibles | `GET /api/estadisticas/semestres` |
| Reporte por semestre | `GET /api/estadisticas/{año}/semestre/{semestre}` |
| PDF por semestre | `GET /api/estadisticas/{año}/semestre/{semestre}/pdf` |
| Reporte por rango | `GET /api/estadisticas/reporte?fechaInicio=&fechaFin=` |
| PDF por rango | `GET /api/estadisticas/reporte/pdf?fechaInicio=&fechaFin=` |

---

## 4. Semestres disponibles

La pantalla usa el endpoint de semestres como selector. El backend construye esos semestres desde 2024 hasta el año actual, incluyendo solo semestres que ya iniciaron.

---

## 5. Rango libre de fechas

El modo rango envía fechas en formato ISO `yyyy-MM-dd`.

Las validaciones definitivas las aplica el backend. El frontend construye la URL con `fechaInicio` y `fechaFin` y muestra el resultado o los mensajes de error retornados.

---

## 6. Descarga PDF

La pantalla descarga PDFs llamando al endpoint correspondiente según el modo actual:

- semestre;
- rango libre.

El backend retorna `application/pdf`. El reporte por rango se genera con la misma plantilla del reporte semestral y se identifica como reporte personalizado.

---

## 7. Panel de inicio

`InicioForm.jsx` usa estadísticas para mostrar una vista de resumen del semestre actual.

Comportamiento:

- calcula el semestre actual en el navegador;
- si el usuario es administrativo o tiene `VER_REPORTES`, consulta el reporte global del semestre;
- si el usuario tiene `VER_CONSULTAS`, consume endpoints por perfil cuando el tipo de perfil es estudiante, asesor o monitor;
- usa `perfilId` recibido desde `/api/auth/me` para construir la URL por perfil.

Endpoints por perfil usados desde inicio:

```http
GET /api/estadisticas/{año}/semestre/{semestre}/estudiante/{perfilId}
GET /api/estadisticas/{año}/semestre/{semestre}/asesor/{perfilId}
GET /api/estadisticas/{año}/semestre/{semestre}/monitor/{perfilId}
```

Desde `/inicio`, los reportes resumidos por perfil consumidos corresponden a estudiante, asesor y monitor.

---

## 8. Vista resumida por perfil

Los endpoints por perfil reutilizan `EstadisticasSemestreDTO` y el backend entrega una vista resumida. El frontend trata como opcionales los agregados adicionales al resumen por perfil.

La vista por perfil se orienta a métricas de inicio, principalmente:

- consultas finalizadas;
- consultas pendientes;
- total de consultas;
- personas atendidas;
- procesos por estado.

---

## 9. Indicadores de procesos

El indicador `procesosPorEstado` se muestra como dato complementario: en reportes globales representa la distribución vigente de procesos y, en reportes por perfil, la distribución vigente asociada al perfil correspondiente.

---

## 10. Manejo de errores

La pantalla usa los mensajes retornados por backend para mostrar errores de validación, permisos o parámetros temporales inválidos.
