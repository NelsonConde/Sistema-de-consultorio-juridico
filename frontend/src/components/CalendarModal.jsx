"use client"

import * as React from "react"
import { Calendar as CalendarIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogTrigger,
  DialogTitle,
} from "@/components/ui/dialog"
import Calendar from "@/components/Calendar"

/**
 * Muestra un botón que abre un modal con el calendario.
 * @returns {JSX.Element} Modal de calendario.
 */
export function CalendarModal() {
  const [open, setOpen] = React.useState(false);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="icon" title="Abrir Calendario">
          <CalendarIcon className="h-5 w-5" />
        </Button>
      </DialogTrigger>
      {/* 
        Ajuste de tamaño 85% del viewport dejando aprox 15% libre.
      */}
      <DialogContent className="max-w-[95vw] sm:max-w-[95vw] w-[95vw] h-[95vh] flex flex-col p-6">
        <DialogTitle className="sr-only">Calendario de Eventos</DialogTitle>
        <div className="flex-1 overflow-y-auto rounded-md border mt-2">
          <Calendar onEventClick={() => setOpen(false)} />
        </div>
      </DialogContent>
    </Dialog>
  )
}
