package co.edu.ufps.legal_cases.business.service.perfil.conciliador;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;

// Agrupa los datos ya normalizados y las relaciones cargadas para crear o actualizar un conciliador.
public record DatosConciliador(
        String nombre,
        String documento,
        String email,
        String telefono,
        String usuario,
        String codigo,
        TipoDocumento tipoDocumento,
        Sede sede,
        TipoConciliador tipoConciliador,
        Boolean activo) {
}