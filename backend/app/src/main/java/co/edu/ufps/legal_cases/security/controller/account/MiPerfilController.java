package co.edu.ufps.legal_cases.security.controller.account;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.MiPerfilDTO;
import co.edu.ufps.legal_cases.security.service.account.MiPerfilService;
import jakarta.validation.Valid;

// Endpoints self-service: cualquier usuario autenticado (los 5 perfiles) lee
// y edita su propio contacto. Sin @PreAuthorize de permiso especial a
// proposito - la unica condicion es tener sesion valida.
@RestController
@RequestMapping("/api/mi-perfil")
public class MiPerfilController {

    private final MiPerfilService miPerfilService;

    public MiPerfilController(MiPerfilService miPerfilService) {
        this.miPerfilService = miPerfilService;
    }

    @GetMapping
    public MiPerfilDTO obtenerMiPerfil() {
        return miPerfilService.obtenerMiPerfil();
    }

    @PatchMapping("/contacto")
    public MiPerfilDTO actualizarContacto(@Valid @RequestBody ActualizarContactoDTO dto) {
        return miPerfilService.actualizarContacto(dto);
    }
}