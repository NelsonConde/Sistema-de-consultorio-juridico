package co.edu.ufps.legal_cases.business.service.perfil.monitor;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;

// Agrupa los datos ya normalizados y las relaciones cargadas para crear o actualizar un monitor.
public record DatosMonitor(
        String nombre,
        String documento,
        String email,
        String telefono,
        String usuario,
        String codigo,
        TipoDocumento tipoDocumento,
        Sede sede,
        Boolean activo) {
}