"use client"

import { apiClient } from "@/lib/apiClient";
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
 * Component implementation detail.
 * Role handling.
 * 
 * @component
 * @param {Object} props - Parameter description.
 * @param {Array} props.mainItems - Item to process.
 * @param {Array} props.footerItems - Item to process.
 * @returns {JSX.Element} Result value.
 * 
 * @example
 * <AppSidebar 
 *   mainItems={[{title: "Inicio", path: "/inicio"}]}
 * Implementation detail.
 * />
 */
export function AppSidebar({ mainItems = [], footerItems = [] }) {
  const [email, setEmail] = React.useState("")
  const [name, setName] = React.useState("")
  const router = useRouter()
  const pathname = usePathname()

  /**
   * Data loading behavior.
   * Component implementation detail.
   */
  React.useEffect(() => {
    /**
     * Data loading behavior.
     * State handling.
     * @async
     */
    const cargarUsuario = async () => {
      try {
        const res = await apiClient.request(`${API_URL_BASE}/auth/me`, {
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
   * Implementation detail.
   * Implementation detail.
   * 
   * @param {string} text - Text to normalize.
   * @returns {string} Result value.
   */
  function normalizePath(text) {
    return `/${String(text).toLowerCase().replace(/\s+/g, "")}`
  }

  /**
   * Data loading behavior.
   * Implementation detail.
   * 
   * @param {Object} item - Item to process.
   * @param {string} item.title - Item to process.
   * @param {string} [item.path] - Item to process.
   * @returns {string} Result value.
   */
  function getItemPath(item) {
    if (item.path) {
      return item.path.startsWith("/") ? item.path : `/${item.path}`
    }

    return normalizePath(item.title)
  }

  /**
   * Selection behavior.
   * Validation rule.
   * 
   * Implementation detail.
   * @param {string} item.path - Item to process.
   */
  function handleSubmit(item) {
    const path = getItemPath(item)

    if (!path || path === "#") return

    router.push(path)
  }

  /**
   * User flow detail.
   * Implementation detail.
   * 
   * @async
   * @returns {Promise<void>}
   */
  const handleLogout = async () => {
    try {
      const response = await apiClient.request(`${API_URL_BASE}/auth/logout`, {
        method: "POST",
      })

      if (!response.ok) {
        throw new Error("No fue posible cerrar la sesión")
      }

      router.replace("/")
    } catch (error) {
      console.error("Error cerrando sesión", error)
    }
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

            {/* USER */}
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