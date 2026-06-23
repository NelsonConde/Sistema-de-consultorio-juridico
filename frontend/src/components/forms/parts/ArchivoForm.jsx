import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { FormFileUpload } from './FormFileUpload';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

/**
 * Formulario para subir archivos desde el frontend.
 * @returns {JSX.Element} Componente de carga de archivos.
 */
export default function ArchivoForm() {
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { API_URL_BASE } from '@/lib/config';

export default function ArchivoForm() {
  const [isUploading, setIsUploading] = useState(false);
  const [directories, setDirectories] = useState([]);
  const [selectedPath, setSelectedPath] = useState("");

  useEffect(() => {
    const fetchDirs = async () => {
      try {
        const response = await fetch(`${API_URL_BASE}/files/directories`, {
          credentials: "include",
        });
        if (response.ok) {
          const data = await response.json();
          setDirectories(data);
        }
      } catch (err) {
        console.error("Error fetching directories:", err);
      }
    };
    fetchDirs();
  }, []);
  
  const { setValue, handleSubmit, watch, reset, formState: { errors } } = useForm({
    defaultValues: {
      archivos: []
    }
  });

  const archivosValue = watch("archivos");

  const onSubmit = async (data) => {
    if (!data.archivos || (Array.isArray(data.archivos) && data.archivos.length === 0)) {
      toast.error('Por favor selecciona al menos un archivo');
      return;
    }

    setIsUploading(true);
    
    const formData = new FormData();
    // Determinamos si es un arreglo (múltiple) o un solo archivo
    const isMultiple = Array.isArray(data.archivos);
    let endpoint = `${FILE_STORAGE_API_URL_BASE}/files/upload`;
    
    if (isMultiple) {
      data.archivos.forEach(file => {
        formData.append('files', file);
      });
      endpoint = `${FILE_STORAGE_API_URL_BASE}/files/upload-multiple`;
    } else {
      formData.append('file', data.archivos);
    }

    if (selectedPath) {
      formData.append('path', selectedPath);
    }

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        credentials: "include",
        body: formData,
      });

      if (!response.ok) {
        throw new Error('Error al subir los archivos');
      }

      const responseData = await response.json();
      console.log("Archivos subidos exitosamente:", responseData);
      toast.success(isMultiple ? 'Archivos subidos correctamente' : 'Archivo subido correctamente');
      
      reset();
    } catch (error) {
      console.error('Upload error:', error);
      toast.error('Ocurrió un error al enviar los archivos al backend');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <Card className="w-full max-w-md mx-auto">
      <CardHeader>
        <CardTitle>Subir Archivos</CardTitle>
        <CardDescription>
          Selecciona tus documentos y envíalos al servidor de almacenamiento.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <FormFileUpload
            name="archivos"
            label="Documentos a subir"
            multiple={true} // Permite seleccionar varios archivos a la vez
            setValue={setValue}
            value={archivosValue}
            errors={errors}
          />

          <Accordion type="single" collapsible className="w-full border rounded-lg px-4">
            <AccordionItem value="ruta" className="border-none">
              <AccordionTrigger className="hover:no-underline">
                Ruta de Destino: <span className="font-normal text-muted-foreground ml-2">{selectedPath ? `/${selectedPath}` : 'Raíz'}</span>
              </AccordionTrigger>
              <AccordionContent>
                <div className="flex flex-col gap-2 mt-2 max-h-48 overflow-y-auto">
                  <label className="flex items-center space-x-2 p-2 rounded hover:bg-muted cursor-pointer transition-colors">
                    <input 
                      type="radio" 
                      name="path" 
                      value="" 
                      checked={selectedPath === ""}
                      onChange={() => setSelectedPath("")}
                      className="w-4 h-4 text-primary focus:ring-primary"
                    />
                    <span>/ (Directorio Raíz)</span>
                  </label>
                  {directories.map(dir => (
                    <label key={dir} className="flex items-center space-x-2 p-2 rounded hover:bg-muted cursor-pointer transition-colors">
                      <input 
                        type="radio" 
                        name="path" 
                        value={dir} 
                        checked={selectedPath === dir}
                        onChange={() => setSelectedPath(dir)}
                        className="w-4 h-4 text-primary focus:ring-primary"
                      />
                      <span>/{dir}</span>
                    </label>
                  ))}
                </div>
              </AccordionContent>
            </AccordionItem>
          </Accordion>

          <Button type="submit" className="w-full" disabled={isUploading}>
            {isUploading ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Enviando...
              </>
            ) : (
              'Enviar Archivos'
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
