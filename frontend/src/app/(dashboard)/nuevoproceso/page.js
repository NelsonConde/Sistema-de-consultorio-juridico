"use client"

import { NuevoProcesoForm } from "@/components/forms/procesos/NuevoProcesosForm"

export default function NuevoProcesoPage() {
  return (
    <div className="p-6 lg:p-10">
      <div className="max-w-5xl mx-auto">
        <div className="mb-6">
          <h1 className="text-3xl font-bold">
            Nuevo Proceso
          </h1>
          <p className="text-muted-foreground">
            Registra un proceso asociado a una consulta jurídica.
          </p>
        </div>

        <div className="rounded-2xl border bg-background shadow-sm p-6 lg:p-8 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-600 via-indigo-600 to-indigo-800" />
          <NuevoProcesoForm />
        </div>
      </div>
    </div>
  )
}
