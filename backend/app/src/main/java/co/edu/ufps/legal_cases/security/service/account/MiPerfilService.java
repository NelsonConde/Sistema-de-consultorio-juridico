package co.edu.ufps.legal_cases.security.service.account;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.MiPerfilDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.contacto.PerfilContactoResolverRegistry;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

@Service
public class MiPerfilService {

    private final UsuarioActualService usuarioActualService;
    private final PerfilContactoResolverRegistry perfilContactoResolverRegistry;
    private final MiPerfilMapper miPerfilMapper;

    public MiPerfilService(
            UsuarioActualService usuarioActualService,
            PerfilContactoResolverRegistry perfilContactoResolverRegistry,
            MiPerfilMapper miPerfilMapper) {

        this.usuarioActualService = usuarioActualService;
        this.perfilContactoResolverRegistry = perfilContactoResolverRegistry;
        this.miPerfilMapper = miPerfilMapper;
    }

    @Transactional(readOnly = true)
    public MiPerfilDTO obtenerMiPerfil() {
        UsuarioSistema usuario = usuarioActualService.obtenerUsuarioActual();

        PerfilContactoDatos contacto = perfilContactoResolverRegistry
                .obtenerResolver(usuario.getTipoPerfilActual())
                .obtenerContacto(usuario.getId());

        return miPerfilMapper.convertirADTO(usuario, contacto);
    }

    @Transactional
    public MiPerfilDTO actualizarContacto(ActualizarContactoDTO dto) {
        UsuarioSistema usuario = usuarioActualService.obtenerUsuarioActual();

        try {
            perfilContactoResolverRegistry
                    .obtenerResolver(usuario.getTipoPerfilActual())
                    .actualizarContacto(usuario.getId(), dto);
        } catch (DataIntegrityViolationException ex) {
            // email/telefono son "unique" por tabla de perfil; no se expone
            // el detalle crudo de la restriccion de base de datos.
            throw new BusinessException("El correo o telefono ya esta en uso por otro usuario");
        }

        return obtenerMiPerfil();
    }
}