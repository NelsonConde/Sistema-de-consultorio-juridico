"use client"

import * as React from "react"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarSeparator,
} from "@/components/ui/sidebar"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Power } from "lucide-react"
import { useRouter, usePathname } from "next/navigation"
import { API_URL_BASE } from "@/lib/config"

/**
 * Componente de barra lateral principal de la aplicación.
 * Muestra menú de navegación, información del usuario y controles de sesión.
 * 
 * @component
 * @param {Object} props - Las propiedades del componente
 * @param {Array} props.mainItems - Array de items del menú principal. Cada item debe tener {title, path}
 * @param {Array} props.footerItems - Array de items del menú del pie de página
 * @returns {JSX.Element} Barra lateral con navegación y controles de usuario
 * 
 * @example
 * <AppSidebar 
 *   mainItems={[{title: "Inicio", path: "/inicio"}]}
 *   footerItems={[{title: "Configuración", path: "/config"}]}
 * />
 */
export function AppSidebar({ mainItems = [], footerItems = [] }) {
  const [email, setEmail] = React.useState("")
  const [name, setName] = React.useState("")
  const router = useRouter()
  const pathname = usePathname()

  /**
   * Efecto para cargar información del usuario autenticado desde el backend.
   * Se ejecuta una sola vez al montar el componente.
   */
  React.useEffect(() => {
    /**
     * Obtiene los datos del usuario desde el endpoint /auth/me
     * Guarda el email y nombre en el estado del componente
     * @async
     */
    const cargarUsuario = async () => {
      try {
        const res = await fetch(`${API_URL_BASE}/auth/me`, {
          method: "GET",
          credentials: "include",
        })

        if (!res.ok) return

        const user = await res.json()

        setEmail(user.username || "")
        setName(user.nombre || user.username?.split("@")[0] || "")

      } catch (error) {
        console.error("Error cargando usuario", error)
      }
    }

    cargarUsuario()
  }, [])

  /**
   * Convierte un texto a una ruta normalizada en minúsculas sin espacios.
   * Ejemplo: "Mi Página" → "/mipágina"
   * 
   * @param {string} text - Texto a normalizar
   * @returns {string} Ruta normalizada con / inicial
   */
  function normalizePath(text) {
    return `/${String(text).toLowerCase().replace(/\s+/g, "")}`
  }

  /**
   * Obtiene la ruta de un item del menú.
   * Si el item tiene propiedad 'path', la utiliza; si no, genera una desde el título.
   * 
   * @param {Object} item - Objeto con propiedades {title, path}
   * @param {string} item.title - Título del item
   * @param {string} [item.path] - Ruta personalizada (opcional)
   * @returns {string} Ruta del item
   */
  function getItemPath(item) {
    if (item.path) {
      return item.path.startsWith("/") ? item.path : `/${item.path}`
    }

    return normalizePath(item.title)
  }

  /**
   * Navega hacia el item del menú seleccionado.
   * Valida que la ruta sea válida antes de navegar.
   * 
   * @param {Object} item - Objeto del menú a navegar
   * @param {string} item.path - Ruta del item
   */
  function handleSubmit(item) {
    const path = getItemPath(item)

    if (!path || path === "#") return

    router.push(path)
  }

  /**
   * Cierra la sesión del usuario.
   * Realiza logout en el backend y redirige a la página de login.
   * 
   * @async
   * @returns {Promise<void>}
   */
  const handleLogout = async () => {
    try {
      await fetch(`${API_URL_BASE}/auth/logout`, {
        method: "POST",
        credentials: "include",
      })
    } catch (error) {
      console.error("Error cerrando sesión", error)
    }

    router.replace("/") // tu login está en /
  }

  return (
    <Sidebar className="relative overflow-hidden border-r h-screen sticky top-0">

      <div className="[background-image:var(--sidebar-gradient)] h-full flex flex-col">

        {/* HEADER */}
        <SidebarHeader className="p-4 flex items-center gap-3 text-sidebar-foreground">
          <div className="flex size-10 items-center justify-center rounded-lg bg-transparent">
            <img src="/logo.png" className="size-10 object-contain width={20} height={20}" />
          </div>

          <div className="flex flex-col leading-tight">
            <span className="font-semibold text-sm">
              Sistema Jurídico
            </span>
            <span className="text-xs opacity-70">
              v1.0.0
            </span>
          </div>
        </SidebarHeader>

        <SidebarSeparator />

        {/* MENU */}
        <SidebarContent className="px-2 py-4 flex-1">
          <SidebarMenu>
            {mainItems.map((item, index) => {
              const path = getItemPath(item)
              const isActive = pathname === path

              return (
                <SidebarMenuItem key={index}>
                  <SidebarMenuButton
                    onClick={() => handleSubmit(item)}
                    className={`
                      rounded-lg transition-all
                      ${isActive
                        ? "!bg-white/15 !text-white font-semibold shadow-sm"
                        : "!text-sidebar-foreground hover:!bg-white/10 hover:!text-white"
                      }
                    `}
                  >
                    <span className={isActive ? "!text-white" : "!text-sidebar-foreground"}>
                      {item.title}
                    </span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )
            })}
          </SidebarMenu>
        </SidebarContent>

        <SidebarSeparator />

        {/* FOOTER */}
        <SidebarFooter className="p-4">
          <SidebarMenu>

            {footerItems.map((item, index) => (
              <SidebarMenuItem key={index}>
                <SidebarMenuButton
                  onClick={() => handleSubmit(item)}
                  className="rounded-lg text-sidebar-foreground hover:bg-sidebar-accent/70"
                >
                  <span>{item.title}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            ))}

            {/* USUARIO */}
            <SidebarMenuItem className="mt-6">
              <div className="flex items-center justify-between gap-3 p-2 rounded-lg hover:bg-sidebar-accent/70">

                <div className="flex items-center gap-3 min-w-0">
                  <Avatar className="size-9 shrink-0">
                    <AvatarImage src="https://github.com/shadcn.png" />
                    <AvatarFallback>
                      {name?.charAt(0).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex flex-col text-sm leading-tight text-sidebar-foreground min-w-0">
                    <span className="font-medium truncate">
                      {name}
                    </span>
                    <span className="text-xs opacity-70 truncate">
                      {email}
                    </span>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleLogout}
                  title="Cerrar sesión"
                  aria-label="Cerrar sesión"
                  className="flex size-9 shrink-0 items-center justify-center rounded-full text-sidebar-foreground transition hover:bg-red-500/20 hover:text-red-200"
                >
                  <Power className="size-5" />
                </button>
              </div>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
      </div>
    </Sidebar>
  )
}