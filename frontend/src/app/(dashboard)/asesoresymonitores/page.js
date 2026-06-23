"use client";

import { Users } from "lucide-react";
import { AsesoresYMonitoresForm } from "@/components/forms/usuarios/AsesoresYMonitoresForm";

export default function AsesoresYMonitoresPage() {
  return (
    <div className="p-6 lg:p-10">
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-primary/10">
            <Users className="w-6 h-6 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              Asesores y Monitores
            </h1>
            <p className="text-muted-foreground">
              Consulta y búsqueda de usuarios activos
            </p>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-background shadow-sm p-6 lg:p-8">
          <AsesoresYMonitoresForm />
        </div>
      </div>
    </div>
  );
}