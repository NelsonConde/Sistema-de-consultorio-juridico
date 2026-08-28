"use client";

import { AuditLogsTable } from "@/components/forms/AdminUsuarios/AuditLogsTable";
import { CambiarRolUsuarioForm } from "@/components/forms/AdminUsuarios/CambiarRolUsuarioForm";
import { RolePermissionsForm } from "@/components/forms/AdminUsuarios/RolePermissionsForm";
import { CatalogosSection } from "@/components/forms/catalogos/CatalogosSection";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

export default function AdministracionPage() {
  return (
    <div className="p-6 lg:p-10">
      <div className="max-w-6xl mx-auto">
        <div className="mb-6">
          <h1 className="text-3xl font-bold">Administración del Sistema</h1>
          <p className="text-muted-foreground">
            Administra catálogos, permisos, roles y auditoría desde un único lugar.
          </p>
        </div>

        <div className="rounded-2xl border bg-background shadow-sm p-6 lg:p-8 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-linear-to-r from-blue-600 via-indigo-600 to-indigo-800" />

          <Tabs defaultValue="catalogos" className="space-y-6">
            <TabsList className="flex w-full items-end justify-start gap-1 rounded-none bg-transparent p-0 border-b overflow-x-auto">
              <TabsTrigger
                value="catalogos"
                className="rounded-t-xl rounded-b-none border border-b-0 bg-muted/60 px-6 py-2 data-[state=active]:bg-background data-[state=active]:shadow-none whitespace-nowrap"
              >
                Catálogos
              </TabsTrigger>
              <TabsTrigger
                value="permisos-rol"
                className="rounded-t-xl rounded-b-none border border-b-0 bg-muted/60 px-6 py-2 data-[state=active]:bg-background data-[state=active]:shadow-none whitespace-nowrap"
              >
                Permisos
              </TabsTrigger>
              <TabsTrigger
                value="cambiar-rol"
                className="rounded-t-xl rounded-b-none border border-b-0 bg-muted/60 px-6 py-2 data-[state=active]:bg-background data-[state=active]:shadow-none whitespace-nowrap"
              >
                Cambiar Rol
              </TabsTrigger>
              <TabsTrigger
                value="auditoria"
                className="rounded-t-xl rounded-b-none border border-b-0 bg-muted/60 px-6 py-2 data-[state=active]:bg-background data-[state=active]:shadow-none whitespace-nowrap"
              >
                Auditoría
              </TabsTrigger>
            </TabsList>

            <TabsContent value="catalogos" className="pt-4">
              <CatalogosSection />
            </TabsContent>
            <TabsContent value="permisos-rol" className="pt-4">
              <RolePermissionsForm />
            </TabsContent>
            <TabsContent value="cambiar-rol" className="pt-4">
              <CambiarRolUsuarioForm />
            </TabsContent>
            <TabsContent value="auditoria" className="pt-4">
              <AuditLogsTable />
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
