package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;

// Agrupa los datos ya normalizados y las relaciones cargadas para crear o actualizar un estudiante.
// Así el command no tiene que mover muchos parámetros sueltos entre validación y mapeo.
public record DatosEstudiante(
        String nombre,
        String documento,
        String email,
        String telefono,
        String usuario,
        String codigo,
        TipoDocumento tipoDocumento,
        Sede sede,
        Asesor asesor,
        Boolean activo,
        Boolean conciliacion) {
}