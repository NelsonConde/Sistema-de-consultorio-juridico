package co.edu.ufps.legal_cases.business.service.perfil.conciliador;

import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.equalsIgnoreCase;
import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.mismoId;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class ConciliadorValidator {

    private final ConciliadorRepository conciliadorRepository;

    public ConciliadorValidator(ConciliadorRepository conciliadorRepository) {
        this.conciliadorRepository = conciliadorRepository;
    }

    public void validarIdNoEnviadoEnCreacion(Long id) {
        if (id != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarIdNoCambiado(Long idRuta, Long idDto) {
        if (idDto != null && !Objects.equals(idDto, idRuta)) {
            throw new BusinessException("No se permite cambiar el id del conciliador");
        }
    }

    public void validarCamposObligatorios(DatosConciliador datos) {
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

        if (datos.tipoConciliador() == null) {
            throw new BusinessException("El tipo de conciliador es obligatorio");
        }
    }

    public void validarDuplicadosCreacion(
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo) {

        if (conciliadorRepository.existsByDocumento(documento)) {
            throw new BusinessException("Ya existe un conciliador con ese documento");
        }

        if (conciliadorRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Ya existe un conciliador con ese email");
        }

        if (conciliadorRepository.existsByTelefono(telefono)) {
            throw new BusinessException("Ya existe un conciliador con ese teléfono");
        }

        if (conciliadorRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new BusinessException("Ya existe un conciliador con ese usuario");
        }

        if (conciliadorRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("Ya existe un conciliador con ese código");
        }
    }

    public void validarDuplicadosActualizacion(
            Long id,
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo) {

        if (conciliadorRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new BusinessException("Ya existe un conciliador con ese documento");
        }

        if (conciliadorRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("Ya existe un conciliador con ese email");
        }

        if (conciliadorRepository.existsByTelefonoAndIdNot(telefono, id)) {
            throw new BusinessException("Ya existe un conciliador con ese teléfono");
        }

        if (conciliadorRepository.existsByUsuarioIgnoreCaseAndIdNot(usuario, id)) {
            throw new BusinessException("Ya existe un conciliador con ese usuario");
        }

        if (conciliadorRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe un conciliador con ese código");
        }
    }

    public void validarExistenCambios(Conciliador conciliador, DatosConciliador datos) {
        if (sinCambios(conciliador, datos)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Conciliador conciliador, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(conciliador.getActivo(), activo)) {
            throw new BusinessException("El conciliador ya tiene ese estado");
        }
    }

    private boolean sinCambios(Conciliador conciliador, DatosConciliador datos) {
        return equalsIgnoreCase(conciliador.getNombre(), datos.nombre())
                && mismoId(conciliador.getTipoDocumento(), datos.tipoDocumento(), TipoDocumento::getId)
                && Objects.equals(conciliador.getDocumento(), datos.documento())
                && equalsIgnoreCase(conciliador.getEmail(), datos.email())
                && Objects.equals(conciliador.getTelefono(), datos.telefono())
                && equalsIgnoreCase(conciliador.getUsuario(), datos.usuario())
                && mismoId(conciliador.getSede(), datos.sede(), Sede::getId)
                && equalsIgnoreCase(conciliador.getCodigo(), datos.codigo())
                && Objects.equals(conciliador.getTipoConciliador(), datos.tipoConciliador())
                && Objects.equals(conciliador.getActivo(), datos.activo());
    }
}