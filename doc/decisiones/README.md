# Decisiones técnicas

Esta carpeta documenta decisiones de diseño aplicadas en el sistema.

Las decisiones aquí descritas explican criterios transversales que aparecen en varias capas del código fuente, como seguridad, permisos, estado funcional, archivos, conciliación y estrategias de perfiles.

## Documentos

| Documento | Decisión documentada |
|---|---|
| `estado-vs-activo.md` | Separación entre ciclo funcional y disponibilidad lógica. |
| `permisos-y-alcance.md` | Combinación entre permisos funcionales y alcance real del usuario. |
| `seguridad-documental.md` | Criterios para documentación segura sin exposición de secretos. |
| `documentacion-vigente.md` | Criterio de mantener la documentación alineada con código fuente vigente. |
| `conciliacion.md` | Decisiones del flujo de conciliación, reuniones, documentos y notificaciones. |
| `estrategia-perfiles.md` | Uso de Strategy para cambio de perfil, desactivación del perfil anterior y resolución del perfil activo. |

## Uso de estas decisiones

Las decisiones técnicas complementan la documentación de backend, API, reglas y frontend. Cuando una decisión afecta un módulo, el documento del módulo describe el comportamiento concreto y este directorio explica el criterio transversal.

## Principios

- Mantener reglas críticas en backend.
- Separar estado funcional de activo lógico.
- Proteger trazabilidad de consultas, procesos, seguimientos y conciliaciones.
- Evitar dependencias rígidas por tipo de perfil mediante estrategias.
- Mantener documentación alineada con código fuente.
- Documentar configuración sensible por nombre de variable, no por valor real.
