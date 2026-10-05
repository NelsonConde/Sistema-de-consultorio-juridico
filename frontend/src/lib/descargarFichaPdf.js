const MENSAJE_GENERICO = "No se pudo descargar la ficha. Intente nuevamente."

export async function descargarFichaPdf(consultaId) {
  try {
    const res = await fetch(`/api/consultas/${consultaId}/ficha-pdf`, {
      method: "GET",
      credentials: "include", // AJUSTAR según cómo hagan las otras llamadas
    })

    const tipo = res.headers.get("Content-Type") ?? ""

    if (!res.ok || !tipo.includes("application/pdf")) {
      const cuerpo = await res.json().catch(() => null)
      return { ok: false, mensaje: cuerpo?.mensaje ?? MENSAJE_GENERICO }
    }

    const blob = await res.blob()
    const disposition = res.headers.get("Content-Disposition") ?? ""
    const nombre =
      /filename="?([^";]+)"?/i.exec(disposition)?.[1] ?? `ficha-expediente-${consultaId}.pdf`

    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = nombre
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)

    return { ok: true }
  } catch {
    return { ok: false, mensaje: MENSAJE_GENERICO }
  }
}