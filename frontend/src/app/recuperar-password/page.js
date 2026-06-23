"use client"

import { RecuperarPasswordForm } from "@/components/auth/RecuperarPasswordForm"
import { Moon, Sun, Scale } from "lucide-react"
import { useTheme } from "next-themes"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

export default function RecuperarPasswordPage() {
  const { setTheme } = useTheme()

  return (
    <main className="relative min-h-screen flex">

      {/* Toggle tema */}
      <div className="absolute top-4 right-4 z-50">
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

      {/* Lado izquierdo */}
      <div className="hidden lg:flex w-1/2 flex-col justify-between p-12 bg-gradient-to-br from-primary to-indigo-700 text-white">

        <div className="flex items-center gap-3">
          <Scale className="h-8 w-8" />
          <span className="text-xl font-semibold">
            Consultorio Jurídico
          </span>
        </div>

        <div className="max-w-md space-y-4">
          <h2 className="text-4xl font-bold leading-tight">
            Recupera tu acceso
          </h2>

          <p className="text-white/80">
            Te enviaremos un enlace a tu correo para restablecer tu contraseña.
          </p>
        </div>

        <p className="text-sm text-white/70">
          © 2026 Todos los derechos reservados
        </p>
      </div>

      {/* Lado derecho */}
      <div className="flex w-full lg:w-1/2 items-center justify-center bg-background px-6 py-12">
        <div className="absolute inset-0 -z-10 bg-gradient-to-tr from-muted/40 via-transparent to-muted/40" />
        <RecuperarPasswordForm />
      </div>

    </main>
  )
}