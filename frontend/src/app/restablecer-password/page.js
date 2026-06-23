"use client"

import { useSearchParams } from "next/navigation"
import { Suspense } from "react"
import { RestablecerPasswordForm } from "@/components/auth/RestablecerPasswordForm"
import { useTheme } from "next-themes";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Button } from "@/components/ui/button"
import { Moon, Sun } from "lucide-react"
import { Scale } from "lucide-react"

function RestablecerPasswordContent() {
  const searchParams = useSearchParams()
  const token = searchParams.get("token")
  const { setTheme } = useTheme()

  if (!token) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-red-500">Enlace inválido o incompleto</p>
      </div>
    )
  }

  return (
    <main className="relative min-h-screen flex">

      {/* Toggle */}
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

      {/* LADO IZQUIERDO */}
      <div className="hidden lg:flex w-1/2 flex-col justify-between p-12 bg-gradient-to-b from-blue-600 via-indigo-600 to-indigo-800 min-h-screen">

        <div className="flex items-center gap-3">
          <Scale className="h-8 w-8" />
          <span className="text-xl font-semibold">
            Consultorio Jurídico
          </span>
        </div>

        <div className="max-w-md space-y-4">
          <h2 className="text-4xl font-bold leading-tight">
            Gestión legal moderna y eficiente
          </h2>

          <p className="text-white/80">
            Administra consultas, usuarios y procesos jurídicos
            en una plataforma centralizada.
          </p>
        </div>

        <p className="text-sm text-white/70">
          © 2026 Todos los derechos reservados
        </p>
      </div>

      {/* LADO DERECHO */}
      <div className="flex w-full lg:w-1/2 items-center justify-center bg-background px-6 py-12">

        <div className="absolute inset-0 -z-10 bg-gradient-to-tr from-muted/40 via-transparent to-muted/40" />

        <RestablecerPasswordForm token={token} />
      </div>

    </main>
  )
}

export default function RestablecerPasswordPage() {
  return (
    <Suspense fallback={<div className="flex items-center justify-center min-h-screen">Cargando...</div>}>
      <RestablecerPasswordContent />
    </Suspense>
  )
}