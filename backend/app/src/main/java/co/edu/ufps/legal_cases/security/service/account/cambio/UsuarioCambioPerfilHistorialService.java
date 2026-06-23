package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioCambioPerfilHistorial;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioCambioPerfilHistorialRepository;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

@Service
@Transactional
public class UsuarioCambioPerfilHistorialService {

    private final UsuarioCambioPerfilHistorialRepository historialRepository;

    public UsuarioCambioPerfilHistorialService(
            UsuarioCambioPerfilHistorialRepository historialRepository) {
        this.historialRepository = historialRepository;
    }

    public void registrarCambio(
            UsuarioSistema usuarioSistema,
            PerfilUsuarioActual perfilAnterior,
            Rol rolAnterior,
            TipoPerfilUsuario tipoPerfilNuevo,
            Long perfilNuevoId,
            Rol rolNuevo,
            String motivo,
            UsuarioSistema cambiadoPorUsuario,
            String cambiadoPorUsername) {

        // Aqui se verifica que los datos no esten vacios
        validarDatosHistorial(
                usuarioSistema,
                perfilAnterior,
                rolAnterior,
                tipoPerfilNuevo,
                perfilNuevoId,
                rolNuevo,
                motivo);

        // Aqui creo el historial del cambio
        UsuarioCambioPerfilHistorial historial = new UsuarioCambioPerfilHistorial();

        historial.setUsuarioSistema(usuarioSistema);

        historial.setTipoPerfilAnterior(perfilAnterior.getTipoPerfil());
        historial.setPerfilAnteriorId(perfilAnterior.getPerfilId());
        historial.setRolAnterior(rolAnterior);
        historial.setRolAnteriorNombre(rolAnterior.getNombre());

        historial.setTipoPerfilNuevo(tipoPerfilNuevo);
        historial.setPerfilNuevoId(perfilNuevoId);
        historial.setRolNuevo(rolNuevo);
        historial.setRolNuevoNombre(rolNuevo.getNombre());

        historial.setCambiadoPorUsuario(cambiadoPorUsuario);
        historial.setCambiadoPorUsername(obtenerUsernameCambio(cambiadoPorUsuario, cambiadoPorUsername));

        historial.setMotivo(normalizarMotivo(motivo));

        historialRepository.save(historial);
    }

    private void validarDatosHistorial(
            UsuarioSistema usuarioSistema,
            PerfilUsuarioActual perfilAnterior,
            Rol rolAnterior,
            TipoPerfilUsuario tipoPerfilNuevo,
            Long perfilNuevoId,
            Rol rolNuevo,
            String motivo) {

        if (usuarioSistema == null || usuarioSistema.getId() == null) {
            throw new BusinessException("El usuario del sistema es obligatorio para registrar el historial");
        }

        if (perfilAnterior == null || perfilAnterior.getPerfilId() == null || perfilAnterior.getTipoPerfil() == null) {
            throw new BusinessException("El perfil anterior es obligatorio para registrar el historial");
        }

        if (rolAnterior == null || rolAnterior.getId() == null) {
            throw new BusinessException("El rol anterior es obligatorio para registrar el historial");
        }

        if (tipoPerfilNuevo == null) {
            throw new BusinessException("El tipo de perfil nuevo es obligatorio para registrar el historial");
        }

        if (perfilNuevoId == null) {
            throw new BusinessException("El id del perfil nuevo es obligatorio para registrar el historial");
        }

        if (rolNuevo == null || rolNuevo.getId() == null) {
            throw new BusinessException("El rol nuevo es obligatorio para registrar el historial");
        }

        normalizarMotivo(motivo);
    }

    private String obtenerUsernameCambio(UsuarioSistema cambiadoPorUsuario, String cambiadoPorUsername) {
        if (cambiadoPorUsuario != null && cambiadoPorUsuario.getUsername() != null) {
            return cambiadoPorUsuario.getUsername();
        }

        return normalizarEmail(cambiadoPorUsername);
    }

    private String normalizarMotivo(String motivo) {
        String motivoNormalizado = normalizarTexto(motivo);

        if (motivoNormalizado == null || motivoNormalizado.isBlank()) {
            throw new BusinessException("El motivo del cambio es obligatorio");
        }

        if (motivoNormalizado.length() > 500) {
            throw new BusinessException("El motivo del cambio no puede superar 500 caracteres");
        }

        return motivoNormalizado;
    }
}