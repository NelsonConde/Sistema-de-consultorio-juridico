"use client"

import { FilePlus } from "lucide-react"
import { NuevaConsultaForm } from "@/components/forms/consulta/NuevaConsultaForm"

export default function NuevaConsultaPage() {
  return (
    <div className="p-6 lg:p-10">
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-primary/10">
            <FilePlus className="w-6 h-6 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              Nueva Consulta
            </h1>
            <p className="text-muted-foreground">
              Diligencie la información para registrar una nueva consulta jurídica
            </p>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-background shadow-sm p-6 lg:p-8">
          <NuevaConsultaForm />
        </div>
      </div>
    </div>
  )
}
