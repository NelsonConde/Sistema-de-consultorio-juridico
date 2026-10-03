package co.edu.ufps.legal_cases.security.service.account.perfil.contacto;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EstudiantePerfilContactoResolver implements PerfilContactoResolver {

    private final EstudianteRepository estudianteRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ESTUDIANTE;
    }

    @Override
    public PerfilContactoDatos obtenerContacto(Long usuarioSistemaId) {
        Estudiante estudiante = obtenerActivo(usuarioSistemaId);

        return new PerfilContactoDatos(
                estudiante.getNombre(),
                estudiante.getEmail(),
                estudiante.getTelefono(),
                estudiante.getSede().getNombre(),
                estudiante.getCodigo());
    }

    @Override
    public void actualizarContacto(Long usuarioSistemaId, ActualizarContactoDTO dto) {
        Estudiante estudiante = obtenerActivo(usuarioSistemaId);

        estudiante.setEmail(dto.getEmail());
        estudiante.setTelefono(dto.getTelefono());

        estudianteRepository.save(estudiante);
    }

    private Estudiante obtenerActivo(Long usuarioSistemaId) {
        return estudianteRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El estudiante asociado al usuario no existe o se encuentra inactivo"));
    }
}