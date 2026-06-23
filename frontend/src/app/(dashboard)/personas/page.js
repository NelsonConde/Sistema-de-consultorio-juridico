"use client";

import { PersonasForm } from "@/components/forms/persona/PersonasForm";
import { Users } from "lucide-react";

export default function PersonasPage() {
  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center gap-3">
        <div className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <Users className="size-6" />
        </div>
        <div>
          <h2 className="text-3xl font-bold">Personas</h2>
          <p className="text-muted-foreground">
            Consulta, edita y administra las personas registradas desde recepción.
          </p>
        </div>
      </div>
      <PersonasForm />
    </div>
  );
}