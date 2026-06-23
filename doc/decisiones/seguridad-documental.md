# Decisión técnica - Seguridad documental

## Contexto

El proyecto contiene configuración de seguridad, autenticación, base de datos, correo, almacenamiento de archivos y variables de entorno. Parte de esa información puede involucrar secretos, tokens, credenciales, firmas, llaves o datos sensibles.

## Decisión

La documentación del repositorio describe únicamente:

- nombres de variables;
- propósito de variables;
- flujos de configuración;
- responsabilidades de componentes;
- criterios de uso seguro.

No se documentan valores reales de configuración sensible.

## Información que no se publica con valores reales

No se deben publicar valores reales de:

- contraseñas;
- secretos JWT;
- tokens;
- API keys;
- firmas;
- llaves privadas;
- cadenas de conexión con credenciales;
- usuarios reales de prueba;
- datos personales reales;
- rutas privadas del equipo;
- enlaces reales de recuperación;
- credenciales de correo.

## Variables documentables por nombre

| Variable | Propósito documental |
|---|---|
| `DB_URL` | URL de conexión a base de datos. |
| `DB_USERNAME` | Usuario de base de datos. |
| `DB_PASSWORD` | Contraseña de base de datos. |
| `DB_DDL_AUTO` | Comportamiento de Hibernate para esquema. |
| `JWT_SECRET` | Secreto usado por emisión y validación de JWT. |
| `BREVO_API_KEY` | Llave del proveedor de correo. |
| `MAIL_FROM_EMAIL` | Correo remitente. |
| `FRONTEND_URL` | URL permitida para flujos que redirigen al frontend. |
| `UPLOAD_DIR` | Directorio base de almacenamiento de archivos. |
| `NEXT_PUBLIC_API_URL` | URL pública de API usada por frontend. |
| `NEXT_PUBLIC_API_URL_BASE` | Base de endpoints usada por frontend. |
| `NEXT_PUBLIC_FILE_STORAGE_API_URL_BASE` | Base para endpoints de archivos. |

## Ejemplo correcto

```text
JWT_SECRET define el secreto usado por el mecanismo de tokens.
```

## Ejemplo incorrecto

```text
JWT_SECRET=valor-real-del-ambiente
```

## Impacto en backend

El backend lee configuración mediante propiedades y variables de entorno. La documentación explica el propósito, no el valor.

## Impacto en frontend

El frontend usa variables públicas `NEXT_PUBLIC_*` para conocer la API. Aunque son públicas por diseño, la documentación conserva enfoque de configuración y no expone ambientes privados.

## Criterios de mantenimiento

Al agregar una nueva variable:

1. Documentar nombre y propósito.
2. No incluir valor real.
3. Indicar si es backend o frontend.
4. Revisar guías de configuración y despliegue.
