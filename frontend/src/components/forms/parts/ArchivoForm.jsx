import { Loader2 } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useFileResource } from "@/hooks/useFileResource";
import { FormFileUpload } from "./FormFileUpload";

/** File handling.*/
export default function ArchivoForm({ resource }) {
  const [isUploading, setIsUploading] = useState(false);
  const { upload } = useFileResource(resource, { load: false });
  const {
    setValue,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm({ defaultValues: { archivos: [] } });

  const archivos = watch("archivos");

  async function onSubmit(data) {
    if (!resource?.type || !resource?.id) {
      toast.error("El archivo debe estar asociado a un recurso");
      return;
    }
    if (!data.archivos?.length) {
      toast.error("Por favor selecciona al menos un archivo");
      return;
    }

    setIsUploading(true);
    try {
      const results = await upload(data.archivos);
      const failed = results.filter((result) => !result.ok);

      if (failed.length === 0) {
        toast.success("Archivos subidos correctamente");
        reset();
      } else if (failed.length < results.length) {
        toast.warning(`${failed.length} archivo(s) no pudieron subirse`);
      } else {
        toast.error("No se pudieron subir los archivos");
      }
    } catch (error) {
      console.error(error);
      toast.error(error.message || "No se pudieron subir los archivos");
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <Card className="mx-auto w-full max-w-md">
      <CardHeader>
        <CardTitle>Subir archivos</CardTitle>
        <CardDescription>
          Los documentos se asociarán al recurso seleccionado.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <FormFileUpload
            name="archivos"
            label="Documentos a subir"
            multiple
            setValue={setValue}
            value={archivos}
            errors={errors}
          />
          <Button type="submit" className="w-full" disabled={isUploading}>
            {isUploading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Enviando...
              </>
            ) : (
              "Enviar archivos"
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
