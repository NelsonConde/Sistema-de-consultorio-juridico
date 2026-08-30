import { FormFileUpload } from "./FormFileUpload";

/**
 * File handling.
 * @param {Object} props - Parameter description.
 * @param {Array<File>} props.archivos - Selected files.
 * @param {function} props.onChange - Parameter description.
 * @returns {JSX.Element} Result value.
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
        setValue={(_name, value) => onChange(value)}
        value={archivos}
        errors={{}}
      />
    </div>
  );
}
