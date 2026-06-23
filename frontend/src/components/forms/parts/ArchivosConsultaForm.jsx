import React from 'react';
import { FormFileUpload } from './FormFileUpload';

/**
 * Sección de formulario para subir archivos asociados a una consulta.
 * @param {Object} props - Propiedades del componente.
 * @param {Array<File>} props.archivos - Lista de archivos seleccionados.
 * @param {function} props.onChange - Callback para actualizar la lista de archivos.
 * @returns {JSX.Element} Componente de archivos de consulta.
 */
export default function ArchivosConsultaForm({ archivos, onChange }) {
  return (
    <div className="space-y-4">
      <h3 className="text-lg font-medium">Documentos Adicionales</h3>
      <p className="text-sm text-muted-foreground">
        Selecciona los documentos relacionados. Estos se subirán por separado.
      </p>
      <FormFileUpload
        name="archivos"
        label="Documentos a subir"
        multiple={true}
        setValue={(name, value) => onChange(value)}
        value={archivos}
        errors={{}}
      />
    </div>
  );
}