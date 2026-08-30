"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { apiResponse } from "@/lib/api";
import { tienePermiso } from "@/lib/authz";
import { CatalogCrudForm } from "./CatalogCrudForm";
import { CATALOG_DEFINITIONS, CATALOG_GROUPS } from "./catalog-definitions";

export function CatalogosSection() {
  const router = useRouter();
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(true);
  const [selectedId, setSelectedId] = useState("");

  useEffect(() => {
    let mounted = true;

    async function loadUser() {
      try {
        const { response, data } = await apiResponse("/auth/me");

        if (response.status === 401) {
          router.replace("/");
          return;
        }

        if (!response.ok) {
          router.replace("/");
          return;
        }

        if (mounted) setUser(data);
      } catch (error) {
        console.error("Error cargando permisos para catálogos:", error);
        toast.error("No fue posible verificar los permisos");
      } finally {
        if (mounted) setChecking(false);
      }
    }

    loadUser();
    return () => {
      mounted = false;
    };
  }, [router]);

  const availableCatalogs = useMemo(
    () =>
      CATALOG_DEFINITIONS.filter((catalog) =>
        tienePermiso(user, catalog.requiredPermission)
      ),
    [user]
  );

  useEffect(() => {
    if (availableCatalogs.length === 0) {
      setSelectedId("");
      return;
    }

    if (!availableCatalogs.some((catalog) => catalog.id === selectedId)) {
      setSelectedId(availableCatalogs[0].id);
    }
  }, [availableCatalogs, selectedId]);

  const selectedCatalog = useMemo(
    () => availableCatalogs.find((catalog) => catalog.id === selectedId) ?? null,
    [availableCatalogs, selectedId]
  );

  if (checking) {
    return <div className="text-center py-10 text-muted-foreground">Cargando...</div>;
  }

  if (availableCatalogs.length === 0) {
    return (
      <div className="rounded-xl border bg-card p-6">
        <h2 className="text-xl font-semibold">Catálogos</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          No tienes permisos para gestionar catálogos del sistema.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-xl border bg-card p-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h2 className="text-2xl font-bold">Catálogos del Sistema</h2>
            <p className="text-muted-foreground">
              Selecciona el catálogo que deseas administrar.
            </p>
          </div>

          <div className="w-full md:w-80">
            <label className="mb-1.5 block text-sm font-medium">
              Catálogo
            </label>
            <Select value={selectedId} onValueChange={setSelectedId}>
              <SelectTrigger className="h-9 w-full">
                <SelectValue placeholder="Seleccione un catálogo" />
              </SelectTrigger>
              <SelectContent position="popper" align="end">
                {CATALOG_GROUPS.map((group) => {
                  const groupCatalogs = availableCatalogs.filter(
                    (catalog) => catalog.group === group
                  );

                  if (groupCatalogs.length === 0) return null;

                  return (
                    <SelectGroup key={group}>
                      <SelectLabel>{group}</SelectLabel>
                      {groupCatalogs.map((catalog) => (
                        <SelectItem key={catalog.id} value={catalog.id}>
                          {catalog.label}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  );
                })}
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      {selectedCatalog && (
        <CatalogCrudForm key={selectedCatalog.id} config={selectedCatalog} />
      )}
    </div>
  );
}
