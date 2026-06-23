"use client";

import { AlertTriangle, X } from "lucide-react";
import { Button } from "@/components/ui/button";

export function ConfirmActionDialog({
  open,
  title,
  description,
  confirmText = "Aceptar",
  cancelText = "Cancelar",
  loading = false,
  variant = "destructive",
  onConfirm,
  onClose,
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/70 backdrop-blur-sm px-4">
      <div className="w-full max-w-md rounded-2xl border bg-card shadow-2xl">
        <div className="flex items-start justify-between gap-4 border-b p-5">
          <div className="flex items-center gap-3">
            <div className="flex size-11 items-center justify-center rounded-full bg-destructive/10 text-destructive">
              <AlertTriangle className="size-5" />
            </div>

            <div>
              <h2 className="text-lg font-semibold">{title}</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {description}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="rounded-md p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground disabled:opacity-50"
          >
            <X className="size-5" />
          </button>
        </div>

        <div className="flex justify-end gap-3 p-5">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={loading}
          >
            {cancelText}
          </Button>

          <Button
            type="button"
            variant={variant}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? "Procesando..." : confirmText}
          </Button>
        </div>
      </div>
    </div>
  );
}