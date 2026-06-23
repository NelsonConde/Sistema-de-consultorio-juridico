package co.edu.ufps.legal_cases.business.service.acceso.conciliacion;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacionCodigo;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

// Centraliza preguntas de alcance del módulo de conciliación.
// No valida permisos funcionales; solo responde si el usuario actual está relacionado
// con la consulta o conciliación según su perfil.
@Service
public class ConciliacionAlcanceService {

    private final UsuarioActualService usuarioActualService;

    public ConciliacionAlcanceService(UsuarioActualService usuarioActualService) {
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public boolean puedeVerConciliacion(Conciliacion conciliacion) {
        if (conciliacion == null || !Boolean.TRUE.equals(conciliacion.getActivo())) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        Consulta consulta = conciliacion.getConsulta();

        if (consulta == null) {
            return false;
        }

        PerfilUsuarioActual perfil = usuarioActualService.obtenerPerfilActual();
        Long perfilId = perfil.getPerfilId();

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ASESOR) {
            return consulta.getAsesor() != null
                    && Objects.equals(consulta.getAsesor().getId(), perfilId);
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.MONITOR) {
            return consulta.getMonitor() != null
                    && Objects.equals(consulta.getMonitor().getId(), perfilId);
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.CONCILIADOR) {
            return esConciliadorAsignado(conciliacion);
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ESTUDIANTE) {
            return esEstudianteRelacionado(conciliacion);
        }

        return false;
    }

    @Transactional(readOnly = true)
    public boolean puedeCrearConciliacion(Consulta consulta) {
        if (consulta == null) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        if (usuarioActualService.esEstudiante() || usuarioActualService.esConciliador()) {
            return false;
        }

        PerfilUsuarioActual perfil = usuarioActualService.obtenerPerfilActual();
        Long perfilId = perfil.getPerfilId();

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ASESOR) {
            return consulta.getAsesor() != null
                    && Objects.equals(consulta.getAsesor().getId(), perfilId);
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.MONITOR) {
            return consulta.getMonitor() != null
                    && Objects.equals(consulta.getMonitor().getId(), perfilId);
        }

        return false;
    }

    @Transactional(readOnly = true)
    public boolean puedeProgramarReunion(Conciliacion conciliacion) {
        if (!conciliacionActiva(conciliacion)) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        return usuarioActualService.esConciliador()
                && esConciliadorAsignado(conciliacion);
    }

    @Transactional(readOnly = true)
    public boolean puedeReprogramarReunion(Conciliacion conciliacion) {
        return puedeProgramarReunion(conciliacion);
    }

    @Transactional(readOnly = true)
    public boolean puedeAsignarConciliador(Conciliacion conciliacion) {
        return conciliacionActiva(conciliacion)
                && usuarioActualService.esRolAdministrador();
    }

    @Transactional(readOnly = true)
    public boolean puedeAsignarEstudiante(Conciliacion conciliacion) {
        if (!conciliacionActiva(conciliacion)) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        return usuarioActualService.esConciliador()
                && esConciliadorAsignado(conciliacion);
    }

    @Transactional(readOnly = true)
    public boolean puedeCambiarEstado(Conciliacion conciliacion, String estadoCodigo) {
        if (!conciliacionActiva(conciliacion) || estadoCodigo == null) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        if (!usuarioActualService.esConciliador() || !esConciliadorAsignado(conciliacion)) {
            return false;
        }

        // El conciliador asignado puede operar el flujo, pero no devolverlo a espera.
        return !EstadoConciliacionCodigo.EN_ESPERA.equals(
                EstadoConciliacionCodigo.normalizar(estadoCodigo));
    }

    @Transactional(readOnly = true)
    public boolean puedeSubirActa(Conciliacion conciliacion) {
        if (!conciliacionActiva(conciliacion)) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        return usuarioActualService.esConciliador()
                && esConciliadorAsignado(conciliacion);
    }

    @Transactional(readOnly = true)
    public boolean puedeReemplazarSolicitud(Conciliacion conciliacion) {
        return conciliacionActiva(conciliacion)
                && usuarioActualService.esRolAdministrador();
    }

    @Transactional(readOnly = true)
    public boolean puedeDesactivarConciliacion(Conciliacion conciliacion) {
        return conciliacionActiva(conciliacion)
                && usuarioActualService.esRolAdministrador();
    }

    @Transactional(readOnly = true)
    public boolean esConciliadorAsignado(Conciliacion conciliacion) {
        if (conciliacion == null || conciliacion.getConciliador() == null) {
            return false;
        }

        PerfilUsuarioActual perfil = usuarioActualService.obtenerPerfilActual();

        if (perfil.getTipoPerfil() != TipoPerfilUsuario.CONCILIADOR) {
            return false;
        }

        return Objects.equals(conciliacion.getConciliador().getId(), perfil.getPerfilId());
    }

    @Transactional(readOnly = true)
    public boolean esEstudianteRelacionado(Conciliacion conciliacion) {
        if (conciliacion == null) {
            return false;
        }

        PerfilUsuarioActual perfil = usuarioActualService.obtenerPerfilActual();

        if (perfil.getTipoPerfil() != TipoPerfilUsuario.ESTUDIANTE) {
            return false;
        }

        Long estudianteActualId = perfil.getPerfilId();

        boolean estudianteDeConciliacion = conciliacion.getEstudiante() != null
                && Objects.equals(conciliacion.getEstudiante().getId(), estudianteActualId);

        boolean estudianteDeConsulta = conciliacion.getConsulta() != null
                && conciliacion.getConsulta().getEstudiante() != null
                && Objects.equals(conciliacion.getConsulta().getEstudiante().getId(), estudianteActualId);

        return estudianteDeConciliacion || estudianteDeConsulta;
    }

    @Transactional(readOnly = true)
    public Long obtenerUsuarioActualId() {
        return usuarioActualService.obtenerUsuarioActualId();
    }

    private boolean conciliacionActiva(Conciliacion conciliacion) {
        return conciliacion != null && Boolean.TRUE.equals(conciliacion.getActivo());
    }
}
