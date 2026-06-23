"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { AppSidebar } from "@/components/app-sidebar";
import { API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import {
  tieneAlgunPermiso,
  tieneTodosLosPermisos,
} from "@/lib/authz";

const SIDEBAR_PAGES = [
  {
    title: "Inicio",
    tooltip: "Inicio",
    path: "/inicio",
    requiredPermissions: [PERMISOS.ACCEDER_INICIO],
    match: "any",
  },
  {
    title: "Recepción",
    tooltip: "Recepción de personas",
    path: "/recepcion",
    requiredPermissions: [PERMISOS.ACCEDER_RECEPCION],
    match: "all",
  },
  {
    title: "Personas",
    tooltip: "Personas",
    path: "/personas",
    requiredPermissions: [PERMISOS.ACCEDER_PERSONAS],
    match: "all",
  },
  {
    title: "Nueva consulta",
    tooltip: "Nueva consulta",
    path: "/nuevaconsulta",
    requiredPermissions: [PERMISOS.ACCEDER_NUEVA_CONSULTA],
    match: "all",
  },
  {
    title: "Consultas jurídicas",
    tooltip: "Consultas jurídicas",
    path: "/consultasjuridicas",
    requiredPermissions: [PERMISOS.ACCEDER_CONSULTAS_JURIDICAS],
    match: "all",
  },
  {
    title: "Tareas",
    tooltip: "Tareas y seguimientos",
    path: "/tareas",
    requiredPermissions: [PERMISOS.ACCEDER_TAREAS],
    match: "all",
  },
  {
    title: "Nuevo proceso",
    tooltip: "Registrar nuevo proceso",
    path: "/nuevoproceso",
    requiredPermissions: [
      PERMISOS.ACCEDER_PROCESOS,
      PERMISOS.GESTIONAR_PROCESOS,
    ],
    match: "all",
  },
  {
    title: "Procesos",
    tooltip: "Procesos",
    path: "/procesos",
    requiredPermissions: [PERMISOS.ACCEDER_PROCESOS],
    match: "all",
  },
  {
    title: "Conciliaciones",
    tooltip: "Conciliaciones",
    path: "/conciliaciones",
    requiredPermissions: [PERMISOS.ACCEDER_CONCILIACIONES],
    match: "all",
  },
  {
    title: "Estudiantes",
    tooltip: "Estudiantes",
    path: "/estudiantes",
    requiredPermissions: [PERMISOS.ACCEDER_ESTUDIANTES],
    match: "all",
  },
  {
    title: "Asesores y monitores",
    tooltip: "Asesores y monitores",
    path: "/asesoresymonitores",
    requiredPermissions: [PERMISOS.ACCEDER_ASESORES_MONITORES],
    match: "all",
  },
  {
    title: "Roles",
    tooltip: "Roles / Usuarios",
    path: "/roles",
    requiredPermissions: [PERMISOS.ACCEDER_ROLES],
    match: "all",
  },
  {
    title: "Estadísticas",
    tooltip: "Estadísticas del semestre",
    path: "/estadisticas",
    requiredPermissions: [PERMISOS.VER_REPORTES],
    match: "any",
  },
  {
    title: "Administración",
    tooltip: "Administración",
    path: "/admin",
    requiredPermissions: [
      PERMISOS.ACCEDER_ADMINISTRACION,
    ],
    match: "all",
  },
  {
    title: "Eliminación",
    tooltip: "Registros desactivados",
    path: "/eliminacion",
    requiredPermissions: [PERMISOS.ACCEDER_ELIMINACION],
    match: "all",
  },
];

/**
 * Determina si un usuario puede ver una página específica.
 * Verifica los permisos requeridos según la lógica de match (any/all).
 * 
 * @param {Object} page - Objeto de página con requiredPermissions y match
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si el usuario tiene acceso a la página
 */
function puedeVerPagina(page, user) {
  if (!user) return false;

  const requiredPermissions = Array.isArray(page.requiredPermissions)
    ? page.requiredPermissions.filter(Boolean)
    : [];

  if (requiredPermissions.length === 0) {
    return true;
  }

  if (page.match === "any") {
    return tieneAlgunPermiso(user, requiredPermissions);
  }

  return tieneTodosLosPermisos(user, requiredPermissions);
}

/**
 * Filtra una lista de páginas según los permisos del usuario.
 * Solo retorna las páginas a las que el usuario tiene acceso.
 * 
 * @param {Array<Object>} pages - Array de páginas disponibles
 * @param {Object} user - Objeto del usuario
 * @returns {Array<Object>} Array de páginas filtradas
 */
function filtrarPaginasPorPermisos(pages, user) {
  return pages.filter((page) => puedeVerPagina(page, user));
}

export function PermissionSidebar() {
  const router = useRouter();

  const [user, setUser] = React.useState(null);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    const cargarUsuario = async () => {
      try {
        const res = await fetch(`${API_URL_BASE}/auth/me`, {
          method: "GET",
          credentials: "include",
        });

        if (res.status === 401) {
          router.replace("/");
          return;
        }

        if (!res.ok) {
          setUser(null);
          return;
        }

        const data = await res.json();
        setUser(data);
      } catch (error) {
        console.error("Error cargando permisos del usuario", error);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    cargarUsuario();
  }, [router]);

  if (loading) {
    return <AppSidebar mainItems={[]} footerItems={[]} />;
  }

  const mainItems = filtrarPaginasPorPermisos(SIDEBAR_PAGES, user);

  return <AppSidebar mainItems={mainItems} footerItems={[]} />;
}