"use client"

import { UsuarioSistemaForm } from "@/components/forms/AdminUsuarios/UsuarioSistemaForm"

export default function UsuarioSistemaPage() {
  return (
    <div className="p-6 lg:p-10">
      <div className="max-w-5xl mx-auto">
        <div className="mb-6 space-y-1">
          <h1 className="text-3xl font-bold tracking-tight">
            Crear Usuario
          </h1>
          <p className="text-muted-foreground">
            Registra un nuevo usuario del sistema y asigna sus credenciales de acceso.
          </p>
        </div>
        <div className="rounded-2xl border border-border bg-background shadow-sm p-6 lg:p-8 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-600 via-indigo-600 to-indigo-800" />
          <UsuarioSistemaForm />
        </div>
      </div>
    </div>
  )
}