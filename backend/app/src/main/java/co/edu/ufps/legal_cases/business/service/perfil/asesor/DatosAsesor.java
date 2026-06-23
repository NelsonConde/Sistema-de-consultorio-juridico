package co.edu.ufps.legal_cases.business.service.perfil.asesor;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;

// Agrupa los datos ya normalizados y las relaciones cargadas para crear o actualizar un asesor.
public record DatosAsesor(
        String nombre,
        String documento,
        String email,
        String telefono,
        String usuario,
        String codigo,
        TipoDocumento tipoDocumento,
        Sede sede,
        Area area,
        Boolean activo) {
}