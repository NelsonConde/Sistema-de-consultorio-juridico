"use client";

import { LockKeyhole, Mail, UserRound } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";

import { CambiarPasswordForm } from "@/components/forms/perfil/CambiarPasswordForm";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { apiResponse } from "@/lib/api";
import { API_URL_BASE } from "@/lib/config";
import { ActualizarContactoForm } from "./ActualizarContactoForm";

const READONLY_DETAILS = [
  { key: "username", label: "Usuario" },
  { key: "sede", label: "Sede" },
  { key: "codigo", label: "Código" },
];

function readableProfile(value) {
  if (!value) return "Sin perfil";

  return String(value)
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
}

export function MiPerfilView() {
  const router = useRouter();
  const [perfil, setPerfil] = useState(null);
  const [status, setStatus] = useState("loading"); // loading | ready | error
  const [activeTab, setActiveTab] = useState("contacto");

  const cargarPerfil = useCallback(async () => {
    setStatus("loading");

    try {
      const { response, data } = await apiResponse(
        `${API_URL_BASE}/mi-perfil`,
        {
          method: "GET",
        },
      );

      if (response.status === 401) {
        toast.error("Sesión expirada", {
          description: "Debe iniciar sesión nuevamente",
        });
        router.replace("/");
        return;
      }

      if (!response.ok) {
        setStatus("error");
        return;
      }

      setPerfil(data);
      setStatus("ready");
    } catch {
      setStatus("error");
    }
  }, [router]);

  useEffect(() => {
    cargarPerfil();
  }, [cargarPerfil]);

  const handleContactUpdated = useCallback((updatedProfile) => {
    if (!updatedProfile || typeof updatedProfile !== "object") return;

    setPerfil((currentProfile) => ({
      ...currentProfile,
      ...updatedProfile,
    }));
  }, []);

  if (status === "loading") {
    return (
      <div
        className="mx-auto grid w-full max-w-5xl gap-6 lg:grid-cols-[minmax(17rem,0.85fr)_minmax(0,1.5fr)]"
        data-testid="profile-loading"
      >
        <span className="sr-only">Cargando perfil</span>
        <Skeleton className="h-72 w-full rounded-xl" />
        <Skeleton className="h-[28rem] w-full rounded-xl" />
      </div>
    );
  }

  if (status === "error" || !perfil) {
    return (
      <div
        className="mx-auto w-full max-w-5xl rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-sm"
        role="alert"
      >
        <p className="font-medium text-destructive">
          No fue posible cargar tu perfil
        </p>
        <p className="mt-1 text-muted-foreground">
          Verifica tu conexión o que tu perfil siga activo, e intenta de nuevo.
        </p>
        <Button
          type="button"
          onClick={cargarPerfil}
          variant="outline"
          className="mt-4"
        >
          Reintentar
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto grid w-full max-w-5xl min-w-0 gap-6 lg:grid-cols-[minmax(17rem,0.85fr)_minmax(0,1.5fr)] lg:items-start">
      <Card className="min-w-0" data-testid="profile-summary">
        <CardHeader className="items-center border-b pb-4 text-center">
          <div className="mb-2 flex size-16 items-center justify-center rounded-full bg-primary/10 text-primary">
            <UserRound className="size-8" aria-hidden="true" />
          </div>
          <CardTitle className="max-w-full break-words text-xl">
            {perfil.nombre || "Nombre no disponible"}
          </CardTitle>
          <div className="flex max-w-full flex-wrap justify-center gap-2 pt-2 text-xs">
            <span className="max-w-full break-words rounded-full bg-muted px-3 py-1 font-medium text-muted-foreground">
              {readableProfile(perfil.tipoPerfil)}
            </span>
          </div>
        </CardHeader>

        <CardContent>
          <dl className="divide-y">
            {READONLY_DETAILS.map(({ key, label }) => (
              <div
                key={key}
                className="grid min-w-0 gap-1 py-3 first:pt-0 last:pb-0 sm:grid-cols-[5.5rem_minmax(0,1fr)] sm:gap-3"
              >
                <dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  {label}
                </dt>
                <dd className="min-w-0 break-words text-sm font-medium sm:text-right">
                  {perfil[key] || "—"}
                </dd>
              </div>
            ))}
          </dl>
        </CardContent>
      </Card>

      <Card className="min-w-0" data-testid="profile-panel">
        <CardContent>
          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="grid h-10 w-full grid-cols-2">
              <TabsTrigger value="contacto" className="min-w-0 px-2">
                <Mail aria-hidden="true" />
                <span>Contacto</span>
              </TabsTrigger>
              <TabsTrigger value="seguridad" className="min-w-0 px-2">
                <LockKeyhole aria-hidden="true" />
                <span>Seguridad</span>
              </TabsTrigger>
            </TabsList>

            <TabsContent
              value="contacto"
              forceMount
              className="mt-5 data-[state=inactive]:hidden"
            >
              <ActualizarContactoForm
                perfil={perfil}
                onUpdated={handleContactUpdated}
              />
            </TabsContent>

            <TabsContent
              value="seguridad"
              forceMount
              className="mt-5 data-[state=inactive]:hidden"
            >
              <CambiarPasswordForm isActive={activeTab === "seguridad"} />
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}
