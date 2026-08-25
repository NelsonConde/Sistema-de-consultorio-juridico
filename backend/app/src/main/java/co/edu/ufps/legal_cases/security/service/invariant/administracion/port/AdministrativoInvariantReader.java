package co.edu.ufps.legal_cases.security.service.invariant.administracion.port;

import java.util.List;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

/**
 * Define el contrato mínimo para obtener el estado administrativo
 * necesario para evaluar las invariantes del sistema.
 */
public interface AdministrativoInvariantReader {

    List<AdministrativoLectura> cargarEstadoAdministrativo();

    // Solo se exponen los datos necesarios para evaluar la administración,
    // evitando propagar entidades JPA entre módulos.
    record AdministrativoLectura(
            Long administrativoId,
            Long usuarioSistemaId,
            Boolean usuarioActivo,
            TipoPerfilUsuario tipoPerfilActual,
            Boolean administrativoActivo,
            Boolean directora,
            Long rolId) {
    }
}