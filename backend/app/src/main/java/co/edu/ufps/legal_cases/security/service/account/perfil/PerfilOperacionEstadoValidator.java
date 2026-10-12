package co.edu.ufps.legal_cases.security.service.account.perfil;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.PerfilOperacionConflictException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PerfilOperacionEstadoValidator {

    private final AdministrativoRepository administrativoRepository;
    private final AsesorRepository asesorRepository;
    private final ConciliadorRepository conciliadorRepository;
    private final EstudianteRepository estudianteRepository;
    private final MonitorRepository monitorRepository;

    public void validarOperacionDirecta(
            UsuarioSistema usuarioSistema,
            TipoPerfilUsuario tipoPerfil,
            OperacionEstadoPerfil operacion) {

        validarDatosObligatorios(usuarioSistema, tipoPerfil, operacion);

        if (usuarioSistema.getTipoPerfilActual() != tipoPerfil) {
            throw new PerfilOperacionConflictException(
                    "No se puede cambiar directamente el estado de un perfil historico. "
                            + "Utilice el cambio explicito de perfil");
        }

        if (operacion == OperacionEstadoPerfil.DESACTIVAR) {
            throw new PerfilOperacionConflictException(
                    "No se puede desactivar directamente el perfil actual. "
                            + "Suspenda la cuenta o utilice el cambio explicito de perfil");
        }

        if (existeOtroPerfilActivo(usuarioSistema.getId(), tipoPerfil)) {
            throw new PerfilOperacionConflictException(
                    "No se puede activar el perfil actual porque la cuenta ya tiene "
                            + "otro perfil operativo activo");
        }
    }

    public void validarExclusividadAntesDeCambioExplicito(
            UsuarioSistema usuarioSistema) {

        validarUsuarioYTipoActual(usuarioSistema);

        if (existeOtroPerfilActivo(
                usuarioSistema.getId(),
                usuarioSistema.getTipoPerfilActual())) {
            throw new PerfilOperacionConflictException(
                    "No se puede cambiar de perfil porque la cuenta ya tiene "
                            + "otro perfil operativo activo");
        }
    }

    private void validarDatosObligatorios(
            UsuarioSistema usuarioSistema,
            TipoPerfilUsuario tipoPerfil,
            OperacionEstadoPerfil operacion) {

        validarUsuarioYTipoActual(usuarioSistema);

        if (tipoPerfil == null) {
            throw new BusinessException("El tipo de perfil es obligatorio");
        }

        if (operacion == null) {
            throw new BusinessException("La operacion de estado del perfil es obligatoria");
        }
    }

    private void validarUsuarioYTipoActual(
            UsuarioSistema usuarioSistema) {

        if (usuarioSistema == null || usuarioSistema.getId() == null) {
            throw new BusinessException(
                    "El usuario del sistema es obligatorio para validar la operacion de perfil");
        }

        if (usuarioSistema.getTipoPerfilActual() == null) {
            throw new BusinessException(
                    "El usuario del sistema no tiene tipo de perfil actual definido");
        }
    }

    private boolean existeOtroPerfilActivo(
            Long usuarioSistemaId,
            TipoPerfilUsuario tipoPerfilActual) {

        if (tipoPerfilActual != TipoPerfilUsuario.ADMINISTRATIVO
                && administrativoRepository
                        .findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                        .isPresent()) {
            return true;
        }

        if (tipoPerfilActual != TipoPerfilUsuario.ASESOR
                && asesorRepository
                        .findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                        .isPresent()) {
            return true;
        }

        if (tipoPerfilActual != TipoPerfilUsuario.CONCILIADOR
                && conciliadorRepository
                        .findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                        .isPresent()) {
            return true;
        }

        if (tipoPerfilActual != TipoPerfilUsuario.ESTUDIANTE
                && estudianteRepository
                        .findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                        .isPresent()) {
            return true;
        }

        return tipoPerfilActual != TipoPerfilUsuario.MONITOR
                && monitorRepository
                        .findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                        .isPresent();
    }
}
