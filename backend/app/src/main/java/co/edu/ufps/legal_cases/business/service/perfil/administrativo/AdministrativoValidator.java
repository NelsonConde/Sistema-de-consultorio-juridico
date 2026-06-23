package co.edu.ufps.legal_cases.business.service.perfil.administrativo;

import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.equalsIgnoreCase;
import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.mismoId;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class AdministrativoValidator {

    private final AdministrativoRepository administrativoRepository;

    public AdministrativoValidator(AdministrativoRepository administrativoRepository) {
        this.administrativoRepository = administrativoRepository;
    }

    public void validarIdNoEnviadoEnCreacion(Long id) {
        if (id != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarIdNoCambiado(Long idRuta, Long idDto) {
        if (idDto != null && !Objects.equals(idDto, idRuta)) {
            throw new BusinessException("No se permite cambiar el id del administrativo");
        }
    }

    public void validarCamposObligatorios(DatosAdministrativo datos) {
        if (datos.nombre() == null) {
            throw new BusinessException("El nombre es obligatorio");
        }

        if (datos.documento() == null || datos.documento().isBlank()) {
            throw new BusinessException("El documento es obligatorio");
        }

        if (datos.email() == null || datos.email().isBlank()) {
            throw new BusinessException("El email es obligatorio");
        }

        if (datos.telefono() == null || datos.telefono().isBlank()) {
            throw new BusinessException("El teléfono es obligatorio");
        }

        if (datos.usuario() == null || datos.usuario().isBlank()) {
            throw new BusinessException("El usuario es obligatorio");
        }

        if (datos.codigo() == null || datos.codigo().isBlank()) {
            throw new BusinessException("El código es obligatorio");
        }

        if (datos.tipoDocumento() == null) {
            throw new BusinessException("El tipo de documento es obligatorio");
        }

        if (datos.sede() == null) {
            throw new BusinessException("La sede es obligatoria");
        }
    }

    public void validarDuplicadosCreacion(
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo) {

        if (administrativoRepository.existsByDocumento(documento)) {
            throw new BusinessException("Ya existe un administrativo con ese documento");
        }

        if (administrativoRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Ya existe un administrativo con ese email");
        }

        if (administrativoRepository.existsByTelefono(telefono)) {
            throw new BusinessException("Ya existe un administrativo con ese teléfono");
        }

        if (administrativoRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new BusinessException("Ya existe un administrativo con ese usuario");
        }

        if (administrativoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("Ya existe un administrativo con ese código");
        }
    }

    public void validarDuplicadosActualizacion(
            Long id,
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo) {

        if (administrativoRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new BusinessException("Ya existe un administrativo con ese documento");
        }

        if (administrativoRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("Ya existe un administrativo con ese email");
        }

        if (administrativoRepository.existsByTelefonoAndIdNot(telefono, id)) {
            throw new BusinessException("Ya existe un administrativo con ese teléfono");
        }

        if (administrativoRepository.existsByUsuarioIgnoreCaseAndIdNot(usuario, id)) {
            throw new BusinessException("Ya existe un administrativo con ese usuario");
        }

        if (administrativoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe un administrativo con ese código");
        }
    }

    public void validarExistenCambios(Administrativo administrativo, DatosAdministrativo datos) {
        if (sinCambios(administrativo, datos)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Administrativo administrativo, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(administrativo.getActivo(), activo)) {
            throw new BusinessException("El administrativo ya tiene ese estado");
        }
    }

    public void validarCambioDirectora(Administrativo administrativo, Boolean directora) {
        if (directora == null) {
            throw new BusinessException("El estado de directora es obligatorio");
        }

        if (Objects.equals(administrativo.getDirectora(), directora)) {
            throw new BusinessException("El administrativo ya tiene ese estado de directora");
        }
    }

    private boolean sinCambios(Administrativo administrativo, DatosAdministrativo datos) {
        return equalsIgnoreCase(administrativo.getNombre(), datos.nombre())
                && mismoId(administrativo.getTipoDocumento(), datos.tipoDocumento(), TipoDocumento::getId)
                && equalsIgnoreCase(administrativo.getDocumento(), datos.documento())
                && equalsIgnoreCase(administrativo.getEmail(), datos.email())
                && Objects.equals(administrativo.getTelefono(), datos.telefono())
                && equalsIgnoreCase(administrativo.getUsuario(), datos.usuario())
                && equalsIgnoreCase(administrativo.getCodigo(), datos.codigo())
                && mismoId(administrativo.getSede(), datos.sede(), Sede::getId)
                && Objects.equals(administrativo.getActivo(), datos.activo())
                && Objects.equals(administrativo.getDirectora(), datos.directora());
    }
}