package co.edu.ufps.legal_cases.business.service.perfil.asesor;

import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.equalsIgnoreCase;
import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.mismoId;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class AsesorValidator {

    private final AsesorRepository asesorRepository;

    public AsesorValidator(AsesorRepository asesorRepository) {
        this.asesorRepository = asesorRepository;
    }

    public void validarIdNoEnviadoEnCreacion(Long id) {
        if (id != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarIdNoCambiado(Long idRuta, Long idDto) {
        if (idDto != null && !Objects.equals(idDto, idRuta)) {
            throw new BusinessException("No se permite cambiar el id del asesor");
        }
    }

    public void validarCamposObligatorios(
            String nombre,
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo) {

        if (nombre == null) {
            throw new BusinessException("El nombre es obligatorio");
        }

        if (documento == null || documento.isBlank()) {
            throw new BusinessException("El documento es obligatorio");
        }

        if (email == null || email.isBlank()) {
            throw new BusinessException("El email es obligatorio");
        }

        if (telefono == null || telefono.isBlank()) {
            throw new BusinessException("El teléfono es obligatorio");
        }

        if (usuario == null || usuario.isBlank()) {
            throw new BusinessException("El usuario es obligatorio");
        }

        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El código es obligatorio");
        }
    }

    public void validarDuplicadosCreacion(
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo) {

        if (asesorRepository.existsByDocumento(documento)) {
            throw new BusinessException("Ya existe un asesor con ese documento");
        }

        if (asesorRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Ya existe un asesor con ese email");
        }

        if (asesorRepository.existsByTelefono(telefono)) {
            throw new BusinessException("Ya existe un asesor con ese teléfono");
        }

        if (asesorRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new BusinessException("Ya existe un asesor con ese usuario");
        }

        if (asesorRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("Ya existe un asesor con ese código");
        }
    }

    public void validarDuplicadosActualizacion(
            Long id,
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo) {

        if (asesorRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new BusinessException("Ya existe un asesor con ese documento");
        }

        if (asesorRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("Ya existe un asesor con ese email");
        }

        if (asesorRepository.existsByTelefonoAndIdNot(telefono, id)) {
            throw new BusinessException("Ya existe un asesor con ese teléfono");
        }

        if (asesorRepository.existsByUsuarioIgnoreCaseAndIdNot(usuario, id)) {
            throw new BusinessException("Ya existe un asesor con ese usuario");
        }

        if (asesorRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe un asesor con ese código");
        }
    }

    public void validarExistenCambios(Asesor asesor, DatosAsesor datos) {
        if (sinCambios(asesor, datos)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Asesor asesor, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(asesor.getActivo(), activo)) {
            throw new BusinessException("El asesor ya tiene ese estado");
        }
    }

    private boolean sinCambios(Asesor asesor, DatosAsesor datos) {
        return equalsIgnoreCase(asesor.getNombre(), datos.nombre())
                && mismoId(asesor.getTipoDocumento(), datos.tipoDocumento(), TipoDocumento::getId)
                && Objects.equals(asesor.getDocumento(), datos.documento())
                && equalsIgnoreCase(asesor.getEmail(), datos.email())
                && Objects.equals(asesor.getTelefono(), datos.telefono())
                && equalsIgnoreCase(asesor.getUsuario(), datos.usuario())
                && mismoId(asesor.getSede(), datos.sede(), Sede::getId)
                && equalsIgnoreCase(asesor.getCodigo(), datos.codigo())
                && mismoId(asesor.getArea(), datos.area(), Area::getId)
                && Objects.equals(asesor.getActivo(), datos.activo());
    }
}