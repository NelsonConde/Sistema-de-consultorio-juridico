"use client"

import * as React from "react"
import { Suspense } from "react"
import { ClipboardList } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { SeguimientosForm } from "@/components/forms/consulta/SeguimientosForm"

export default function SeguimientosPage() {
  return (
    <div className="p-6 lg:p-10">
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-primary/10">
            <ClipboardList className="w-6 h-6 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              Tareas
            </h1>
          </div>
        </div>
        <Card>
          <CardContent className="p-6 lg:p-8">
            <Suspense fallback={<div>Cargando tareas...</div>}>
              <SeguimientosForm />
            </Suspense>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}