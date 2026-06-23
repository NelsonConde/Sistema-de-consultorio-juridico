# Sistema de gestión de casos jurídicos

Sistema web para la gestión integral de casos jurídicos del consultorio jurídico.

El proyecto centraliza la administración de usuarios, roles, permisos, personas, consultas jurídicas, seguimientos, procesos, conciliaciones, auditoría y archivos asociados al funcionamiento del consultorio.

## Propósito

El sistema permite organizar y controlar el ciclo operativo de atención jurídica, desde el registro de personas y consultas hasta la gestión de actuaciones internas, seguimiento de casos, procesos y conciliaciones.

La solución está estructurada como una aplicación web con backend en Spring Boot y frontend en Next.js.

## Arquitectura general

```text
backend/
  app/
    Backend principal del sistema.

frontend/
  Aplicación web del sistema.
```

## Backend

El backend principal se encuentra en:

```text
backend/app
```

Tecnologías principales:

- Java 21.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- PostgreSQL/Supabase.
- Jakarta Validation.
- Lombok.
- Maven.
- JWT para autenticación.
- Cookies para manejo de sesión.
- Soporte de auditoría.
- Soporte de envío de correos.
- Soporte de almacenamiento de archivos.

## Frontend

El frontend se encuentra en:

```text
frontend
```

Tecnologías principales:

- Next.js.
- React.
- Tailwind CSS.
- Radix UI / shadcn.
- Fetch API.
- Playwright.

## Seguridad y configuración

La configuración sensible del sistema se maneja mediante variables de entorno o configuración externa del entorno de ejecución.

La documentación del repositorio describe el propósito de las variables, pero no debe incluir valores reales de secretos, llaves, tokens, firmas, credenciales o contraseñas.

Variables principales del backend:

| Variable | Propósito |
|---|---|
| `DB_URL` | Define la conexión a la base de datos. |
| `DB_USERNAME` | Define el usuario de conexión a base de datos. |
| `DB_PASSWORD` | Define la contraseña de conexión a base de datos. |
| `JWT_SECRET` | Define el secreto usado por el mecanismo de tokens. |
| `BREVO_API_KEY` | Define la llave del proveedor de correo. |
| `MAIL_FROM_EMAIL` | Define el remitente usado para correos del sistema. |

Variables principales del frontend:

| Variable | Propósito |
|---|---|
| `NEXT_PUBLIC_API_URL` | Define la URL pública base del backend. |
| `NEXT_PUBLIC_API_URL_BASE` | Define la URL pública base de la API. |
| `NEXT_PUBLIC_FILE_STORAGE_API_URL_BASE` | Define la URL pública base para consumo de archivos. |

Las variables `NEXT_PUBLIC_*` son visibles para el navegador y no deben contener información sensible.

## Módulos principales

### Autenticación

Gestiona inicio de sesión, validación de sesión actual y cierre de sesión.

El frontend consume la autenticación usando cookie de sesión y envía las peticiones protegidas con:

```javascript
credentials: "include"
```

### Usuarios, roles y permisos

Gestiona usuarios del sistema, perfiles internos, roles y permisos funcionales.

La autorización combina permisos generales con reglas de alcance propias de cada módulo.

### Personas

Gestiona la información de personas relacionadas con el consultorio jurídico y sus casos.

### Catálogos

Gestiona datos auxiliares usados por los formularios y flujos principales del sistema.

### Consultas jurídicas

Gestiona el registro y ciclo operativo de las consultas jurídicas.

### Seguimientos

Gestiona seguimientos asociados a consultas jurídicas, incluyendo respuestas y documentos adjuntos cuando aplica.

### Procesos

Gestiona procesos asociados a consultas jurídicas y su estado operativo.

### Conciliaciones

Gestiona conciliaciones asociadas a consultas jurídicas.

Características principales del módulo:

- creación de conciliación desde consulta;
- carga de solicitud PDF;
- autoasignación de estudiante habilitado;
- autoasignación de conciliador por menor carga;
- estados administrados por catálogo;
- validación de reglas por código técnico de estado;
- detalle con consultante, partes y contrapartes desde la consulta;
- asignación de estudiante;
- asignación de conciliador;
- cambio de estados operativos;
- finalización con acta PDF;
- reemplazo de solicitud;
- desactivación lógica;
- validación de conciliaciones pendientes al cerrar consultas.

### Auditoría

Registra acciones relevantes del sistema según las reglas configuradas en backend.

### Archivos

Gestiona carga, consulta y descarga de documentos usados por los módulos del sistema.

## Ejecución local

### Backend

Desde la raíz del proyecto:

```bash
cd backend/app
./mvnw spring-boot:run
```

En Windows:

```powershell
cd backend/app
.\mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Por defecto, el frontend se ejecuta en:

```text
http://localhost:3000
```

## Docker Compose

Desde la raíz del proyecto:

```bash
docker compose up --build
```

## Convenciones de mantenimiento

- La documentación debe mantenerse alineada con el código fuente vigente.
- Los valores sensibles no deben documentarse ni versionarse.
- Los archivos generados, reportes locales y configuraciones de IDE no deben formar parte del repositorio.
- Los permisos deben documentarse cuando afecten navegación, acciones o reglas de acceso.
- Las reglas de negocio críticas deben validarse en backend.