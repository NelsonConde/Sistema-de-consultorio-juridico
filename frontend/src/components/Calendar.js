"use client"

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import listPlugin from '@fullcalendar/list'
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { toast } from "sonner"
import { apiClient } from '@/lib/apiClient'

/**
 * Calendar component.
 *
 * Role handling.
 * Follow-up workflow detail.
 * Consultation flow detail.
 * Follow-up workflow detail.
 * Conciliation workflow detail.
 *
 * Follow-up workflow detail.
 */
export default function Calendar({ onEventClick }) {
  const router = useRouter()
  const [loading, setLoading] = useState(true)

  const fetchEvents = async (fetchInfo, successCallback, failureCallback) => {
    try {
      setLoading(true)
      const params = new URLSearchParams({
        from: fetchInfo.startStr.slice(0, 10),
        to: fetchInfo.endStr.slice(0, 10),
      })
      const response = await apiClient.get(`/agenda?${params.toString()}`)

      if (!response.ok) throw new Error(`No se pudo obtener la agenda (${response.status})`)

      const agenda = await response.json()
      successCallback(agenda.map(event => ({
        ...event,
        classNames: [event.type === 'CONCILIATION_MEETING'
          ? 'bg-primary text-primary-foreground border-primary'
          : event.overdue
            ? 'bg-destructive text-destructive-foreground border-destructive'
            : 'bg-secondary text-secondary-foreground border-secondary'],
        extendedProps: event,
      })))
    } catch (error) {
      console.error('Error cargando agenda:', error)
      toast.error("Error al cargar eventos", {
        description: "No se pudo obtener la agenda para el rango seleccionado."
      })
      failureCallback(error)
    } finally {
      setLoading(false)
    }
  }

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
            events={fetchEvents}
            timeZone="America/Bogota"
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
              // Consultation flow detail.
              const { consultaId, type, resourceId } = info.event.extendedProps;
              
              if (onEventClick) {
                onEventClick();
              }

              if (type === 'CONCILIATION_MEETING' && resourceId) {
                router.push(`/conciliaciones/${resourceId}`)
              } else if (consultaId) {
                router.push(`/tareas?search=${consultaId}`)
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
