"use client";

import { EliminacionForm } from "@/components/forms/EliminacionForm";
import { RotateCcw } from "lucide-react";

export default function EliminacionPage() {
  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center gap-3">
        <div className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <RotateCcw className="size-6" />
        </div>
        <div>
          <h2 className="text-3xl font-bold">Eliminación</h2>
        </div>
      </div>
      <EliminacionForm />
    </div>
  );
}
