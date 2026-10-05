"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Loader2 } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { apiClient } from "@/lib/apiClient";
import { API_URL_BASE } from "@/lib/config";
import {
  leerJsonSeguro,
  mensajeErrorDesdeRespuesta
} from "./consultas-juridicas.utils";
import { getResponseCorrelationId, withErrorReference } from "@/lib/api";

export function BotonDescargarFicha({ consultaId }) {
  const router = useRouter()
  const [cargando, setCargando] = useState(false)

  async function descargar() {
    if (cargando) return
    setCargando(true)

    try {
      const res = await apiClient.request(
        `${API_URL_BASE}/consultas/${consultaId}/ficha-pdf`,
        { method: "GET", credentials: "include" }
      )

      const tipo = res.headers.get("Content-Type") ?? ""

      if (res.ok && tipo.includes("application/pdf")) {
        const blob = await res.blob()
        const disposition = res.headers.get("Content-Disposition") ?? ""
        const nombre =
          /filename="?([^";]+)"?/i.exec(disposition)?.[1] ??
          `ficha-expediente-${consultaId}.pdf`

        const url = URL.createObjectURL(blob)
        const a = document.createElement("a")
        a.href = url
        a.download = nombre
        document.body.appendChild(a)
        a.click()
        a.remove()
        URL.revokeObjectURL(url)

        toast.success("Ficha descargada correctamente")
        return
      }

      // Si llegamos aquí, no es un PDF: es un error en JSON
      const payload = await leerJsonSeguro(res)
      const correlationId = getResponseCorrelationId(res, payload)

      if (res.status === 401) {
        router.push("/")
        return
      }

      toast.error(
        res.status === 403 ? "No se puede descargar la ficha" : "Error al descargar la ficha",
        {
          description: withErrorReference(
            mensajeErrorDesdeRespuesta(payload, "No se pudo descargar la ficha."),
            correlationId
          ),
        }
      )
    } catch {
      toast.error("Error de conexión", {
        description: "No se pudo descargar la ficha.",
      })
    } finally {
      setCargando(false)
    }
  }

  return (
    <Button size="sm" variant="outline" onClick={descargar} disabled={cargando}>
      {cargando && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
      {cargando ? "Generando..." : "Ficha PDF"}
    </Button>
  )
}