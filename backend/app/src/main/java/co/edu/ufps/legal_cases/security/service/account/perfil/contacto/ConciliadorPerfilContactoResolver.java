package co.edu.ufps.legal_cases.security.service.account.perfil.contacto;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConciliadorPerfilContactoResolver implements PerfilContactoResolver {

    private final ConciliadorRepository conciliadorRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.CONCILIADOR;
    }

    @Override
    public PerfilContactoDatos obtenerContacto(Long usuarioSistemaId) {
        Conciliador conciliador = obtenerActivo(usuarioSistemaId);

        return new PerfilContactoDatos(
                conciliador.getNombre(),
                conciliador.getEmail(),
                conciliador.getTelefono(),
                conciliador.getSede().getNombre(),
                conciliador.getCodigo());
    }

    @Override
    public void actualizarContacto(Long usuarioSistemaId, ActualizarContactoDTO dto) {
        Conciliador conciliador = obtenerActivo(usuarioSistemaId);

        conciliador.setEmail(dto.getEmail());
        conciliador.setTelefono(dto.getTelefono());

        conciliadorRepository.save(conciliador);
    }

    private Conciliador obtenerActivo(Long usuarioSistemaId) {
        return conciliadorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El conciliador asociado al usuario no existe o se encuentra inactivo"));
    }
}