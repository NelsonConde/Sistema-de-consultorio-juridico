"use client"

import * as React from "react"
import { usePathname } from "next/navigation"
import { useTheme } from "next-themes"
import { Moon, Sun } from "lucide-react"

import {
  SidebarProvider,
  SidebarInset,
  SidebarTrigger,
} from "@/components/ui/sidebar"
import { TooltipProvider } from "@/components/ui/tooltip"
import { PermissionSidebar } from "@/components/navigation/PermissionSidebar"
import { Toaster } from "@/components/ui/sonner"
import { Button } from "@/components/ui/button"
import { CalendarModal } from "@/components/CalendarModal"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

const ROUTE_INFO = {
  "/inicio": { breadcrumb: "Home", title: "Panel de control del consultorio jurídico" },
  "/admin": { breadcrumb: "Configuración / Catálogos", title: "Gestión de Parámetros" },
  "/asesoresymonitores": { breadcrumb: "Panel / Usuarios", title: "Asesores y Monitores" },
  "/conciliadores": { breadcrumb: "Conciliadores / Gestión", title: "Nuevo Conciliador" },
  "/consultasjuridicas": { breadcrumb: "Panel / Consultas", title: "Consultas Jurídicas" },
  "/estudiantes": { breadcrumb: "Panel / Usuarios", title: "Estudiantes" },
  "/nuevaconsulta": { breadcrumb: "Panel / Consultas / Nueva", title: "Nueva Consulta Jurídica" },
  "/personas": { breadcrumb: "Panel / Personas", title: "Gestión de personas" },
  "/recepcion": { breadcrumb: "Panel / Personas", title: "Registro de Persona" },
  "/roles": { breadcrumb: "Sistema / Usuarios", title: "Crear Usuario del Sistema" },
  "/tareas": { breadcrumb: "Control Tareas", title: "Organización de tareas y seguimiento de proyectos" },
}

export default function DashboardLayout({ children }) {
  const { setTheme } = useTheme()
  const pathname = usePathname()

  // Default fallback si no hay mapeo
  const info = ROUTE_INFO[pathname] || { breadcrumb: "Sistema", title: "Casos Jurídicos" }

  return (
    <TooltipProvider>
      <SidebarProvider className="min-h-screen">
        <PermissionSidebar />

        <SidebarInset className="bg-muted/30 min-h-screen flex flex-col">
          {/* HEADER PERSISTENTE */}
          <header className="sticky top-0 z-50 flex h-16 shrink-0 items-center justify-between border-b bg-background/80 px-6 backdrop-blur">
            <div className="flex items-center gap-4">
              <SidebarTrigger />

              <div className="flex flex-col leading-tight">
                <span className="text-sm text-muted-foreground">
                  {info.breadcrumb}
                </span>
                <span className="font-semibold">
                  {info.title}
                </span>
              </div>
            </div>

            {/* BOTONES GLOBALES */}
            <div className="flex items-center gap-2">
              <CalendarModal />
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="icon">
                    <Sun className="h-5 w-5 transition-all dark:scale-0 dark:-rotate-90" />
                    <Moon className="absolute h-5 w-5 scale-0 rotate-90 transition-all dark:scale-100 dark:rotate-0" />
                  </Button>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                  <DropdownMenuItem onClick={() => setTheme("light")}>
                    Claro
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => setTheme("dark")}>
                    Oscuro
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => setTheme("system")}>
                    Sistema
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </header>

          {/* CONTENIDO DE LA PÁGINA */}
          <main className="flex-1 overflow-auto">
            {children}
          </main>
        </SidebarInset>

        <Toaster richColors position="bottom-right" />
      </SidebarProvider>
    </TooltipProvider>
  )
}
