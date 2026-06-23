"use client"

import React, { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import listPlugin from '@fullcalendar/list'
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { toast } from "sonner"
import { API_URL_BASE } from '@/lib/config'

/**
 * Componente Calendar
 *
 * Muestra los seguimientos del usuario autenticado según su rol:
 * - Administrador: ve todos los seguimientos.
 * - Asesor/Monitor: ve los seguimientos de consultas dentro de su alcance.
 * - Estudiante: ve solo los seguimientos marcados como notificarEstudiante = true.
 * - Conciliador: por ahora no ve ninguno hasta que el módulo esté implementado.
 *
 * Cada seguimiento aparece en el día de su fechaEntrega.
 */
export default function Calendar({ onEventClick }) {
  const router = useRouter()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchEvents = async () => {
      try {
        setLoading(true)

        const response = await fetch(`${API_URL_BASE}/seguimientos/calendario`, {
          credentials: 'include'
        })

        if (!response.ok) throw new Error('No se pudo obtener los seguimientos')

        const seguimientos = await response.json()

        // Solo se muestran seguimientos que tienen fechaEntrega definida
        const mappedEvents = seguimientos
          .filter(seg => seg.fechaEntrega)
          .map(seg => ({
            id: seg.id.toString(),
            title: seg.descripcion || seg.categoriaSeguimientoNombre || 'Seguimiento',
            start: seg.fechaEntrega,
            allDay: true,
            classNames: [
              seg.alertaDisciplinaria
                ? 'bg-destructive text-destructive-foreground border-destructive'
                : seg.categoriaSeguimientoNombre?.toLowerCase().includes('audiencia')
                  ? 'bg-primary text-primary-foreground border-primary'
                  : 'bg-secondary text-secondary-foreground border-secondary'
            ],
            extendedProps: { ...seg }
          }))

        setEvents(mappedEvents)
      } catch (error) {
        console.error('Error cargando eventos:', error)
        toast.error("Error al cargar eventos", {
          description: "No se pudieron obtener los seguimientos del calendario."
        })
      } finally {
        setLoading(false)
      }
    }

    fetchEvents()
  }, [])

  return (
    <Card className="w-full border-border bg-card shadow-sm">
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <CardTitle className="text-xl font-semibold text-foreground">
          Calendario de Actividades
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="full-calendar-wrapper relative">
          {loading && (
            <div className="absolute inset-0 z-10 flex items-center justify-center bg-background/50 backdrop-blur-[1px]">
              <div className="flex flex-col items-center gap-2">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                <p className="text-sm font-medium text-muted-foreground">Cargando seguimientos...</p>
              </div>
            </div>
          )}
          <FullCalendar
            plugins={[dayGridPlugin, listPlugin]}
            initialView="dayGridMonth"
            events={events}
            locale="es"
            headerToolbar={{
              left: 'prev,next today',
              center: 'title',
              right: 'dayGridMonth,listMonth'
            }}
            buttonText={{
              today: 'Hoy',
              month: 'Mes',
              list: 'Lista'
            }}
            height="auto"
            aspectRatio={1.6}
            editable={false}
            selectable={false}
            dayMaxEvents={true}
            eventDisplay="block"
            eventClick={(info) => {
              // Extraer el ID de la consulta o proceso para redirigir
              const consultaId = info.event.extendedProps.consultaId;
              
              if (onEventClick) {
                onEventClick();
              }

              if (consultaId) {
                // Redirigimos a la página de tareas/seguimientos
                // Enviamos el parámetro de búsqueda para que se autocompleta el campo
                router.push(`/tareas?search=${consultaId}`); 
              } else {
                router.push('/tareas');
              }
            }}
          />
        </div>
      </CardContent>

      <style jsx global>{`
        .full-calendar-wrapper .fc {
          --fc-border-color: var(--border);
          --fc-daygrid-event-dot-width: 8px;
          --fc-today-bg-color: var(--accent);
          background-color: transparent;
          color: var(--foreground);
        }

        .full-calendar-wrapper .fc-toolbar {
          display: grid !important;
          grid-template-columns: 1fr auto 1fr !important;
          gap: 1rem;
          @apply items-center mb-6 !important;
        }

        .full-calendar-wrapper .fc-toolbar-chunk:nth-child(1) {
          @apply flex justify-start;
        }

        .full-calendar-wrapper .fc-toolbar-chunk:nth-child(2) {
          @apply flex justify-center min-w-[200px];
        }

        .full-calendar-wrapper .fc-toolbar-chunk:nth-child(3) {
          @apply flex justify-end;
        }

        .full-calendar-wrapper .fc-toolbar-title {
          @apply text-xl font-bold text-foreground m-0 text-center !important;
          white-space: nowrap;
        }

        .full-calendar-wrapper .fc-button {
          @apply bg-secondary text-secondary-foreground border-border hover:bg-secondary/80 font-medium px-4 py-2 h-9 transition-colors !important;
          background-image: none !important;
          box-shadow: none !important;
          text-transform: capitalize !important;
        }

        .full-calendar-wrapper .fc-button-primary:not(:disabled).fc-button-active,
        .full-calendar-wrapper .fc-button-primary:not(:disabled):active {
          @apply bg-primary text-primary-foreground !important;
        }

        .full-calendar-wrapper .fc-col-header-cell {
          @apply py-3 bg-muted/50 font-semibold text-muted-foreground border-border;
        }

        .full-calendar-wrapper .fc-col-header-cell-cushion {
          @apply text-sm;
        }

        .full-calendar-wrapper .fc-daygrid-day {
          @apply border-border transition-colors;
        }

        .full-calendar-wrapper .fc-daygrid-day:hover {
          @apply bg-muted/30;
        }

        .full-calendar-wrapper .fc-day-today {
          @apply bg-accent/20 !important;
        }

        .full-calendar-wrapper .fc-daygrid-day-number {
          @apply p-2 text-sm text-muted-foreground font-medium;
        }

        .full-calendar-wrapper .fc-event {
          @apply rounded-md border px-2 py-0.5 text-xs font-medium shadow-sm transition-all cursor-pointer hover:opacity-80 !important;
        }

        .full-calendar-wrapper .fc-daygrid-event {
          @apply my-0.5 mx-1 !important;
        }

        .dark .full-calendar-wrapper .fc-theme-standard td,
        .dark .full-calendar-wrapper .fc-theme-standard th {
          border-color: var(--border);
        }

        .dark .full-calendar-wrapper .fc-daygrid-day-number,
        .dark .full-calendar-wrapper .fc-col-header-cell-cushion {
          color: var(--muted-foreground);
        }

        .dark .full-calendar-wrapper .fc-toolbar-title {
          color: var(--foreground);
        }
      `}</style>
    </Card>
  )
}