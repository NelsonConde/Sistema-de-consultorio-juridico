/**
 * Constantes de permisos del sistema de casos jurídicos.
 *
 * Cada clave es el identificador usado en el frontend; el valor es el nombre
 * exacto que el backend almacena en la BD y devuelve en el array `permisos`
 * del objeto de usuario (`/api/auth/me`).
 *
 * La comparación de permisos se hace de forma normalizada (sin tildes,
 * sin mayúsculas) en `lib/authz.js`, por lo que el valor aquí debe coincidir
 * con el nombre del backend pero no es sensible a tildes ni a case.
 *
 * @module lib/permission
 *
 * @example
 * import { PERMISOS } from "@/lib/permission";
 * import { tienePermiso } from "@/lib/authz";
 *
 * if (tienePermiso(user, PERMISOS.VER_CONSULTAS)) {
 *   // mostrar listado de consultas
 * }
 */

export const PERMISOS = {
  // ── Navegación — controlan qué páginas aparecen en el menú ──────────────
  ACCEDER_INICIO: "Acceder inicio",
  ACCEDER_RECEPCION: "Acceder recepción",
  ACCEDER_TAREAS: "Acceder tareas",
  ACCEDER_NUEVA_CONSULTA: "Acceder nueva consulta",
  ACCEDER_CONSULTAS_JURIDICAS: "Acceder consultas jurídicas",
  ACCEDER_ADMINISTRACION: "Acceder administración",
  ACCEDER_ROLES: "Acceder roles",
  ACCEDER_ESTUDIANTES: "Acceder estudiantes",
  ACCEDER_ASESORES_MONITORES: "Acceder asesores y monitores",
  ACCEDER_PERSONAS: "Acceder personas",
  ACCEDER_ELIMINACION: "Acceder eliminación",
  ACCEDER_CONCILIACIONES: "Acceder conciliaciones",
  ACCEDER_PROCESOS: "Acceder procesos",

  // ── Catálogos — tipos de documento, áreas, sedes, temas, etc. ───────────
  VER_CATALOGOS: "Ver catálogos",
  GESTIONAR_CATALOGOS: "Gestionar catálogos",

  // ── Personas — personas naturales registradas en el sistema ─────────────
  VER_PERSONAS: "Ver personas",
  CREAR_PERSONAS: "Crear personas",
  EDITAR_PERSONAS: "Editar personas",
  CAMBIAR_ESTADO_PERSONAS: "Cambiar estado personas",
  GESTIONAR_PERSONAS: "Gestionar personas",

  // ── Consultas jurídicas ──────────────────────────────────────────────────
  VER_CONSULTAS: "Ver consultas",
  CREAR_CONSULTAS: "Crear consultas",
  EDITAR_CONSULTAS: "Editar consultas",
  CAMBIAR_ESTADO_CONSULTAS: "Cambiar estado consultas",
  ARCHIVAR_CONSULTAS: "Archivar consultas",
  ASIGNAR_RESPONSABLES_CONSULTA: "Asignar responsables consulta",

  // ── Seguimientos / tareas ────────────────────────────────────────────────
  VER_SEGUIMIENTOS: "Ver seguimientos",
  CREAR_SEGUIMIENTOS: "Crear seguimientos",
  EDITAR_SEGUIMIENTOS: "Editar seguimientos",
  ELIMINAR_SEGUIMIENTOS: "Eliminar seguimientos",
  RESPONDER_SEGUIMIENTOS: "Responder seguimientos",
  APROBAR_RESPUESTAS_SEGUIMIENTO: "Aprobar respuestas de seguimiento",
  VER_ALERTAS_DISCIPLINARIAS: "Ver alertas disciplinarias",
  GESTIONAR_CATEGORIAS_SEGUIMIENTO: "Gestionar categorías de seguimiento",

  // ── Usuarios del sistema ─────────────────────────────────────────────────
  VER_USUARIOS: "Ver usuarios",
  CREAR_USUARIOS: "Crear usuarios",
  EDITAR_USUARIOS: "Editar usuarios",
  CAMBIAR_ESTADO_USUARIOS: "Cambiar estado usuarios",
  ASIGNAR_ROL_USUARIOS: "Asignar rol usuarios",

  // ── Roles y permisos ─────────────────────────────────────────────────────
  VER_ROLES: "Ver roles",
  CREAR_ROLES: "Crear roles",
  EDITAR_ROLES: "Editar roles",
  ASIGNAR_PERMISOS_ROLES: "Asignar permisos a roles",

  // ── Estudiantes ──────────────────────────────────────────────────────────
  VER_ESTUDIANTES: "Ver estudiantes",
  CAMBIAR_ESTADO_ESTUDIANTES: "Cambiar estado estudiantes",

  // ── Asesores y monitores ─────────────────────────────────────────────────
  VER_ASESORES_MONITORES: "Ver asesores y monitores",
  GESTIONAR_ASESORES_MONITORES: "Gestionar asesores y monitores",

  // ── Administradores ──────────────────────────────────────────────────────
  VER_ADMINISTRADORES: "Ver administradores",
  /** Solo la directora puede gestionar administradores. */
  GESTIONAR_ADMINISTRADORES: "Gestionar administradores",

  // ── Perfiles auxiliares (asesores, monitores, estudiantes) ───────────────
  VER_PERFILES_AUXILIARES: "Ver perfiles auxiliares",

  // ── Conciliaciones ───────────────────────────────────────────────────────
  VER_CONCILIACIONES: "Ver conciliaciones",
  GESTIONAR_CONCILIACIONES: "Gestionar conciliaciones",
  PROGRAMAR_REUNIONES_CONCILIACION: "Programar reuniones de conciliación",
  REPROGRAMAR_REUNIONES_CONCILIACION: "Reprogramar reuniones de conciliación",
  CONCLUIR_CONCILIACIONES: "Concluir conciliaciones",

  // ── Conciliadores ────────────────────────────────────────────────────────
  VER_CONCILIADORES: "Ver conciliadores",
  GESTIONAR_CONCILIADORES: "Gestionar conciliadores",

  // ── Reportes y estadísticas ──────────────────────────────────────────────
  VER_REPORTES: "Ver reportes",
  /** Creado localmente para controlar acceso a la página de estadísticas. */
  ACCEDER_ESTADISTICAS: "Acceder estadísticas",

  // ── Procesos judiciales ──────────────────────────────────────────────────
  VER_PROCESOS: "Ver procesos",
  GESTIONAR_PROCESOS: "Gestionar procesos",
};
