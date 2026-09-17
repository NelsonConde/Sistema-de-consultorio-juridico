package co.edu.ufps.legal_cases.security.service.account.perfil.contacto;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdministrativoPerfilContactoResolver implements PerfilContactoResolver {

    private final AdministrativoRepository administrativoRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ADMINISTRATIVO;
    }

    @Override
    public PerfilContactoDatos obtenerContacto(Long usuarioSistemaId) {
        Administrativo administrativo = obtenerActivo(usuarioSistemaId);

        return new PerfilContactoDatos(
                administrativo.getNombre(),
                administrativo.getEmail(),
                administrativo.getTelefono(),
                administrativo.getSede().getNombre(),
                administrativo.getCodigo());
    }

    @Override
    public void actualizarContacto(Long usuarioSistemaId, ActualizarContactoDTO dto) {
        Administrativo administrativo = obtenerActivo(usuarioSistemaId);

        administrativo.setEmail(dto.getEmail());
        administrativo.setTelefono(dto.getTelefono());

        administrativoRepository.save(administrativo);
    }

    private Administrativo obtenerActivo(Long usuarioSistemaId) {
        return administrativoRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El administrativo asociado al usuario no existe o se encuentra inactivo"));
    }
}