/**
 * Permission and authorization handling.
 *
 * Implementation detail.
 * Permission and authorization handling.
 * User flow detail.
 *
 * Permission and authorization handling.
 * Implementation detail.
 * Implementation detail.
 *
 * @module lib/permission
 *
 * @example
 * Permission and authorization handling.
 * import { tienePermiso } from "@/lib/authz";
 *
 * Permission and authorization handling.
 * List and table handling.
 * }
 */

export const PERMISOS = {
  // Role handling.
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

  // ── Catalogs ───────────
  VER_CATALOGOS: "Ver catálogos",
  GESTIONAR_CATALOGOS: "Gestionar catálogos",

  // ── People — natural persons registered in the system ──────────────────
  VER_PERSONAS: "Ver personas",
  CREAR_PERSONAS: "Crear personas",
  EDITAR_PERSONAS: "Editar personas",
  CAMBIAR_ESTADO_PERSONAS: "Cambiar estado personas",
  GESTIONAR_PERSONAS: "Gestionar personas",

  // ── Consultations ──────────────────────────────────────────────────
  VER_CONSULTAS: "Ver consultas",
  CREAR_CONSULTAS: "Crear consultas",
  EDITAR_CONSULTAS: "Editar consultas",
  CAMBIAR_ESTADO_CONSULTAS: "Cambiar estado consultas",
  ARCHIVAR_CONSULTAS: "Archivar consultas",
  ASIGNAR_RESPONSABLES_CONSULTA: "Asignar responsables consulta",

  // ── Follow-ups ────────────────────────────────────────────────
  VER_SEGUIMIENTOS: "Ver seguimientos",
  CREAR_SEGUIMIENTOS: "Crear seguimientos",
  EDITAR_SEGUIMIENTOS: "Editar seguimientos",
  ELIMINAR_SEGUIMIENTOS: "Eliminar seguimientos",
  RESPONDER_SEGUIMIENTOS: "Responder seguimientos",
  APROBAR_RESPUESTAS_SEGUIMIENTO: "Aprobar respuestas de seguimiento",
  VER_ALERTAS_DISCIPLINARIAS: "Ver alertas disciplinarias",
  GESTIONAR_CATEGORIAS_SEGUIMIENTO: "Gestionar categorías de seguimiento",

  // ── Users ─────────────────────────────────────────────────
  VER_USUARIOS: "Ver usuarios",
  CREAR_USUARIOS: "Crear usuarios",
  EDITAR_USUARIOS: "Editar usuarios",
  CAMBIAR_ESTADO_USUARIOS: "Cambiar estado usuarios",
  ASIGNAR_ROL_USUARIOS: "Asignar rol usuarios",

  // ── Roles and permissions ────────────────────────────────────────────────
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
  /** Implementation detail.*/
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

  // ── Statistics ──────────────────────────────────────────────
  VER_REPORTES: "Ver reportes",
  /** Role handling.*/
  ACCEDER_ESTADISTICAS: "Acceder estadísticas",

  // ── Procesos judiciales ──────────────────────────────────────────────────
  VER_PROCESOS: "Ver procesos",
  GESTIONAR_PROCESOS: "Gestionar procesos",
};
