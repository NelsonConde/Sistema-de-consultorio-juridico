import { readResponseBody } from "@/lib/api";

export function normalizar(value) {
  return String(value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}

export function nombrePermiso(permiso) {
  if (typeof permiso === "string") return permiso;

  return (
    permiso?.nombre ||
    permiso?.nombrePermiso ||
    permiso?.descripcion ||
    permiso?.permiso ||
    ""
  );
}

export function idPermiso(permiso) {
  return permiso?.id ?? permiso?.permisoId;
}

export function nombreRol(rol) {
  return rol?.nombre || rol?.rolNombre || rol?.name || "";
}

export function claveRol(rol) {
  const nombre = normalizar(nombreRol(rol));

  if (!nombre) return "";
  if (nombre.includes("ESTUDIANTE")) return "ESTUDIANTE";
  if (nombre.includes("CONCILIADOR")) return "CONCILIADOR";
  if (nombre.includes("ASESOR")) return "ASESOR";
  if (nombre.includes("MONITOR")) return "MONITOR";
  if (nombre.includes("ADMIN") || nombre.includes("DIRECTOR")) return "ADMINISTRADOR";

  return nombre;
}

export function permisosAsignarPagina(page, rol) {
  const permisosPorRol = page?.permisosAsignarPorRol || {};
  const clave = claveRol(rol);

  if (clave && Array.isArray(permisosPorRol[clave])) {
    return permisosPorRol[clave];
  }

  if (Array.isArray(permisosPorRol.DEFAULT)) {
    return permisosPorRol.DEFAULT;
  }

  return Array.isArray(page?.permisosAsignar) ? page.permisosAsignar : [];
}

export function permisosGestionadosPagina(page) {
  const nombres = new Set();
  const agregar = (lista) => {
    if (!Array.isArray(lista)) return;
    lista.filter(Boolean).forEach((permiso) => nombres.add(permiso));
  };

  agregar(page?.permisosAsignar);
  Object.values(page?.permisosAsignarPorRol || {}).forEach(agregar);

  return [...nombres];
}

export function buscarPermiso(permisos, nombre) {
  return permisos.find(
    (permiso) => normalizar(nombrePermiso(permiso)) === normalizar(nombre)
  );
}

export function paginaMarcada(page, permisosRol) {
  const permisosRolNormalizados = permisosRol.map((permiso) =>
    normalizar(nombrePermiso(permiso))
  );

  const permisosVista = Array.isArray(page.permisosVista)
    ? page.permisosVista.filter(Boolean)
    : [];

  if (permisosVista.length === 0) return false;

  return permisosVista.some((permiso) =>
    permisosRolNormalizados.includes(normalizar(permiso))
  );
}

export async function leerRespuesta(response) {
  const data = await readResponseBody(response);
  return typeof data === "string" ? { mensaje: data } : data;
}

export function extraerLista(data) {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];

  const claves = [
    "content",
    "data",
    "items",
    "rows",
    "permisos",
    "roles",
    "resultado",
    "payload",
  ];

  for (const clave of claves) {
    const valor = data[clave];

    if (Array.isArray(valor)) return valor;

    if (valor && typeof valor === "object") {
      const interno = extraerLista(valor);
      if (interno.length > 0) return interno;
    }
  }

  return [];
}

export function esErrorDuplicadoPermiso(data) {
  const mensaje = normalizar(data?.mensaje || data?.message || data?.error);

  return mensaje.includes("YA EXISTE") && mensaje.includes("PERMISO");
}
