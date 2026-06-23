package co.edu.ufps.legal_cases.business.service.perfil.monitor;

import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.equalsIgnoreCase;
import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.mismoId;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class MonitorValidator {

    private final MonitorRepository monitorRepository;

    public MonitorValidator(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    public void validarIdNoEnviadoEnCreacion(Long id) {
        if (id != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarIdNoCambiado(Long idRuta, Long idDto) {
        if (idDto != null && !Objects.equals(idDto, idRuta)) {
            throw new BusinessException("No se permite cambiar el id del monitor");
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

        if (monitorRepository.existsByDocumento(documento)) {
            throw new BusinessException("Ya existe un monitor con ese documento");
        }

        if (monitorRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Ya existe un monitor con ese email");
        }

        if (monitorRepository.existsByTelefono(telefono)) {
            throw new BusinessException("Ya existe un monitor con ese teléfono");
        }

        if (monitorRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new BusinessException("Ya existe un monitor con ese usuario");
        }

        if (monitorRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("Ya existe un monitor con ese código");
        }
    }

    public void validarDuplicadosActualizacion(
            Long id,
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo) {

        if (monitorRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new BusinessException("Ya existe un monitor con ese documento");
        }

        if (monitorRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("Ya existe un monitor con ese email");
        }

        if (monitorRepository.existsByTelefonoAndIdNot(telefono, id)) {
            throw new BusinessException("Ya existe un monitor con ese teléfono");
        }

        if (monitorRepository.existsByUsuarioIgnoreCaseAndIdNot(usuario, id)) {
            throw new BusinessException("Ya existe un monitor con ese usuario");
        }

        if (monitorRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe un monitor con ese código");
        }
    }

    public void validarExistenCambios(Monitor monitor, DatosMonitor datos) {
        if (sinCambios(monitor, datos)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Monitor monitor, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(monitor.getActivo(), activo)) {
            throw new BusinessException("El monitor ya tiene ese estado");
        }
    }

    private boolean sinCambios(Monitor monitor, DatosMonitor datos) {
        return equalsIgnoreCase(monitor.getNombre(), datos.nombre())
                && mismoId(monitor.getTipoDocumento(), datos.tipoDocumento(), TipoDocumento::getId)
                && equalsIgnoreCase(monitor.getDocumento(), datos.documento())
                && equalsIgnoreCase(monitor.getEmail(), datos.email())
                && Objects.equals(monitor.getTelefono(), datos.telefono())
                && equalsIgnoreCase(monitor.getUsuario(), datos.usuario())
                && equalsIgnoreCase(monitor.getCodigo(), datos.codigo())
                && mismoId(monitor.getSede(), datos.sede(), Sede::getId)
                && Objects.equals(monitor.getActivo(), datos.activo());
    }
}