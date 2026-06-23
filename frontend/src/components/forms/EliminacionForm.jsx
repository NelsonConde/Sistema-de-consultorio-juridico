"use client"

/**
 * Formulario de eliminación y reactivación de registros.
 *
 * Permite cambiar el estado activo/inactivo de personas, usuarios,
 * estudiantes y consultas. Requiere los permisos correspondientes
 * según el tipo de recurso.
 *
 * @module components/forms/EliminacionForm
 */
;

import React, { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { API_URL_BASE } from "@/lib/config";
import { DEFAULT_PAGE_SIZE_OPTIONS, getTotalPages, paginateItems, sortByIdAsc } from "@/lib/list-utils";
import { RotateCcw, RefreshCw } from "lucide-react";
import { useRouter } from "next/navigation";
import { PERMISOS } from "@/lib/permission";
import { tieneTodosLosPermisos } from "@/lib/authz";

const SECCIONES = [
  {
    id: "personas",
    titulo: "Personas",
    endpoint: "/personas",
    tipo: "persona",
    reactivar: "activo",
  },
  {
    id: "consultas",
    titulo: "Consultas",
    endpoint: "/consultas/archivadas",
    tipo: "consulta",
    reactivar: "consulta",
  },
  {
    id: "usuarios",
    titulo: "Usuarios del sistema",
    endpoint: "/usuarios-sistema",
    tipo: "usuario",
    reactivar: "activo",
  },
  {
    id: "administrativos",
    titulo: "Administrativos",
    endpoint: "/administrativos",
    tipo: "perfil",
    reactivar: "activo",
  },
  {
    id: "asesores",
    titulo: "Asesores",
    endpoint: "/asesores",
    tipo: "perfil",
    reactivar: "activo",
  },
  {
    id: "estudiantes",
    titulo: "Estudiantes",
    endpoint: "/estudiantes",
    tipo: "perfil",
    reactivar: "activo",
  },
  {
    id: "monitores",
    titulo: "Monitores",
    endpoint: "/monitores",
    tipo: "perfil",
    reactivar: "activo",
  },
  {
    id: "conciliadores",
    titulo: "Conciliadores",
    endpoint: "/conciliadores",
    tipo: "perfil",
    reactivar: "activo",
  },
];

/**
 * Normaliza un valor a texto para comparaciones internas.
 * @param {any} value - Valor a normalizar.
 * @returns {string} Cadena normalizada.
 */
function normalizar(value) {
  return String(value || "").trim().toUpperCase();
}

/**
 * Determina si un registro está inactivo.
 * @param {Object} item - Registro de persona o perfil.
 * @returns {boolean} True si el registro se considera inactivo.
 */
function estaInactivo(item) {
  return item?.activo === false || normalizar(item?.estado) === "INACTIVO";
}

/**
 * Determina si una consulta está archivada.
 * @param {Object} item - Registro de consulta.
 * @returns {boolean} True si la consulta está archivada.
 */
function estaArchivadaConsulta(item) {
  const estado = normalizar(item?.estado);

  return estado === "ARCHIVADO" || estado === "ARCHIVADA";
}

/**
 * Devuelve el nombre visible de un registro.
 * @param {Object} item - Registro de persona o consulta.
 * @returns {string} Nombre mostrado.
 */
function nombrePersona(item) {
  return (
    item?.nombre ||
    item?.nombreCompleto ||
    [item?.nombres, item?.apellidos].filter(Boolean).join(" ") ||
    item?.username ||
    item?.usuario ||
    "Sin nombre"
  );
}

/**
 * Obtiene el documento o identificador de una persona.
 * @param {Object} item - Registro de persona.
 * @returns {string} Documento o identificador.
 */
function documentoPersona(item) {
  return item?.documento || item?.numeroDocumento || item?.cedula || "N/A";
}

/**
 * Genera un texto descriptivo para una consulta.
 * @param {Object} item - Registro de consulta.
 * @returns {string} Texto descriptivo.
 */
function textoConsulta(item) {
  return (
    item?.consulta ||
    item?.descripcion ||
    item?.resumen ||
    item?.tramite ||
    `Consulta #${item?.id}`
  );
}

/**
 * Obtiene un detalle adicional según el tipo de registro.
 * @param {Object} item - Registro actual.
 * @param {string} tipo - Tipo de registro.
 * @returns {string} Detalle adicional.
 */
function detalleItem(item, tipo) {
  if (tipo === "consulta") {
    return item?.fecha || item?.estado || "Consulta archivada";
  }

  if (tipo === "usuario") {
    return item?.rolNombre || item?.tipoPerfil || "Usuario desactivado";
  }

  if (tipo === "persona") {
    return item?.tipoUsuario || item?.correo || item?.telefono || "Persona desactivada";
  }

  return item?.codigo || item?.usuario || item?.telefono || "Perfil desactivado";
}

/**
 * Lee la respuesta de la API y devuelve JSON o mensaje simple.
 * @param {Response} response - Respuesta HTTP recibida.
 * @returns {Promise<any>} Resultado parseado.
 */
async function leerRespuesta(response) {
  const text = await response.text();

  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { mensaje: text };
  }
}

/**
 * Componente que muestra registros desactivados y permite reactivarlos.
 * @returns {JSX.Element} Formulario de eliminación.
 */
export function EliminacionForm() {
  const [seccionActiva, setSeccionActiva] = useState("personas");
  const [data, setData] = useState({});
  const [loading, setLoading] = useState(true);
  const [reactivando, setReactivando] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [confirmDialogAbierto, setConfirmDialogAbierto] = useState(false);
  const [itemAReactivar, setItemAReactivar] = useState(null);
  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);
  const router = useRouter();

  const seccion = useMemo(() => {
    return SECCIONES.find((item) => item.id === seccionActiva);
  }, [seccionActiva]);

  const itemsActuales = useMemo(() => {
    const lista = data[seccionActiva] || [];
    const q = busqueda.trim().toLowerCase();

    const filtradosPorEstado = lista.filter((item) => {
      if (seccion?.tipo === "consulta") {
        return estaArchivadaConsulta(item);
      }

      return estaInactivo(item);
    });

    const filtrados = !q
      ? filtradosPorEstado
      : filtradosPorEstado.filter((item) =>
      [
        item?.id,
        nombrePersona(item),
        documentoPersona(item),
        textoConsulta(item),
        detalleItem(item, seccion?.tipo),
        item?.estado,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(q)
    );

    return sortByIdAsc(filtrados);
  }, [data, seccionActiva, busqueda, seccion]);

  useEffect(() => {
    verificarYCargar();
  }, []);

  async function verificarYCargar() {
    try {
      setLoading(true);

      const res = await fetch(`${API_URL_BASE}/auth/me`, {
        method: "GET",
        credentials: "include",
      });

      if (res.status === 401) {
        router.replace("/");
        return;
      }

      if (!res.ok) {
        router.replace("/");
        return;
      }

      const user = await res.json();

      const puedeEntrar = tieneTodosLosPermisos(user, [
        PERMISOS.CAMBIAR_ESTADO_PERSONAS,
        PERMISOS.CAMBIAR_ESTADO_USUARIOS,
        PERMISOS.CAMBIAR_ESTADO_ESTUDIANTES,
        PERMISOS.CAMBIAR_ESTADO_CONSULTAS,
        PERMISOS.ARCHIVAR_CONSULTAS,
      ]);

      if (!puedeEntrar) {
        router.replace("/inicio");
        return;
      }

      await cargarTodo();
    } catch (error) {
      console.error("Error verificando permisos de eliminación", error);
      router.replace("/");
    } finally {
      setLoading(false);
    }
  }

  async function cargarTodo() {
    try {
      setLoading(true);

      const resultados = {};

      await Promise.all(
        SECCIONES.map(async (item) => {
          try {
            const res = await fetch(`${API_URL_BASE}${item.endpoint}`, {
              credentials: "include",
            });

            if (res.status === 401) {
              toast.error("La sesión expiró");
              resultados[item.id] = [];
              return;
            }

            if (res.status === 403) {
              resultados[item.id] = [];
              return;
            }

            if (!res.ok) {
              resultados[item.id] = [];
              return;
            }

            const json = await res.json();
            resultados[item.id] = Array.isArray(json) ? json : [];
          } catch (error) {
            console.error(`Error cargando ${item.titulo}`, error);
            resultados[item.id] = [];
          }
        })
      );

      setData(resultados);
    } finally {
      setLoading(false);
    }
  }

  function abrirConfirmReactivar(item) {
    setItemAReactivar(item);
    setConfirmDialogAbierto(true);
  }

  function cerrarConfirmDialog() {
    setConfirmDialogAbierto(false);
    setItemAReactivar(null);
  }

  async function confirmarReactivarItem() {
    if (!seccion || !itemAReactivar) return;

    try {
      setReactivando(`${seccion.id}-${itemAReactivar.id}`);

      if (seccion.reactivar === "consulta") {
        await reactivarConsulta(itemAReactivar);
      } else {
        await reactivarActivo(seccion.endpoint, itemAReactivar.id);
      }

      toast.success(
        seccion.reactivar === "consulta"
          ? "Consulta desarchivada correctamente"
          : "Registro reactivado correctamente"
      );
      await cargarTodo();
    } catch (error) {
      console.error(error);
      toast.error(error.message || "No se pudo reactivar el registro");
    } finally {
      setReactivando("");
      cerrarConfirmDialog();
    }
  }

  async function reactivarActivo(endpoint, id) {
    const url = endpoint === "/personas"
      ? `${API_URL_BASE}${endpoint}/${id}/reactivar`
      : `${API_URL_BASE}${endpoint}/${id}/activo?activo=true`;

    const res = await fetch(url, {
      method: "PATCH",
      credentials: "include",
    });

    const data = await leerRespuesta(res);

    if (!res.ok) {
      throw new Error(
        data?.mensaje || data?.message || "No se pudo cambiar el estado"
      );
    }
  }

  async function reactivarConsulta(item) {
    const res = await fetch(`${API_URL_BASE}/consultas/${item.id}/desarchivar`, {
      method: "PATCH",
      credentials: "include",
    });

    const data = await leerRespuesta(res);

    if (!res.ok) {
      throw new Error(
        data?.mensaje || data?.message || "No se pudo desarchivar la consulta"
      );
    }
  }

  const totalSeccion = itemsActuales.length;
  const totalPaginas = getTotalPages(totalSeccion, registrosPorPagina);
  const itemsPaginados = useMemo(
    () => paginateItems(itemsActuales, paginaActual, registrosPorPagina),
    [itemsActuales, paginaActual, registrosPorPagina]
  );

  useEffect(() => {
    setPaginaActual(1);
  }, [seccionActiva, busqueda, registrosPorPagina]);

  useEffect(() => {
    if (paginaActual > totalPaginas) {
      setPaginaActual(totalPaginas);
    }
  }, [paginaActual, totalPaginas]);

  return (
    <div className="space-y-5">
      <div className="rounded-xl border bg-card p-5 space-y-4">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h3 className="text-xl font-bold">Registros desactivados</h3>
            <p className="text-sm text-muted-foreground">
              Selecciona una sección para ver registros desactivados o archivados.
            </p>
          </div>

          <Button type="button" variant="outline" onClick={cargarTodo}>
            <RefreshCw className="mr-2 size-4" />
            Actualizar
          </Button>
        </div>

        <div className="grid grid-cols-2 gap-2 md:grid-cols-4">
          {SECCIONES.map((item) => {
            const total = (data[item.id] || []).filter((row) =>
              item.tipo === "consulta" ? estaArchivadaConsulta(row) : estaInactivo(row)
            ).length;

            return (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  setSeccionActiva(item.id);
                  setBusqueda("");
                }}
                className={`rounded-lg border px-3 py-2 text-sm font-medium transition ${seccionActiva === item.id
                  ? "border-primary bg-primary text-primary-foreground"
                  : "bg-background hover:bg-muted"
                  }`}
              >
                {item.titulo}
                <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">
                  {total}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="rounded-xl border bg-card p-5 space-y-4">
        <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <h3 className="text-lg font-bold">{seccion?.titulo}</h3>
            <p className="text-sm text-muted-foreground">
              {totalSeccion} registro(s) para recuperar.
            </p>
          </div>

          <div className="w-full md:max-w-sm">
            <label className="text-sm font-medium">Buscar</label>
            <input
              value={busqueda}
              onChange={(event) => setBusqueda(event.target.value)}
              placeholder="ID, nombre, documento o estado..."
              className="mt-1 h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>
        </div>

        <div className="overflow-hidden rounded-lg border">
          <div className="max-h-[560px] overflow-auto">
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-muted">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">ID</th>
                  <th className="px-4 py-3 text-left font-medium">Nombre</th>
                  <th className="px-4 py-3 text-left font-medium">
                    {seccion?.tipo === "consulta" ? "Consulta" : "Documento / Usuario"}
                  </th>
                  <th className="px-4 py-3 text-left font-medium">Detalle</th>
                  <th className="px-4 py-3 text-right font-medium">Acción</th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="px-4 py-8 text-center text-muted-foreground"
                    >
                      Cargando registros...
                    </td>
                  </tr>
                ) : itemsActuales.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="px-4 py-8 text-center text-muted-foreground"
                    >
                      No hay registros desactivados en esta sección.
                    </td>
                  </tr>
                ) : (
                  itemsPaginados.map((item) => {
                    const key = `${seccion.id}-${item.id}`;
                    const isLoading = reactivando === key;

                    return (
                      <tr
                        key={key}
                        className="border-t transition hover:bg-muted/40"
                      >
                        <td className="px-4 py-3">{item.id}</td>

                        <td className="px-4 py-3">
                          <div className="font-medium">
                            {seccion?.tipo === "consulta"
                              ? item.nombre || nombrePersona(item)
                              : nombrePersona(item)}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            Estado: {item.estado || (item.activo === false ? "Inactivo" : "N/A")}
                          </div>
                        </td>

                        <td className="px-4 py-3">
                          {seccion?.tipo === "consulta"
                            ? textoConsulta(item)
                            : documentoPersona(item)}
                        </td>

                        <td className="px-4 py-3">
                          {detalleItem(item, seccion?.tipo)}
                        </td>

                        <td className="px-4 py-3">
                          <div className="flex justify-end">
                            <Button
                              type="button"
                              size="sm"
                              onClick={() => abrirConfirmReactivar(item)}
                              disabled={isLoading}
                            >
                              <RotateCcw className="mr-1 size-4" />
                              {isLoading ? "Reactivando..." : "Reactivar"}
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        <Pagination
          currentPage={paginaActual}
          totalPages={totalPaginas}
          onPageChange={setPaginaActual}
          pageSize={registrosPorPagina}
          onPageSizeChange={(value) => {
            setRegistrosPorPagina(value);
            setPaginaActual(1);
          }}
          pageSizeOptions={DEFAULT_PAGE_SIZE_OPTIONS}
          totalItems={totalSeccion}
        />
      </div>

      <ConfirmActionDialog
        open={confirmDialogAbierto}
        title="Reactivar registro"
        description={`¿Seguro que deseas reactivar "${itemAReactivar ? nombrePersona(itemAReactivar) : ""}"?`}
        confirmText="Reactivar"
        cancelText="Cancelar"
        loading={Boolean(reactivando)}
        variant="default"
        onConfirm={confirmarReactivarItem}
        onClose={cerrarConfirmDialog}
      />
    </div>
  );
}