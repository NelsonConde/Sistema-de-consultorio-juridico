"use client"

import * as React from "react"
import { PersonaForm } from "@/components/forms/persona/PersonaForm"
export default function Home() {
  return (
    <div className="p-6 lg:p-10">
      <div className="max-w-6xl mx-auto">
        <div className="mb-6 space-y-1">
          <h1 className="text-3xl font-bold tracking-tight">
            Registro de Persona
          </h1>
          <p className="text-muted-foreground">
            Complete la información para registrar un nuevo usuario en el sistema.
          </p>
        </div>
        <div className="rounded-2xl border border-border bg-background shadow-sm p-6 lg:p-8">
          <PersonaForm />
        </div>
      </div>
    </div>
  )
}