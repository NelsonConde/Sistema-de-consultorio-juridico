package co.edu.ufps.legal_cases.security.service.account.perfil.contacto;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AsesorPerfilContactoResolver implements PerfilContactoResolver {

    private final AsesorRepository asesorRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ASESOR;
    }

    @Override
    public PerfilContactoDatos obtenerContacto(Long usuarioSistemaId) {
        Asesor asesor = obtenerActivo(usuarioSistemaId);

        return new PerfilContactoDatos(
                asesor.getNombre(),
                asesor.getEmail(),
                asesor.getTelefono(),
                asesor.getSede().getNombre(),
                asesor.getCodigo());
    }

    @Override
    public void actualizarContacto(Long usuarioSistemaId, ActualizarContactoDTO dto) {
        Asesor asesor = obtenerActivo(usuarioSistemaId);

        asesor.setEmail(dto.getEmail());
        asesor.setTelefono(dto.getTelefono());

        asesorRepository.save(asesor);
    }

    private Asesor obtenerActivo(Long usuarioSistemaId) {
        return asesorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El asesor asociado al usuario no existe o se encuentra inactivo"));
    }
}