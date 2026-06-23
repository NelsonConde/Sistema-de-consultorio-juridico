"use client"

/**
 * Formulario de gestión de seguimientos (tareas) de consultas jurídicas.
 *
 * Permite crear, editar y responder seguimientos según el rol del usuario.
 * Los estudiantes solo pueden ver y responder; asesores y monitores pueden
 * crear, editar, y calificar respuestas.
 *
 * @module components/forms/consulta/SeguimientosForm
 */
import React, { useEffect, useMemo, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { API_URL_BASE, FILE_STORAGE_API_URL_BASE } from "@/lib/config"
import { PERMISOS } from "@/lib/permission"
import { tieneAlgunPermiso, tienePermiso } from "@/lib/authz"
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog"
import Pagination from "@/components/ui/Pagination"
import { FormFileUpload } from "@/components/forms/parts/FormFileUpload"
import { DEFAULT_PAGE_SIZE_OPTIONS, getTotalPages, paginateItems, sortByIdAsc } from "@/lib/list-utils"

const FORM_TAREA_INICIAL = {
  categoriaId: "",
  descripcion: "",
  fechaEntrega: "",
  diasNotificacion: "",
  notificarPartes: false,
  alertaDisciplinaria: false,
  notificarEstudiante: true,
}

const FORM_RESPUESTA_INICIAL = {
  contenido: "",
  archivos: [],
}

const FORM_DECISION_INICIAL = {
  estado: "APROBADA",
  observacionRevision: "",
}

const FORM_ESTADO_SEGUIMIENTO_INICIAL = {
  estado: "COMPLETADO",
}

const ESTADOS_SEGUIMIENTO = [
  { value: "PENDIENTE", label: "Pendiente" },
  { value: "COMPLETADO", label: "Completado" },
  { value: "CANCELADO", label: "Cancelado" },
]

const PERMISOS_LEGACY = {
  GESTIONAR_CONSULTAS: "Gestionar consultas",
  GESTIONAR_SEGUIMIENTOS: "Gestionar seguimientos",
}

function normalizar(value) {
  return String(value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase()
}

function tienePerfil(user, perfil) {
  return normalizar(user?.tipoPerfil || user?.rolNombre) === normalizar(perfil)
}

function esEstudiante(user) {
  return tienePerfil(user, "ESTUDIANTE")
}

function extraerLista(data) {
  if (Array.isArray(data)) return data
  if (!data || typeof data !== "object") return []

  const claves = [
    "content",
    "data",
    "items",
    "rows",
    "consultas",
    "seguimientos",
    "tareas",
    "categorias",
    "respuestas",
    "pendientes",
    "resultado",
    "payload",
  ]

  for (const clave of claves) {
    const valor = data[clave]

    if (Array.isArray(valor)) return valor

    if (valor && typeof valor === "object") {
      const interno = extraerLista(valor)
      if (interno.length > 0) return interno
    }
  }

  return []
}

function puedeAccederTareasUsuario(user) {
  return (
    tienePermiso(user, PERMISOS.ACCEDER_TAREAS) &&
    tieneAlgunPermiso(user, [
      PERMISOS.VER_SEGUIMIENTOS,
      PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
    ])
  )
}

function puedeVerConsultasUsuario(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.VER_CONSULTAS,
    PERMISOS_LEGACY.GESTIONAR_CONSULTAS,
  ])
}

function puedeCargarCategoriasUsuario(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.VER_SEGUIMIENTOS,
    PERMISOS.CREAR_SEGUIMIENTOS,
    PERMISOS.EDITAR_SEGUIMIENTOS,
    PERMISOS.GESTIONAR_CATEGORIAS_SEGUIMIENTO,
    PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
  ])
}

function puedeCrearTarea(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.CREAR_SEGUIMIENTOS,
    PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
  ])
}

function puedeEditarTarea(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.EDITAR_SEGUIMIENTOS,
    PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
  ])
}

function puedeEliminarTarea(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.ELIMINAR_SEGUIMIENTOS,
    PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
  ])
}

function puedeResponderTarea(user) {
  return tienePermiso(user, PERMISOS.RESPONDER_SEGUIMIENTOS)
}

function puedeRevisarRespuestas(user) {
  return tienePermiso(user, PERMISOS.APROBAR_RESPUESTAS_SEGUIMIENTO)
}

function puedeVerAlertasDisciplinarias(user) {
  return tienePermiso(user, PERMISOS.VER_ALERTAS_DISCIPLINARIAS)
}

function accionPermitidaPorRegistro(item, accion, permisoGlobal) {
  const acciones = item?.accionesPermitidas

  if (acciones && typeof acciones[accion] === "boolean") {
    return acciones[accion]
  }

  return permisoGlobal
}

function labelConsulta(consulta) {
  return [
    `#${consulta.id || consulta.consultaId}`,
    consulta.consulta || consulta.descripcion || consulta.hechos || consulta.asunto,
    consulta.nombre || consulta.apellido
      ? `${consulta.nombre || ""} ${consulta.apellido || ""}`.trim()
      : "",
    consulta.cedula || consulta.documento,
  ]
    .filter(Boolean)
    .join(" - ")
}

function obtenerTextoTarea(item) {
  return (
    item.descripcion ||
    item.observacion ||
    item.detalle ||
    item.comentario ||
    "Sin descripción"
  )
}

function obtenerCategoriaTarea(item) {
  return (
    item.categoriaNombre ||
    item.categoriaSeguimientoNombre ||
    item.categoria?.nombre ||
    item.categoriaSeguimiento?.nombre ||
    item.categoria ||
    "Sin categoría"
  )
}

function obtenerCategoriaIdTarea(item) {
  return (
    item.categoriaSeguimientoId ||
    item.categoriaId ||
    item.categoria?.id ||
    item.categoriaSeguimiento?.id ||
    ""
  )
}

function obtenerAutorTarea(item) {
  return (
    item.autorNombre ||
    item.autorUsername ||
    item.autor ||
    item.username ||
    "Sin autor"
  )
}

function obtenerFechaTarea(item) {
  return (
    item.fechaCreacion ||
    item.fechaRegistro ||
    item.createdAt ||
    item.fecha ||
    ""
  )
}

function obtenerIdTarea(item) {
  return item?.id || item?.seguimientoId
}

function ordenarPorFechaDesc(lista) {
  return [...lista].sort((a, b) => {
    const fechaA = new Date(a.fechaActualizacion || a.fechaCreacion || a.fechaDecision || 0)
    const fechaB = new Date(b.fechaActualizacion || b.fechaCreacion || b.fechaDecision || 0)
    return fechaB.getTime() - fechaA.getTime()
  })
}

function ultimaRespuesta(lista = []) {
  const respuestas = ordenarPorFechaDesc(lista)
  return respuestas[0] || null
}

function getAccionRespuesta(ultima, puedeResponder) {
  if (!puedeResponder) return "NINGUNA"
  if (!ultima) return "RESPONDER"

  switch (normalizar(ultima.estado)) {
    case "PENDIENTE":
      return "EDITAR"
    case "RECHAZADA":
      return "RESPONDER_NUEVAMENTE"
    case "APROBADA":
      return "SOLO_LECTURA"
    default:
      return "NINGUNA"
  }
}

function textoAccionRespuesta(accion) {
  switch (accion) {
    case "RESPONDER":
      return "Responder"
    case "EDITAR":
      return "Editar respuesta"
    case "RESPONDER_NUEVAMENTE":
      return "Responder nuevamente"
    default:
      return "Ver respuesta"
  }
}

function estadoBadgeClass(estado) {
  switch (normalizar(estado)) {
    case "APROBADA":
      return "border-emerald-500/30 bg-emerald-500/10 text-emerald-700"
    case "RECHAZADA":
      return "border-destructive/30 bg-destructive/10 text-destructive"
    case "PENDIENTE":
      return "border-yellow-500/30 bg-yellow-500/10 text-yellow-700"
    default:
      return "border-muted bg-muted text-muted-foreground"
  }
}

function estadoSeguimientoBadgeClass(estado) {
  switch (normalizar(estado)) {
    case "COMPLETADO":
      return "border-emerald-500/30 bg-emerald-500/10 text-emerald-700"
    case "CANCELADO":
      return "border-destructive/30 bg-destructive/10 text-destructive"
    case "PENDIENTE":
      return "border-yellow-500/30 bg-yellow-500/10 text-yellow-700"
    default:
      return "border-muted bg-muted text-muted-foreground"
  }
}

function textoEstadoSeguimiento(estado) {
  const encontrado = ESTADOS_SEGUIMIENTO.find((item) => item.value === normalizar(estado))
  return encontrado?.label || estado || "Sin estado"
}

function consultaPermiteOperaciones(consulta) {
  const estado = normalizar(consulta?.estado)
  return estado !== "CERRADO" && estado !== "ARCHIVADO"
}

function seguimientoPermiteOperaciones(tarea) {
  return normalizar(tarea?.estado || "PENDIENTE") === "PENDIENTE"
}

function seguimientoEstaVencido(tarea) {
  if (!tarea?.fechaEntrega || !seguimientoPermiteOperaciones(tarea)) return false

  const hoy = new Date()
  hoy.setHours(0, 0, 0, 0)

  const fechaEntrega = new Date(`${tarea.fechaEntrega}T00:00:00`)
  return fechaEntrega.getTime() < hoy.getTime()
}

function pathRespuesta(seguimientoId, respuestaId) {
  return `tareas-${seguimientoId}-respuestas-${respuestaId}`
}

async function leerRespuesta(response) {
  const text = await response.text()

  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return { mensaje: text }
  }
}

function mensajeError(data, defecto) {
  return data?.mensaje || data?.message || data?.error || defecto
}

export function SeguimientosForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const searchQuery = searchParams.get('search') || ""
  const FILES_API = FILE_STORAGE_API_URL_BASE || API_URL_BASE

  const [user, setUser] = useState(null)
  const [consultas, setConsultas] = useState([])
  const [categorias, setCategorias] = useState([])
  const [tareas, setTareas] = useState([])
  const [respuestasPorTarea, setRespuestasPorTarea] = useState({})
  const [pendientesRevision, setPendientesRevision] = useState([])
  const [consultaSeleccionada, setConsultaSeleccionada] = useState(null)
  const [busquedaLocal, setBusquedaLocal] = useState(searchQuery)

  useEffect(() => {
    if (searchQuery) {
      setBusquedaLocal(searchQuery)
    }
  }, [searchQuery])
  const [paso, setPaso] = useState(1)
  const [formTarea, setFormTarea] = useState(FORM_TAREA_INICIAL)
  const [tareaEditando, setTareaEditando] = useState(null)
  const [tareaRespuesta, setTareaRespuesta] = useState(null)
  const [respuestaEditando, setRespuestaEditando] = useState(null)
  const [respuestaDecision, setRespuestaDecision] = useState(null)
  const [formDecision, setFormDecision] = useState(FORM_DECISION_INICIAL)
  const [seguimientoEstado, setSeguimientoEstado] = useState(null)
  const [formEstadoSeguimiento, setFormEstadoSeguimiento] = useState(FORM_ESTADO_SEGUIMIENTO_INICIAL)
  const [loading, setLoading] = useState(true)
  const [loadingTareas, setLoadingTareas] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [subiendoRespuesta, setSubiendoRespuesta] = useState(false)
  const [guardandoDecision, setGuardandoDecision] = useState(false)
  const [guardandoEstadoSeguimiento, setGuardandoEstadoSeguimiento] = useState(false)
  const [archivosPorRespuesta, setArchivosPorRespuesta] = useState({})
  const [cargandoArchivosRespuesta, setCargandoArchivosRespuesta] = useState({})
  const [archivosTarea, setArchivosTarea] = useState([])
  const [archivosPorTarea, setArchivosPorTarea] = useState({})
  const [cargandoArchivosTarea, setCargandoArchivosTarea] = useState({})
  const [tareaAEliminar, setTareaAEliminar] = useState(null)
  const [eliminando, setEliminando] = useState(false)
  const [paginaConsultas, setPaginaConsultas] = useState(1)
  const [registrosPorPaginaConsultas, setRegistrosPorPaginaConsultas] = useState(10)
  const [paginaTareas, setPaginaTareas] = useState(1)
  const [registrosPorPaginaTareas, setRegistrosPorPaginaTareas] = useState(10)
  const [paginaPendientes, setPaginaPendientes] = useState(1)
  const [registrosPorPaginaPendientes, setRegistrosPorPaginaPendientes] = useState(5)

  const {
    register,
    setValue,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: FORM_RESPUESTA_INICIAL,
  })

  const archivosRespuesta = watch("archivos")

  const puedeCrear = useMemo(() => puedeCrearTarea(user), [user])
  const puedeEditar = useMemo(() => puedeEditarTarea(user), [user])
  const puedeEliminar = useMemo(() => puedeEliminarTarea(user), [user])
  const puedeResponder = useMemo(() => puedeResponderTarea(user), [user])
  const puedeRevisar = useMemo(() => puedeRevisarRespuestas(user), [user])
  const puedeVerAlertas = useMemo(() => puedeVerAlertasDisciplinarias(user), [user])
  const usuarioEstudiante = useMemo(() => esEstudiante(user), [user])

  const consultasFiltradas = useMemo(() => {
    const texto = busquedaLocal.trim().toLowerCase()

    const filtradas = !texto
      ? consultas
      : consultas.filter((consulta) =>
      [
        consulta.id,
        consulta.consulta,
        consulta.descripcion,
        consulta.hechos,
        consulta.fecha,
        consulta.nombre,
        consulta.apellido,
        consulta.cedula,
        consulta.documento,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(texto)
    )

    return sortByIdAsc(filtradas)
  }, [consultas, busquedaLocal])

  useEffect(() => {
    cargarInicial()
  }, [])

  async function apiRequest(url, options = {}) {
    const res = await fetch(url, {
      credentials: "include",
      headers:
        options.body instanceof FormData
          ? options.headers || {}
          : { "Content-Type": "application/json", ...(options.headers || {}) },
      ...options,
    })

    const data = await leerRespuesta(res)

    if (res.status === 401) {
      router.push("/")
      throw new Error("Sesión vencida")
    }

    if (res.status === 403) {
      throw new Error("No tienes permisos para realizar esta acción")
    }

    if (!res.ok) {
      throw new Error(mensajeError(data, "No se pudo procesar la solicitud"))
    }

    return data
  }

  async function fetchLista(url, mensaje403) {
    try {
      const res = await fetch(url, { credentials: "include" })
      const data = await leerRespuesta(res)

      if (res.status === 401) {
        router.push("/")
        return []
      }

      if (res.status === 403) {
        throw new Error(mensaje403 || "No tienes permisos para consultar esta información")
      }

      if (!res.ok) {
        throw new Error(mensajeError(data, "No se pudo cargar la información solicitada"))
      }

      return extraerLista(data)
    } catch (error) {
      throw error
    }
  }

  async function cargarInicial() {
    try {
      setLoading(true)

      const meRes = await fetch(`${API_URL_BASE}/auth/me`, {
        credentials: "include",
      })

      if (meRes.status === 401) {
        router.push("/")
        return
      }

      if (!meRes.ok) {
        toast.error("No se pudo verificar la sesión")
        return
      }

      const meData = await meRes.json()
      setUser(meData)

      if (!puedeAccederTareasUsuario(meData)) {
        toast.error("No tienes permisos para acceder a tareas")
        router.push("/inicio")
        return
      }

      const consultasPermitidas = puedeVerConsultasUsuario(meData)
      const categoriasPermitidas = puedeCargarCategoriasUsuario(meData)
      const revisarPermitido = puedeRevisarRespuestas(meData)

      const [categoriasRes, consultasRes, pendientesRes] = await Promise.allSettled([
        categoriasPermitidas
          ? fetchLista(
              `${API_URL_BASE}/seguimientos/categorias/activas`,
              "No tienes permiso para consultar categorías"
            )
          : Promise.resolve([]),
        consultasPermitidas
          ? fetchLista(
              `${API_URL_BASE}/consultas`,
              "No tienes permiso para consultar consultas"
            )
          : Promise.resolve([]),
        revisarPermitido ? cargarPendientesRevision(false) : Promise.resolve([]),
      ])

      if (categoriasRes.status === "fulfilled") setCategorias(categoriasRes.value)
      if (consultasRes.status === "fulfilled") setConsultas(sortByIdAsc(consultasRes.value))
      if (pendientesRes.status === "fulfilled") setPendientesRevision(sortByIdAsc(pendientesRes.value))

      const errorCarga = [categoriasRes, consultasRes, pendientesRes].find(
        (item) => item.status === "rejected"
      )

      if (errorCarga) {
        toast.error(errorCarga.reason?.message || "No se pudo cargar toda la información")
      }
    } catch (error) {
      console.error(error)
      toast.error(error.message || "Error cargando datos")
    } finally {
      setLoading(false)
    }
  }

  async function cargarPendientesRevision(mostrarToast = true) {
    try {
      const data = await apiRequest(`${API_URL_BASE}/seguimientos/respuestas/pendientes`)
      const pendientes = extraerLista(data)

      if (mostrarToast) {
        toast.success("Pendientes actualizados")
      }

      pendientes.forEach((respuesta) => {
        if (respuesta.seguimientoId && respuesta.id) {
          cargarArchivosRespuesta(respuesta.seguimientoId, respuesta.id)
        }
      })

      return sortByIdAsc(pendientes)
    } catch (error) {
      if (mostrarToast) toast.error(error.message || "No se pudieron cargar pendientes")
      throw error
    }
  }

  async function refrescarPendientesRevision() {
    const pendientes = await cargarPendientesRevision(true)
    setPendientesRevision(sortByIdAsc(pendientes))
  }

  async function cargarRespuestasPorSeguimiento(seguimientoId) {
    try {
      const data = await apiRequest(`${API_URL_BASE}/seguimientos/${seguimientoId}/respuestas`)
      const respuestas = extraerLista(data)

      setRespuestasPorTarea((prev) => ({
        ...prev,
        [seguimientoId]: respuestas,
      }))

      respuestas.forEach((respuesta) => {
        if (respuesta.id) {
          cargarArchivosRespuesta(seguimientoId, respuesta.id)
        }
      })

      return respuestas
    } catch (error) {
      console.error(`Error cargando respuestas de seguimiento ${seguimientoId}:`, error)
      setRespuestasPorTarea((prev) => ({
        ...prev,
        [seguimientoId]: [],
      }))
      return []
    }
  }

  async function cargarTareasPorConsulta(consultaId) {
    try {
      setLoadingTareas(true)
      setRespuestasPorTarea({})
      setArchivosPorTarea({})

      const endpoint = usuarioEstudiante
        ? `${API_URL_BASE}/seguimientos/consulta/${consultaId}/visibles-estudiante`
        : `${API_URL_BASE}/seguimientos/consulta/${consultaId}`

      const tareasData = await fetchLista(
        endpoint,
        "No tienes permisos para consultar las tareas"
      )

      setTareas(sortByIdAsc(tareasData))

      await Promise.allSettled([
        ...tareasData.map((item) => cargarRespuestasPorSeguimiento(obtenerIdTarea(item))),
        ...tareasData.map((item) => cargarArchivosTarea(obtenerIdTarea(item))),
      ])
    } catch (error) {
      console.error(error)
      toast.error(error.message || "Error cargando tareas")
    } finally {
      setLoadingTareas(false)
    }
  }

  async function seleccionarConsulta(consulta) {
    setConsultaSeleccionada(consulta)
    setPaso(2)
    setTareaEditando(null)
    setTareaRespuesta(null)
    setRespuestaEditando(null)
    setRespuestaDecision(null)
    setSeguimientoEstado(null)
    setFormEstadoSeguimiento(FORM_ESTADO_SEGUIMIENTO_INICIAL)
    setTareaAEliminar(null)
    setFormTarea(FORM_TAREA_INICIAL)
    setArchivosTarea([])
    reset(FORM_RESPUESTA_INICIAL)
    await cargarTareasPorConsulta(consulta.id || consulta.consultaId)
  }

  function volverAConsultas() {
    setPaso(1)
    setConsultaSeleccionada(null)
    setTareas([])
    setRespuestasPorTarea({})
    setTareaEditando(null)
    setTareaRespuesta(null)
    setRespuestaEditando(null)
    setRespuestaDecision(null)
    setSeguimientoEstado(null)
    setFormEstadoSeguimiento(FORM_ESTADO_SEGUIMIENTO_INICIAL)
    setTareaAEliminar(null)
    setFormTarea(FORM_TAREA_INICIAL)
    setArchivosTarea([])
    reset(FORM_RESPUESTA_INICIAL)
  }

  function handleTareaChange(event) {
    const { name, value, type, checked } = event.target

    setFormTarea((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }))
  }

  function limpiarFormTarea() {
    setFormTarea(FORM_TAREA_INICIAL)
    setTareaEditando(null)
    setArchivosTarea([])
  }

  function editarTarea(tarea) {
    setTareaEditando(tarea)
    setFormTarea({
      categoriaId: obtenerCategoriaIdTarea(tarea),
      descripcion: obtenerTextoTarea(tarea),
      fechaEntrega: tarea.fechaEntrega || "",
      diasNotificacion: tarea.diasNotificacion ?? "",
      notificarPartes: Boolean(tarea.notificarPartes),
      alertaDisciplinaria: Boolean(tarea.alertaDisciplinaria),
      notificarEstudiante: tarea.notificarEstudiante !== false,
    })
  }

  async function guardarTarea(event) {
    event.preventDefault()

    if (!puedeCrear && !tareaEditando) {
      toast.error("No tienes permisos para crear tareas")
      return
    }

    if (!puedeEditar && tareaEditando) {
      toast.error("No tienes permisos para editar tareas")
      return
    }

    if (!consultaSeleccionada) {
      toast.error("Selecciona una consulta")
      return
    }

    if (!consultaPermiteOperaciones(consultaSeleccionada)) {
      toast.error("No se pueden crear ni editar tareas en consultas cerradas o archivadas")
      return
    }

    if (!formTarea.categoriaId) {
      toast.error("Selecciona una categoría")
      return
    }

    if (!formTarea.descripcion.trim()) {
      toast.error("La descripción es obligatoria")
      return
    }

    if (formTarea.diasNotificacion !== "" && Number(formTarea.diasNotificacion) < 0) {
      toast.error("Los días de notificación no pueden ser negativos")
      return
    }

    if (formTarea.diasNotificacion !== "" && !formTarea.fechaEntrega) {
      toast.error("Para usar días de notificación debes indicar fecha de entrega")
      return
    }

    try {
      setGuardando(true)

      const payload = {
        consultaId: Number(consultaSeleccionada.id || consultaSeleccionada.consultaId),
        categoriaSeguimientoId: Number(formTarea.categoriaId),
        descripcion: formTarea.descripcion.trim(),
        fechaEntrega: formTarea.fechaEntrega || null,
        diasNotificacion: formTarea.diasNotificacion === "" ? null : Number(formTarea.diasNotificacion),
        notificarPartes: Boolean(formTarea.notificarPartes),
        alertaDisciplinaria: Boolean(formTarea.alertaDisciplinaria),
        notificarEstudiante: Boolean(formTarea.notificarEstudiante),
      }

      const url = tareaEditando
        ? `${API_URL_BASE}/seguimientos/${obtenerIdTarea(tareaEditando)}`
        : `${API_URL_BASE}/seguimientos`

      const method = tareaEditando ? "PUT" : "POST"

      const result = await apiRequest(url, {
        method,
        body: JSON.stringify(payload),
      })

      const seguimientoId = result?.id || obtenerIdTarea(tareaEditando)

      if (seguimientoId && archivosTarea.length > 0) {
        try {
          await subirDocumentosTarea(seguimientoId, archivosTarea)
          await cargarArchivosTarea(seguimientoId)
        } catch (error) {
          console.error(error)
          toast.warning("La tarea se guardó, pero hubo un problema subiendo los documentos")
        }
      }

      toast.success(tareaEditando ? "Tarea actualizada" : "Tarea creada")
      limpiarFormTarea()
      setArchivosTarea([])
      await cargarTareasPorConsulta(consultaSeleccionada.id || consultaSeleccionada.consultaId)
    } catch (error) {
      console.error(error)
      toast.error(error.message || "Error guardando tarea")
    } finally {
      setGuardando(false)
    }
  }

  function pedirConfirmacionEliminar(tarea) {
    if (!puedeEliminar) {
      toast.error("No tienes permisos para eliminar tareas")
      return
    }

    if (!consultaPermiteOperaciones(consultaSeleccionada)) {
      toast.error("No se pueden eliminar tareas en consultas cerradas o archivadas")
      return
    }

    if (!seguimientoPermiteOperaciones(tarea)) {
      toast.error("Solo se pueden eliminar tareas pendientes")
      return
    }

    setTareaAEliminar(tarea)
  }

  async function confirmarEliminarTarea() {
    if (!tareaAEliminar) return

    try {
      setEliminando(true)

      await apiRequest(`${API_URL_BASE}/seguimientos/${obtenerIdTarea(tareaAEliminar)}`, {
        method: "DELETE",
      })

      toast.success("Tarea eliminada correctamente")
      setTareaAEliminar(null)

      if (consultaSeleccionada?.id || consultaSeleccionada?.consultaId) {
        await cargarTareasPorConsulta(consultaSeleccionada.id || consultaSeleccionada.consultaId)
      }
    } catch (error) {
      console.error(error)
      toast.error(error.message || "Error eliminando tarea")
    } finally {
      setEliminando(false)
    }
  }

  function abrirRespuesta(tarea) {
    if (!consultaPermiteOperaciones(consultaSeleccionada)) {
      toast.error("No se puede responder una tarea de una consulta cerrada o archivada")
      return
    }

    if (!seguimientoPermiteOperaciones(tarea)) {
      toast.error("Solo se pueden responder tareas pendientes")
      return
    }

    const seguimientoId = obtenerIdTarea(tarea)
    const ultima = ultimaRespuesta(respuestasPorTarea[seguimientoId] || [])
    const accion = getAccionRespuesta(ultima, puedeResponder)

    if (accion === "SOLO_LECTURA" || accion === "NINGUNA") return

    setTareaRespuesta(tarea)
    setRespuestaEditando(accion === "EDITAR" ? ultima : null)
    reset({
      contenido: accion === "EDITAR" ? ultima?.contenido || "" : "",
      archivos: [],
    })
  }

  function cerrarRespuesta() {
    setTareaRespuesta(null)
    setRespuestaEditando(null)
    reset(FORM_RESPUESTA_INICIAL)
  }

  function pathTarea(seguimientoId) {
    return `tareas-${seguimientoId}-documentos`
  }

  async function subirDocumentosTarea(seguimientoId, archivos) {
    if (!archivos || archivos.length === 0) return

    const formData = new FormData()

    archivos.forEach((file) => {
      formData.append("files", file)
    })

    formData.append("path", pathTarea(seguimientoId))

    const res = await fetch(`${FILES_API}/files/upload-multiple`, {
      method: "POST",
      credentials: "include",
      body: formData,
    })

    if (!res.ok) {
      throw new Error("La tarea se guardó, pero ocurrió un error subiendo los documentos")
    }
  }

  async function cargarArchivosTarea(seguimientoId) {
    if (!seguimientoId) return

    const path = pathTarea(seguimientoId)

    try {
      setCargandoArchivosTarea((prev) => ({
        ...prev,
        [seguimientoId]: true,
      }))

      const res = await fetch(
        `${FILES_API}/files/list/${encodeURIComponent(path)}`,
        {
          credentials: "include",
        }
      )

      if (!res.ok) {
        setArchivosPorTarea((prev) => ({
          ...prev,
          [seguimientoId]: [],
        }))
        return
      }

      const data = await res.json()

      setArchivosPorTarea((prev) => ({
        ...prev,
        [seguimientoId]: Array.isArray(data) ? data : extraerLista(data),
      }))
    } catch (error) {
      console.error("Error cargando archivos de tarea:", error)

      setArchivosPorTarea((prev) => ({
        ...prev,
        [seguimientoId]: [],
      }))
    } finally {
      setCargandoArchivosTarea((prev) => ({
        ...prev,
        [seguimientoId]: false,
      }))
    }
  }

  async function descargarArchivoTarea(seguimientoId, fileName) {
    const path = pathTarea(seguimientoId)

    try {
      const res = await fetch(
        `${FILES_API}/files/download/${encodeURIComponent(path)}/${encodeURIComponent(fileName)}`,
        {
          method: "GET",
          credentials: "include",
        }
      )

      if (!res.ok) {
        throw new Error("No se pudo descargar el archivo")
      }

      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)

      const a = document.createElement("a")
      a.href = url
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      a.remove()

      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error("Error descargando archivo:", error)
      toast.error(error.message || "Error descargando archivo")
    }
  }

  async function subirDocumentosRespuesta(seguimientoId, respuestaId, archivos) {
    if (!archivos || archivos.length === 0) return

    const formData = new FormData()

    archivos.forEach((file) => {
      formData.append("files", file)
    })

    formData.append("path", pathRespuesta(seguimientoId, respuestaId))

    const res = await fetch(`${FILES_API}/files/upload-multiple`, {
      method: "POST",
      credentials: "include",
      body: formData,
    })

    if (!res.ok) {
      throw new Error("La respuesta se guardó, pero ocurrió un error subiendo los documentos")
    }
  }

  async function cargarArchivosRespuesta(seguimientoId, respuestaId) {
    if (!seguimientoId || !respuestaId) return

    const path = pathRespuesta(seguimientoId, respuestaId)

    try {
      setCargandoArchivosRespuesta((prev) => ({
        ...prev,
        [respuestaId]: true,
      }))

      const res = await fetch(
        `${FILES_API}/files/list/${encodeURIComponent(path)}`,
        {
          credentials: "include",
        }
      )

      if (!res.ok) {
        setArchivosPorRespuesta((prev) => ({
          ...prev,
          [respuestaId]: [],
        }))
        return
      }

      const data = await res.json()

      setArchivosPorRespuesta((prev) => ({
        ...prev,
        [respuestaId]: Array.isArray(data) ? data : extraerLista(data),
      }))
    } catch (error) {
      console.error("Error cargando archivos de respuesta:", error)

      setArchivosPorRespuesta((prev) => ({
        ...prev,
        [respuestaId]: [],
      }))
    } finally {
      setCargandoArchivosRespuesta((prev) => ({
        ...prev,
        [respuestaId]: false,
      }))
    }
  }

  async function descargarArchivoRespuesta(seguimientoId, respuestaId, fileName) {
    const path = pathRespuesta(seguimientoId, respuestaId)

    try {
      const res = await fetch(
        `${FILES_API}/files/download/${encodeURIComponent(path)}/${encodeURIComponent(fileName)}`,
        {
          method: "GET",
          credentials: "include",
        }
      )

      if (!res.ok) {
        throw new Error("No se pudo descargar el archivo")
      }

      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)

      const a = document.createElement("a")
      a.href = url
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      a.remove()

      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error("Error descargando archivo:", error)
      toast.error(error.message || "Error descargando archivo")
    }
  }

  async function guardarRespuesta(data) {
    if (!puedeResponder) {
      toast.error("No tienes permiso para responder tareas")
      return
    }

    if (!consultaSeleccionada || !tareaRespuesta) {
      toast.error("Selecciona una tarea")
      return
    }

    if (!consultaPermiteOperaciones(consultaSeleccionada)) {
      toast.error("No se puede responder una tarea de una consulta cerrada o archivada")
      return
    }

    if (!seguimientoPermiteOperaciones(tareaRespuesta)) {
      toast.error("Solo se pueden responder tareas pendientes")
      return
    }

    if (!data.contenido?.trim()) {
      toast.error("Escribe la respuesta")
      return
    }

    try {
      setSubiendoRespuesta(true)

      const seguimientoId = obtenerIdTarea(tareaRespuesta)
      const payload = { contenido: data.contenido.trim() }
      const url = respuestaEditando
        ? `${API_URL_BASE}/seguimientos/respuestas/${respuestaEditando.id}`
        : `${API_URL_BASE}/seguimientos/${seguimientoId}/respuestas`
      const method = respuestaEditando ? "PUT" : "POST"

      const result = await apiRequest(url, {
        method,
        body: JSON.stringify(payload),
      })

      const respuestaId = result?.id || respuestaEditando?.id
      const archivos = Array.isArray(data.archivos) ? data.archivos : []

      if (respuestaId) {
        await subirDocumentosRespuesta(seguimientoId, respuestaId, archivos)
        await cargarArchivosRespuesta(seguimientoId, respuestaId)
      }

      toast.success(respuestaEditando ? "Respuesta actualizada" : "Respuesta enviada correctamente")
      cerrarRespuesta()
      await cargarRespuestasPorSeguimiento(seguimientoId)
    } catch (error) {
      console.error(error)
      toast.error(error.message || "Error enviando respuesta")
    } finally {
      setSubiendoRespuesta(false)
    }
  }

  function abrirDecision(respuesta, estado) {
    setRespuestaDecision(respuesta)
    setFormDecision({
      estado,
      observacionRevision: respuesta.observacionRevision || "",
    })
  }

  async function guardarDecision(event) {
    event.preventDefault()

    if (!puedeRevisar) {
      toast.error("No tienes permiso para revisar respuestas")
      return
    }

    if (!respuestaDecision?.id) {
      toast.error("Selecciona una respuesta")
      return
    }

    if (!formDecision.observacionRevision.trim()) {
      toast.error("Escribe una observación")
      return
    }

    try {
      setGuardandoDecision(true)

      await apiRequest(`${API_URL_BASE}/seguimientos/respuestas/${respuestaDecision.id}/decision`, {
        method: "PATCH",
        body: JSON.stringify({
          estado: formDecision.estado,
          observacionRevision: formDecision.observacionRevision.trim(),
        }),
      })

      toast.success(
        formDecision.estado === "APROBADA"
          ? "Respuesta aprobada"
          : "Respuesta rechazada"
      )

      setRespuestaDecision(null)
      setFormDecision(FORM_DECISION_INICIAL)
      await refrescarPendientesRevision()

      if (consultaSeleccionada?.id || consultaSeleccionada?.consultaId) {
        await cargarTareasPorConsulta(consultaSeleccionada.id || consultaSeleccionada.consultaId)
      }
    } catch (error) {
      console.error(error)
      toast.error(error.message || "No se pudo guardar la decisión")
    } finally {
      setGuardandoDecision(false)
    }
  }


  function abrirCambioEstadoSeguimiento(tarea) {
    if (!puedeEditar) {
      toast.error("No tienes permisos para cambiar el estado de tareas")
      return
    }

    if (!consultaPermiteOperaciones(consultaSeleccionada)) {
      toast.error("No se puede cambiar el estado de tareas en consultas cerradas o archivadas")
      return
    }

    const estadoActual = normalizar(tarea?.estado || "PENDIENTE")
    const estadoSugerido = estadoActual === "PENDIENTE" ? "COMPLETADO" : estadoActual

    setSeguimientoEstado(tarea)
    setFormEstadoSeguimiento({ estado: estadoSugerido })
  }

  function cerrarCambioEstadoSeguimiento() {
    setSeguimientoEstado(null)
    setFormEstadoSeguimiento(FORM_ESTADO_SEGUIMIENTO_INICIAL)
  }

  async function guardarCambioEstadoSeguimiento(event) {
    event.preventDefault()

    if (!seguimientoEstado) {
      toast.error("Selecciona una tarea")
      return
    }

    if (!formEstadoSeguimiento.estado) {
      toast.error("Selecciona el nuevo estado")
      return
    }

    try {
      setGuardandoEstadoSeguimiento(true)

      await apiRequest(
        `${API_URL_BASE}/seguimientos/${obtenerIdTarea(seguimientoEstado)}/estado?estado=${encodeURIComponent(formEstadoSeguimiento.estado)}`,
        { method: "PATCH" }
      )

      toast.success("Estado de la tarea actualizado")
      cerrarCambioEstadoSeguimiento()

      if (consultaSeleccionada?.id || consultaSeleccionada?.consultaId) {
        await cargarTareasPorConsulta(consultaSeleccionada.id || consultaSeleccionada.consultaId)
      }

      if (puedeRevisar) {
        await refrescarPendientesRevision()
      }
    } catch (error) {
      console.error(error)
      toast.error(error.message || "No se pudo cambiar el estado de la tarea")
    } finally {
      setGuardandoEstadoSeguimiento(false)
    }
  }

  function renderArchivosRespuesta(respuesta) {
    if (!respuesta?.id) return null

    const seguimientoId = respuesta.seguimientoId || respuesta.seguimiento?.id
    const archivos = archivosPorRespuesta[respuesta.id] || []

    return (
      <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm font-medium">Documentos entregados</p>

          {seguimientoId && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => cargarArchivosRespuesta(seguimientoId, respuesta.id)}
            >
              Ver archivos
            </Button>
          )}
        </div>

        {cargandoArchivosRespuesta[respuesta.id] ? (
          <p className="text-sm text-muted-foreground">Cargando archivos...</p>
        ) : archivos.length > 0 ? (
          <ul className="space-y-2">
            {archivos.map((fileName) => (
              <li
                key={fileName}
                className="flex items-center justify-between gap-3 rounded-md border bg-background px-3 py-2 text-sm"
              >
                <span className="truncate">{fileName}</span>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => descargarArchivoRespuesta(seguimientoId, respuesta.id, fileName)}
                >
                  Descargar
                </Button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-muted-foreground">No hay documentos entregados.</p>
        )}
      </div>
    )
  }

  function renderArchivosTarea(tarea) {
    if (!tarea) return null

    const seguimientoId = obtenerIdTarea(tarea)
    const archivos = archivosPorTarea[seguimientoId] || []

    return (
      <div className="rounded-lg border bg-muted/30 p-3 space-y-2">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm font-medium">Documentos del seguimiento</p>

          {seguimientoId && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => cargarArchivosTarea(seguimientoId)}
            >
              Ver archivos
            </Button>
          )}
        </div>

        {cargandoArchivosTarea[seguimientoId] ? (
          <p className="text-sm text-muted-foreground">Cargando archivos...</p>
        ) : archivos.length > 0 ? (
          <ul className="space-y-2">
            {archivos.map((fileName) => (
              <li
                key={fileName}
                className="flex items-center justify-between gap-3 rounded-md border bg-background px-3 py-2 text-sm"
              >
                <span className="truncate">{fileName}</span>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => descargarArchivoTarea(seguimientoId, fileName)}
                >
                  Descargar
                </Button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-muted-foreground">No hay documentos adjuntos al seguimiento.</p>
        )}
      </div>
    )
  }

  const tareasOrdenadas = useMemo(() => sortByIdAsc(tareas), [tareas])
  const pendientesOrdenados = useMemo(() => sortByIdAsc(pendientesRevision), [pendientesRevision])

  const totalPaginasConsultas = getTotalPages(consultasFiltradas.length, registrosPorPaginaConsultas)
  const consultasPaginadas = useMemo(
    () => paginateItems(consultasFiltradas, paginaConsultas, registrosPorPaginaConsultas),
    [consultasFiltradas, paginaConsultas, registrosPorPaginaConsultas]
  )

  const totalPaginasTareas = getTotalPages(tareasOrdenadas.length, registrosPorPaginaTareas)
  const tareasPaginadas = useMemo(
    () => paginateItems(tareasOrdenadas, paginaTareas, registrosPorPaginaTareas),
    [tareasOrdenadas, paginaTareas, registrosPorPaginaTareas]
  )

  const totalPaginasPendientes = getTotalPages(pendientesOrdenados.length, registrosPorPaginaPendientes)
  const pendientesPaginados = useMemo(
    () => paginateItems(pendientesOrdenados, paginaPendientes, registrosPorPaginaPendientes),
    [pendientesOrdenados, paginaPendientes, registrosPorPaginaPendientes]
  )

  useEffect(() => {
    setPaginaConsultas(1)
  }, [busquedaLocal, registrosPorPaginaConsultas])

  useEffect(() => {
    if (paginaConsultas > totalPaginasConsultas) {
      setPaginaConsultas(totalPaginasConsultas)
    }
  }, [paginaConsultas, totalPaginasConsultas])

  useEffect(() => {
    setPaginaTareas(1)
  }, [consultaSeleccionada, registrosPorPaginaTareas])

  useEffect(() => {
    if (paginaTareas > totalPaginasTareas) {
      setPaginaTareas(totalPaginasTareas)
    }
  }, [paginaTareas, totalPaginasTareas])

  useEffect(() => {
    if (paginaPendientes > totalPaginasPendientes) {
      setPaginaPendientes(totalPaginasPendientes)
    }
  }, [paginaPendientes, totalPaginasPendientes])

  if (loading) {
    return <div className="text-center py-10">Cargando tareas...</div>
  }

  return (
    <div className="space-y-6">
      {puedeRevisar && (
        <div className="rounded-xl border bg-card p-5 space-y-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h2 className="text-xl font-bold">Respuestas pendientes de revisión</h2>
            </div>

            <Button type="button" variant="outline" onClick={refrescarPendientesRevision}>
              Actualizar pendientes
            </Button>
          </div>

          {pendientesOrdenados.length === 0 ? (
            <div className="rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground">
              No hay respuestas pendientes por revisar.
            </div>
          ) : (
            <div className="space-y-3">
              {pendientesPaginados.map((respuesta) => (
                <div key={respuesta.id} className="rounded-lg border bg-background p-4 space-y-3">
                  <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                    <div className="space-y-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className={`rounded-full border px-2.5 py-1 text-xs font-medium ${estadoBadgeClass(respuesta.estado)}`}>
                          {respuesta.estado || "PENDIENTE"}
                        </span>

                        {respuesta.fueraPlazo && (
                          <span className="rounded-full border border-yellow-500/30 bg-yellow-500/10 px-2.5 py-1 text-xs font-medium text-yellow-700">
                            Fuera de plazo
                          </span>
                        )}
                      </div>

                      <p className="text-sm whitespace-pre-line">{respuesta.contenido}</p>

                      <div className="grid grid-cols-1 gap-2 text-xs text-muted-foreground md:grid-cols-3">
                        <div><span className="font-medium text-foreground">Seguimiento: </span>#{respuesta.seguimientoId}</div>
                        <div><span className="font-medium text-foreground">Consulta: </span>#{respuesta.consultaId}</div>
                        <div><span className="font-medium text-foreground">Estudiante: </span>{respuesta.estudianteNombre || "N/A"}</div>
                      </div>
                    </div>

                    <div className="flex flex-wrap gap-2">
                      <Button type="button" size="sm" onClick={() => abrirDecision(respuesta, "APROBADA")}>
                        Aprobar
                      </Button>

                      <Button type="button" size="sm" variant="destructive" onClick={() => abrirDecision(respuesta, "RECHAZADA")}>
                        Rechazar
                      </Button>
                    </div>
                  </div>

                  {renderArchivosRespuesta(respuesta)}
                </div>
              ))}

              <Pagination
                currentPage={paginaPendientes}
                totalPages={totalPaginasPendientes}
                onPageChange={setPaginaPendientes}
                pageSize={registrosPorPaginaPendientes}
                onPageSizeChange={(value) => {
                  setRegistrosPorPaginaPendientes(value)
                  setPaginaPendientes(1)
                }}
                pageSizeOptions={DEFAULT_PAGE_SIZE_OPTIONS}
                totalItems={pendientesOrdenados.length}
              />
            </div>
          )}
        </div>
      )}

      {paso === 1 && (
        <div className="rounded-xl border bg-card p-5 space-y-4">
          <div>
            <h2 className="text-xl font-bold">Seleccionar consulta</h2>
            <p className="text-sm text-muted-foreground">
              Selecciona una consulta para ver o gestionar sus tareas. La tabla muestra solo las consultas permitidas para tu usuario.
            </p>
          </div>

          <input
            value={busquedaLocal}
            onChange={(event) => setBusquedaLocal(event.target.value)}
            placeholder="Buscar por ID, descripción, nombre, apellido, cédula o fecha..."
            className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />

          <div className="overflow-hidden rounded-lg border">
            <table className="w-full text-sm">
              <thead className="bg-muted">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">ID</th>
                  <th className="px-4 py-3 text-left font-medium">Consulta</th>
                  <th className="px-4 py-3 text-left font-medium">Fecha</th>
                  <th className="px-4 py-3 text-left font-medium">Persona</th>
                  <th className="px-4 py-3 text-left font-medium">Cédula</th>
                  <th className="px-4 py-3 text-left font-medium">Estado</th>
                  <th className="px-4 py-3 text-right font-medium">Acción</th>
                </tr>
              </thead>

              <tbody>
                {consultasFiltradas.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-muted-foreground">
                      No hay consultas para mostrar.
                    </td>
                  </tr>
                ) : (
                  consultasPaginadas.map((consulta) => (
                    <tr
                      key={consulta.id || consulta.consultaId}
                      onClick={() => seleccionarConsulta(consulta)}
                      className="border-t transition hover:bg-muted/40 cursor-pointer"
                    >
                      <td className="px-4 py-3">{consulta.id || consulta.consultaId}</td>
                      <td className="px-4 py-3 max-w-[360px] truncate" title={consulta.consulta || consulta.descripcion}>
                        {consulta.consulta || consulta.descripcion || consulta.hechos || "Sin descripción"}
                      </td>
                      <td className="px-4 py-3">{consulta.fecha || consulta.fechaCreacion || "Sin fecha"}</td>
                      <td className="px-4 py-3">
                        {[consulta.nombre, consulta.apellido].filter(Boolean).join(" ") || "N/A"}
                      </td>
                      <td className="px-4 py-3">{consulta.cedula || consulta.documento || "N/A"}</td>
                      <td className="px-4 py-3">
                        <span className="rounded-full border bg-muted px-2.5 py-1 text-xs font-medium">
                          {consulta.estado || "Sin estado"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={(event) => {
                            event.stopPropagation()
                            seleccionarConsulta(consulta)
                          }}
                        >
                          Abrir tareas
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <Pagination
            currentPage={paginaConsultas}
            totalPages={totalPaginasConsultas}
            onPageChange={setPaginaConsultas}
            pageSize={registrosPorPaginaConsultas}
            onPageSizeChange={(value) => {
              setRegistrosPorPaginaConsultas(value)
              setPaginaConsultas(1)
            }}
            pageSizeOptions={DEFAULT_PAGE_SIZE_OPTIONS}
            totalItems={consultasFiltradas.length}
          />
        </div>
      )}

      {paso === 2 && (
        <div className="space-y-6">
          <div className="rounded-xl border bg-card p-5 space-y-3">
            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div>
                <h2 className="text-xl font-bold">Tareas de la consulta</h2>
                <p className="text-sm text-muted-foreground">
                  {labelConsulta(consultaSeleccionada)}
                </p>

                {!consultaPermiteOperaciones(consultaSeleccionada) && (
                  <p className="mt-2 rounded-md border border-yellow-500/30 bg-yellow-500/10 px-3 py-2 text-sm text-yellow-700">
                    Esta consulta está {consultaSeleccionada?.estado}. Solo se permite visualización histórica; no se pueden crear, editar, responder, eliminar ni cambiar estados de tareas.
                  </p>
                )}
              </div>

              <Button type="button" variant="outline" onClick={volverAConsultas}>
                Cambiar consulta
              </Button>
            </div>
          </div>

          {consultaPermiteOperaciones(consultaSeleccionada) && (puedeCrear || tareaEditando) && (
            <form onSubmit={guardarTarea} className="rounded-xl border bg-card p-5 space-y-4">
              <div>
                <h3 className="font-semibold">
                  {tareaEditando ? "Editar tarea" : "Crear tarea"}
                </h3>
                <p className="text-sm text-muted-foreground">
                  {tareaEditando
                    ? "Actualiza la información de la tarea seleccionada."
                    : "Registra una tarea para la consulta seleccionada."}
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="flex flex-col gap-1.5">
                  <label className="text-sm font-medium">Categoría</label>
                  <select
                    name="categoriaId"
                    value={formTarea.categoriaId}
                    onChange={handleTareaChange}
                    className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    <option value="">Seleccione una categoría</option>
                    {categorias.map((categoria) => (
                      <option key={categoria.id} value={categoria.id}>
                        {categoria.nombre}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-sm font-medium">Fecha de entrega</label>
                  <input
                    type="date"
                    name="fechaEntrega"
                    value={formTarea.fechaEntrega}
                    onChange={handleTareaChange}
                    className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-sm font-medium">Días de notificación</label>
                  <input
                    type="number"
                    min="0"
                    name="diasNotificacion"
                    value={formTarea.diasNotificacion}
                    onChange={handleTareaChange}
                    placeholder="Ej. 3"
                    className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  />
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium">Descripción</label>
                <textarea
                  name="descripcion"
                  value={formTarea.descripcion}
                  onChange={handleTareaChange}
                  rows={4}
                  placeholder="Describe la tarea, compromiso o actividad a realizar..."
                  className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
              </div>

              <FormFileUpload
                name="archivos"
                label="Documentos del seguimiento"
                multiple={true}
                setValue={(name, value) => {
                  if (name === "archivos") {
                    setArchivosTarea(Array.isArray(value) ? value : value ? [value] : [])
                  }
                }}
                value={archivosTarea}
              />

              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  name="notificarPartes"
                  checked={formTarea.notificarPartes}
                  onChange={handleTareaChange}
                  className="h-4 w-4"
                />
                Notificar partes y contrapartes
              </label>

              {puedeVerAlertas && (
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    name="alertaDisciplinaria"
                    checked={formTarea.alertaDisciplinaria}
                    onChange={handleTareaChange}
                    className="h-4 w-4"
                  />
                  Marcar como alerta disciplinaria
                </label>
              )}

              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  name="notificarEstudiante"
                  checked={formTarea.notificarEstudiante}
                  onChange={handleTareaChange}
                  className="h-4 w-4"
                />
                Visible/notificar al estudiante
              </label>

              <div className="flex justify-end gap-3">
                {tareaEditando && (
                  <Button type="button" variant="outline" onClick={limpiarFormTarea}>
                    Cancelar edición
                  </Button>
                )}

                <Button type="submit" disabled={guardando}>
                  {guardando
                    ? "Guardando..."
                    : tareaEditando
                      ? "Actualizar tarea"
                      : "Crear tarea"}
                </Button>
              </div>
            </form>
          )}

          <div className="rounded-xl border bg-card p-5 space-y-4">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h3 className="font-semibold">Tareas existentes</h3>
                <p className="text-sm text-muted-foreground">
                  Revisa las tareas asociadas a esta consulta.
                </p>
              </div>

              <Button
                type="button"
                variant="outline"
                onClick={() => cargarTareasPorConsulta(consultaSeleccionada.id || consultaSeleccionada.consultaId)}
                disabled={loadingTareas}
              >
                Actualizar
              </Button>
            </div>

            {loadingTareas ? (
              <div className="rounded-lg border bg-muted/30 p-6 text-center text-sm text-muted-foreground">
                Cargando tareas...
              </div>
            ) : tareasOrdenadas.length === 0 ? (
              <div className="rounded-lg border bg-muted/30 p-6 text-center text-sm text-muted-foreground">
                No hay tareas registradas para esta consulta.
              </div>
            ) : (
              <div className="space-y-3">
                {tareasPaginadas.map((tarea) => {
                  const seguimientoId = obtenerIdTarea(tarea)
                  const respuestas = respuestasPorTarea[seguimientoId] || []
                  const ultima = ultimaRespuesta(respuestas)
                  const accionRespuesta = getAccionRespuesta(ultima, puedeResponder)
                  const mostrarBotonRespuesta = consultaPermiteOperaciones(consultaSeleccionada) && seguimientoPermiteOperaciones(tarea) && ["RESPONDER", "EDITAR", "RESPONDER_NUEVAMENTE"].includes(accionRespuesta)
                  const puedeOperarConsulta = consultaPermiteOperaciones(consultaSeleccionada)
                  const puedeOperarSeguimiento = seguimientoPermiteOperaciones(tarea)
                  const puedeEditarRegistro = puedeOperarConsulta && puedeOperarSeguimiento && accionPermitidaPorRegistro(tarea, "puedeEditar", puedeEditar)
                  const puedeEliminarRegistro = puedeOperarConsulta && puedeOperarSeguimiento && accionPermitidaPorRegistro(tarea, "puedeEliminar", puedeEliminar)
                  const puedeCambiarEstadoRegistro = puedeOperarConsulta && accionPermitidaPorRegistro(tarea, "puedeEditar", puedeEditar)

                  return (
                    <div key={seguimientoId} className="rounded-lg border bg-background p-4 space-y-3">
                      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                        <div className="space-y-2">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="rounded-full border bg-muted px-2.5 py-1 text-xs font-medium">
                              {obtenerCategoriaTarea(tarea)}
                            </span>

                            <span className={`rounded-full border px-2.5 py-1 text-xs font-medium ${estadoSeguimientoBadgeClass(tarea.estado || "PENDIENTE")}`}>
                              {textoEstadoSeguimiento(tarea.estado || "PENDIENTE")}
                            </span>

                            {seguimientoEstaVencido(tarea) && (
                              <span className="rounded-full border border-yellow-500/30 bg-yellow-500/10 px-2.5 py-1 text-xs font-medium text-yellow-700">
                                Vencida
                              </span>
                            )}

                            {tarea.alertaDisciplinaria && puedeVerAlertas && (
                              <span className="rounded-full border border-destructive/30 bg-destructive/10 px-2.5 py-1 text-xs font-medium text-destructive">
                                Alerta disciplinaria
                              </span>
                            )}

                            {ultima && (
                              <span className={`rounded-full border px-2.5 py-1 text-xs font-medium ${estadoBadgeClass(ultima.estado)}`}>
                                Respuesta {ultima.estado || "PENDIENTE"}
                              </span>
                            )}

                            {ultima?.fueraPlazo && (
                              <span className="rounded-full border border-yellow-500/30 bg-yellow-500/10 px-2.5 py-1 text-xs font-medium text-yellow-700">
                                Fuera de plazo
                              </span>
                            )}
                          </div>

                          <p className="text-sm whitespace-pre-line">
                            {obtenerTextoTarea(tarea)}
                          </p>
                        </div>

                        <div className="flex flex-wrap gap-2">
                          {puedeEditarRegistro && (
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              onClick={() => editarTarea(tarea)}
                            >
                              Editar
                            </Button>
                          )}

                          {puedeEliminarRegistro && (
                            <Button
                              type="button"
                              variant="destructive"
                              size="sm"
                              onClick={() => pedirConfirmacionEliminar(tarea)}
                            >
                              Eliminar
                            </Button>
                          )}

                          {puedeCambiarEstadoRegistro && (
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              onClick={() => abrirCambioEstadoSeguimiento(tarea)}
                            >
                              Cambiar estado
                            </Button>
                          )}

                          {mostrarBotonRespuesta && (
                            <Button
                              type="button"
                              size="sm"
                              onClick={() => abrirRespuesta(tarea)}
                            >
                              {textoAccionRespuesta(accionRespuesta)}
                            </Button>
                          )}
                        </div>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-5 gap-3 text-xs text-muted-foreground">
                        <div>
                          <span className="font-medium text-foreground">Autor: </span>
                          {obtenerAutorTarea(tarea)}
                        </div>

                        <div>
                          <span className="font-medium text-foreground">Registro: </span>
                          {obtenerFechaTarea(tarea) || "Sin fecha"}
                        </div>

                        <div>
                          <span className="font-medium text-foreground">Entrega programada: </span>
                          {tarea.fechaEntrega || "Sin fecha"}
                        </div>

                        <div>
                          <span className="font-medium text-foreground">Recordatorio: </span>
                          {tarea.diasNotificacion ?? "Sin días"}
                        </div>

                        <div>
                          <span className="font-medium text-foreground">Notifica partes: </span>
                          {tarea.notificarPartes ? "Sí" : "No"}
                        </div>
                      </div>

                      {renderArchivosTarea(tarea)}

                      {ultima && (
                        <div className="rounded-lg border bg-muted/30 p-3 space-y-3">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="text-sm font-medium">Última respuesta</p>
                            <span className={`rounded-full border px-2 py-0.5 text-xs font-medium ${estadoBadgeClass(ultima.estado)}`}>
                              {ultima.estado}
                            </span>
                            {ultima.fueraPlazo && (
                              <span className="rounded-full border border-yellow-500/30 bg-yellow-500/10 px-2 py-0.5 text-xs font-medium text-yellow-700">
                                Fuera de plazo
                              </span>
                            )}
                          </div>

                          <p className="text-sm whitespace-pre-line">{ultima.contenido}</p>

                          {ultima.observacionRevision && (
                            <div className="rounded-md border bg-background p-3 text-sm">
                              <span className="font-medium">Observación de revisión: </span>
                              {ultima.observacionRevision}
                            </div>
                          )}

                          {renderArchivosRespuesta({ ...ultima, seguimientoId })}
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            )}

            {tareasOrdenadas.length > 0 && (
              <Pagination
                currentPage={paginaTareas}
                totalPages={totalPaginasTareas}
                onPageChange={setPaginaTareas}
                pageSize={registrosPorPaginaTareas}
                onPageSizeChange={(value) => {
                  setRegistrosPorPaginaTareas(value)
                  setPaginaTareas(1)
                }}
                pageSizeOptions={DEFAULT_PAGE_SIZE_OPTIONS}
                totalItems={tareasOrdenadas.length}
              />
            )}
          </div>

          {puedeResponder && tareaRespuesta && (
            <form
              onSubmit={handleSubmit(guardarRespuesta)}
              className="rounded-xl border bg-card p-5 space-y-4"
            >
              <div>
                <h3 className="font-semibold">
                  {respuestaEditando ? "Editar respuesta" : "Responder tarea"}
                </h3>
                <p className="text-sm text-muted-foreground">
                  Tarea #{obtenerIdTarea(tareaRespuesta)}: {obtenerTextoTarea(tareaRespuesta)}
                </p>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium">Respuesta</label>
                <textarea
                  {...register("contenido")}
                  rows={4}
                  placeholder="Escribe tu respuesta..."
                  className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
              </div>

              <FormFileUpload
                name="archivos"
                label={respuestaEditando ? "Agregar documentos de soporte" : "Documentos de soporte"}
                multiple={true}
                setValue={setValue}
                value={archivosRespuesta}
                errors={errors}
              />

              <div className="flex justify-end gap-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={cerrarRespuesta}
                  disabled={subiendoRespuesta}
                >
                  Cancelar
                </Button>

                <Button type="submit" disabled={subiendoRespuesta}>
                  {subiendoRespuesta
                    ? "Guardando..."
                    : respuestaEditando
                      ? "Actualizar respuesta"
                      : "Enviar respuesta"}
                </Button>
              </div>
            </form>
          )}
        </div>
      )}

      {respuestaDecision && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <form
            onSubmit={guardarDecision}
            className="w-full max-w-md rounded-xl border bg-background p-6 shadow-xl space-y-4"
          >
            <div>
              <h3 className="text-lg font-semibold">
                {formDecision.estado === "APROBADA" ? "Aprobar respuesta" : "Rechazar respuesta"}
              </h3>
              <p className="mt-1 text-sm text-muted-foreground">
                Escribe la observación que quedará asociada a la decisión.
              </p>
            </div>

            <div className="rounded-lg border bg-muted/30 p-3 text-sm whitespace-pre-line">
              {respuestaDecision.contenido}
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium">Observación</label>
              <textarea
                value={formDecision.observacionRevision}
                onChange={(event) =>
                  setFormDecision((prev) => ({
                    ...prev,
                    observacionRevision: event.target.value,
                  }))
                }
                rows={4}
                className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>

            <div className="flex justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => setRespuestaDecision(null)}
                disabled={guardandoDecision}
              >
                Cancelar
              </Button>

              <Button
                type="submit"
                variant={formDecision.estado === "RECHAZADA" ? "destructive" : "default"}
                disabled={guardandoDecision}
              >
                {guardandoDecision ? "Guardando..." : "Confirmar"}
              </Button>
            </div>
          </form>
        </div>
      )}


      {seguimientoEstado && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <form
            onSubmit={guardarCambioEstadoSeguimiento}
            className="w-full max-w-md rounded-xl border bg-background p-6 shadow-xl space-y-4"
          >
            <div>
              <h3 className="text-lg font-semibold">Cambiar estado de tarea</h3>
              <p className="mt-1 text-sm text-muted-foreground">
                Usa este flujo para completar, cancelar o volver a pendiente una tarea según las reglas del backend.
              </p>
            </div>

            <div className="rounded-lg border bg-muted/30 p-3 text-sm">
              <p className="font-medium">{obtenerCategoriaTarea(seguimientoEstado)}</p>
              <p className="mt-1 line-clamp-3 text-muted-foreground">
                {obtenerTextoTarea(seguimientoEstado)}
              </p>
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium">Estado</label>
              <select
                value={formEstadoSeguimiento.estado}
                onChange={(event) =>
                  setFormEstadoSeguimiento((prev) => ({
                    ...prev,
                    estado: event.target.value,
                  }))
                }
                className="w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {ESTADOS_SEGUIMIENTO.map((estado) => (
                  <option key={estado.value} value={estado.value}>
                    {estado.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={cerrarCambioEstadoSeguimiento}
                disabled={guardandoEstadoSeguimiento}
              >
                Cancelar
              </Button>

              <Button type="submit" disabled={guardandoEstadoSeguimiento}>
                {guardandoEstadoSeguimiento ? "Guardando..." : "Guardar estado"}
              </Button>
            </div>
          </form>
        </div>
      )}

      {tareaAEliminar && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="w-full max-w-md rounded-xl border bg-background p-6 shadow-xl space-y-4">
            <div>
              <h3 className="text-lg font-semibold">Eliminar tarea</h3>
              <p className="mt-1 text-sm text-muted-foreground">
                La tarea será desactivada lógicamente y se cancelarán sus notificaciones pendientes.
              </p>
            </div>

            <div className="rounded-lg border bg-muted/30 p-3 text-sm">
              <p className="font-medium">{obtenerCategoriaTarea(tareaAEliminar)}</p>
              <p className="mt-1 line-clamp-3 text-muted-foreground">
                {obtenerTextoTarea(tareaAEliminar)}
              </p>
            </div>

            <div className="flex justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => setTareaAEliminar(null)}
                disabled={eliminando}
              >
                Cancelar
              </Button>

              <Button
                type="button"
                variant="destructive"
                onClick={confirmarEliminarTarea}
                disabled={eliminando}
              >
                {eliminando ? "Eliminando..." : "Sí, eliminar"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
