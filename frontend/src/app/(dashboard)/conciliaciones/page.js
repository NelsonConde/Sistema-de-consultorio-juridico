"use client";

import { CalendarClock, Scale } from "lucide-react";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import { ConciliacionesForm } from "@/components/forms/conciliacion/ConciliacionesForm";
import { ReunionesConciliacionForm } from "@/components/forms/conciliacion/ReunionesConciliacionForm";

export default function ConciliacionesPage() {
  return (
    <div className="p-6 lg:p-10">
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-primary/10 p-3">
            <Scale className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Conciliaciones</h1>
            <p className="text-muted-foreground">
              Consulta, crea y gestiona conciliaciones y sus reuniones según permisos y alcance del usuario.
            </p>
          </div>
        </div>

        <div className="rounded-2xl border bg-background p-6 shadow-sm relative overflow-hidden">
          <div className="absolute top-0 left-0 h-1 w-full bg-gradient-to-r from-blue-600 via-indigo-600 to-indigo-800" />

          <Tabs defaultValue="conciliaciones" className="space-y-6">
            <TabsList className="flex w-full items-end justify-start gap-1 rounded-none border-b bg-transparent p-0 overflow-x-auto">
              <TabsTrigger
                value="conciliaciones"
                className="rounded-t-xl rounded-b-none border border-b-0 bg-muted/60 px-6 py-2 data-[state=active]:bg-background data-[state=active]:shadow-none whitespace-nowrap"
              >
                <Scale className="mr-2 h-4 w-4" />
                Conciliaciones
              </TabsTrigger>
              <TabsTrigger
                value="reuniones"
                className="rounded-t-xl rounded-b-none border border-b-0 bg-muted/60 px-6 py-2 data-[state=active]:bg-background data-[state=active]:shadow-none whitespace-nowrap"
              >
                <CalendarClock className="mr-2 h-4 w-4" />
                Reuniones
              </TabsTrigger>
            </TabsList>

            <TabsContent value="conciliaciones" className="pt-2">
              <ConciliacionesForm />
            </TabsContent>

            <TabsContent value="reuniones" className="pt-2">
              <ReunionesConciliacionForm />
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
