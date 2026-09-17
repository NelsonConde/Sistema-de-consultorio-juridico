package co.edu.ufps.legal_cases.security.service.account.perfil.contacto;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MonitorPerfilContactoResolver implements PerfilContactoResolver {

    private final MonitorRepository monitorRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.MONITOR;
    }

    @Override
    public PerfilContactoDatos obtenerContacto(Long usuarioSistemaId) {
        Monitor monitor = obtenerActivo(usuarioSistemaId);

        return new PerfilContactoDatos(
                monitor.getNombre(),
                monitor.getEmail(),
                monitor.getTelefono(),
                monitor.getSede().getNombre(),
                monitor.getCodigo());
    }

    @Override
    public void actualizarContacto(Long usuarioSistemaId, ActualizarContactoDTO dto) {
        Monitor monitor = obtenerActivo(usuarioSistemaId);

        monitor.setEmail(dto.getEmail());
        monitor.setTelefono(dto.getTelefono());

        monitorRepository.save(monitor);
    }

    private Monitor obtenerActivo(Long usuarioSistemaId) {
        return monitorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El monitor asociado al usuario no existe o se encuentra inactivo"));
    }
}