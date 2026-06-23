package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.cambio.DatosPerfilCambioNormalizados;

// Este servicio centraliza las validaciones necesarias para hacer el cambio de perfil/rol
// Se usa en los manejadores de cambio de perfil/rol
@Service
@Transactional(readOnly = true)
public class PerfilCambioDuplicadosService {

    private final EstudianteRepository estudianteRepository;
    private final AsesorRepository asesorRepository;
    private final MonitorRepository monitorRepository;
    private final AdministrativoRepository administrativoRepository;
    private final ConciliadorRepository conciliadorRepository;

    public PerfilCambioDuplicadosService(
            EstudianteRepository estudianteRepository,
            AsesorRepository asesorRepository,
            MonitorRepository monitorRepository,
            AdministrativoRepository administrativoRepository,
            ConciliadorRepository conciliadorRepository) {
        this.estudianteRepository = estudianteRepository;
        this.asesorRepository = asesorRepository;
        this.monitorRepository = monitorRepository;
        this.administrativoRepository = administrativoRepository;
        this.conciliadorRepository = conciliadorRepository;
    }

    public void validarEstudiante(Long idActual, DatosPerfilCambioNormalizados datos) {
        if (idActual == null) {
            if (estudianteRepository.existsByDocumento(datos.getDocumento())) {
                throw new BusinessException("Ya existe un estudiante con ese documento");
            }

            if (estudianteRepository.existsByEmailIgnoreCase(datos.getEmail())) {
                throw new BusinessException("Ya existe un estudiante con ese email");
            }

            if (estudianteRepository.existsByTelefono(datos.getTelefono())) {
                throw new BusinessException("Ya existe un estudiante con ese teléfono");
            }

            if (estudianteRepository.existsByUsuarioIgnoreCase(datos.getUsuario())) {
                throw new BusinessException("Ya existe un estudiante con ese usuario");
            }

            if (estudianteRepository.existsByCodigoIgnoreCase(datos.getCodigo())) {
                throw new BusinessException("Ya existe un estudiante con ese código");
            }

            return;
        }

        if (estudianteRepository.existsByDocumentoAndIdNot(datos.getDocumento(), idActual)) {
            throw new BusinessException("Ya existe un estudiante con ese documento");
        }

        if (estudianteRepository.existsByEmailIgnoreCaseAndIdNot(datos.getEmail(), idActual)) {
            throw new BusinessException("Ya existe un estudiante con ese email");
        }

        if (estudianteRepository.existsByTelefonoAndIdNot(datos.getTelefono(), idActual)) {
            throw new BusinessException("Ya existe un estudiante con ese teléfono");
        }

        if (estudianteRepository.existsByUsuarioIgnoreCaseAndIdNot(datos.getUsuario(), idActual)) {
            throw new BusinessException("Ya existe un estudiante con ese usuario");
        }

        if (estudianteRepository.existsByCodigoIgnoreCaseAndIdNot(datos.getCodigo(), idActual)) {
            throw new BusinessException("Ya existe un estudiante con ese código");
        }
    }

    public void validarAsesor(Long idActual, DatosPerfilCambioNormalizados datos) {
        if (idActual == null) {
            if (asesorRepository.existsByDocumento(datos.getDocumento())) {
                throw new BusinessException("Ya existe un asesor con ese documento");
            }

            if (asesorRepository.existsByEmailIgnoreCase(datos.getEmail())) {
                throw new BusinessException("Ya existe un asesor con ese email");
            }

            if (asesorRepository.existsByTelefono(datos.getTelefono())) {
                throw new BusinessException("Ya existe un asesor con ese teléfono");
            }

            if (asesorRepository.existsByUsuarioIgnoreCase(datos.getUsuario())) {
                throw new BusinessException("Ya existe un asesor con ese usuario");
            }

            if (asesorRepository.existsByCodigoIgnoreCase(datos.getCodigo())) {
                throw new BusinessException("Ya existe un asesor con ese código");
            }

            return;
        }

        if (asesorRepository.existsByDocumentoAndIdNot(datos.getDocumento(), idActual)) {
            throw new BusinessException("Ya existe un asesor con ese documento");
        }

        if (asesorRepository.existsByEmailIgnoreCaseAndIdNot(datos.getEmail(), idActual)) {
            throw new BusinessException("Ya existe un asesor con ese email");
        }

        if (asesorRepository.existsByTelefonoAndIdNot(datos.getTelefono(), idActual)) {
            throw new BusinessException("Ya existe un asesor con ese teléfono");
        }

        if (asesorRepository.existsByUsuarioIgnoreCaseAndIdNot(datos.getUsuario(), idActual)) {
            throw new BusinessException("Ya existe un asesor con ese usuario");
        }

        if (asesorRepository.existsByCodigoIgnoreCaseAndIdNot(datos.getCodigo(), idActual)) {
            throw new BusinessException("Ya existe un asesor con ese código");
        }
    }

    public void validarMonitor(Long idActual, DatosPerfilCambioNormalizados datos) {
        if (idActual == null) {
            if (datos.getDocumento() != null && monitorRepository.existsByDocumento(datos.getDocumento())) {
                throw new BusinessException("Ya existe un monitor con ese documento");
            }

            if (monitorRepository.existsByEmailIgnoreCase(datos.getEmail())) {
                throw new BusinessException("Ya existe un monitor con ese email");
            }

            if (monitorRepository.existsByTelefono(datos.getTelefono())) {
                throw new BusinessException("Ya existe un monitor con ese teléfono");
            }

            if (monitorRepository.existsByUsuarioIgnoreCase(datos.getUsuario())) {
                throw new BusinessException("Ya existe un monitor con ese usuario");
            }

            if (monitorRepository.existsByCodigoIgnoreCase(datos.getCodigo())) {
                throw new BusinessException("Ya existe un monitor con ese código");
            }

            return;
        }

        if (datos.getDocumento() != null && monitorRepository.existsByDocumentoAndIdNot(datos.getDocumento(), idActual)) {
            throw new BusinessException("Ya existe un monitor con ese documento");
        }

        if (monitorRepository.existsByEmailIgnoreCaseAndIdNot(datos.getEmail(), idActual)) {
            throw new BusinessException("Ya existe un monitor con ese email");
        }

        if (monitorRepository.existsByTelefonoAndIdNot(datos.getTelefono(), idActual)) {
            throw new BusinessException("Ya existe un monitor con ese teléfono");
        }

        if (monitorRepository.existsByUsuarioIgnoreCaseAndIdNot(datos.getUsuario(), idActual)) {
            throw new BusinessException("Ya existe un monitor con ese usuario");
        }

        if (monitorRepository.existsByCodigoIgnoreCaseAndIdNot(datos.getCodigo(), idActual)) {
            throw new BusinessException("Ya existe un monitor con ese código");
        }
    }

    public void validarAdministrativo(Long idActual, DatosPerfilCambioNormalizados datos) {
        if (idActual == null) {
            if (datos.getDocumento() != null && administrativoRepository.existsByDocumento(datos.getDocumento())) {
                throw new BusinessException("Ya existe un administrativo con ese documento");
            }

            if (administrativoRepository.existsByEmailIgnoreCase(datos.getEmail())) {
                throw new BusinessException("Ya existe un administrativo con ese email");
            }

            if (administrativoRepository.existsByTelefono(datos.getTelefono())) {
                throw new BusinessException("Ya existe un administrativo con ese teléfono");
            }

            if (administrativoRepository.existsByUsuarioIgnoreCase(datos.getUsuario())) {
                throw new BusinessException("Ya existe un administrativo con ese usuario");
            }

            if (administrativoRepository.existsByCodigoIgnoreCase(datos.getCodigo())) {
                throw new BusinessException("Ya existe un administrativo con ese código");
            }

            return;
        }

        if (datos.getDocumento() != null
                && administrativoRepository.existsByDocumentoAndIdNot(datos.getDocumento(), idActual)) {
            throw new BusinessException("Ya existe un administrativo con ese documento");
        }

        if (administrativoRepository.existsByEmailIgnoreCaseAndIdNot(datos.getEmail(), idActual)) {
            throw new BusinessException("Ya existe un administrativo con ese email");
        }

        if (administrativoRepository.existsByTelefonoAndIdNot(datos.getTelefono(), idActual)) {
            throw new BusinessException("Ya existe un administrativo con ese teléfono");
        }

        if (administrativoRepository.existsByUsuarioIgnoreCaseAndIdNot(datos.getUsuario(), idActual)) {
            throw new BusinessException("Ya existe un administrativo con ese usuario");
        }

        if (administrativoRepository.existsByCodigoIgnoreCaseAndIdNot(datos.getCodigo(), idActual)) {
            throw new BusinessException("Ya existe un administrativo con ese código");
        }
    }

    public void validarConciliador(Long idActual, DatosPerfilCambioNormalizados datos) {
        if (idActual == null) {
            if (conciliadorRepository.existsByDocumento(datos.getDocumento())) {
                throw new BusinessException("Ya existe un conciliador con ese documento");
            }

            if (conciliadorRepository.existsByEmailIgnoreCase(datos.getEmail())) {
                throw new BusinessException("Ya existe un conciliador con ese email");
            }

            if (conciliadorRepository.existsByTelefono(datos.getTelefono())) {
                throw new BusinessException("Ya existe un conciliador con ese teléfono");
            }

            if (conciliadorRepository.existsByUsuarioIgnoreCase(datos.getUsuario())) {
                throw new BusinessException("Ya existe un conciliador con ese usuario");
            }

            if (conciliadorRepository.existsByCodigoIgnoreCase(datos.getCodigo())) {
                throw new BusinessException("Ya existe un conciliador con ese código");
            }

            return;
        }

        if (conciliadorRepository.existsByDocumentoAndIdNot(datos.getDocumento(), idActual)) {
            throw new BusinessException("Ya existe un conciliador con ese documento");
        }

        if (conciliadorRepository.existsByEmailIgnoreCaseAndIdNot(datos.getEmail(), idActual)) {
            throw new BusinessException("Ya existe un conciliador con ese email");
        }

        if (conciliadorRepository.existsByTelefonoAndIdNot(datos.getTelefono(), idActual)) {
            throw new BusinessException("Ya existe un conciliador con ese teléfono");
        }

        if (conciliadorRepository.existsByUsuarioIgnoreCaseAndIdNot(datos.getUsuario(), idActual)) {
            throw new BusinessException("Ya existe un conciliador con ese usuario");
        }

        if (conciliadorRepository.existsByCodigoIgnoreCaseAndIdNot(datos.getCodigo(), idActual)) {
            throw new BusinessException("Ya existe un conciliador con ese código");
        }
    }
}