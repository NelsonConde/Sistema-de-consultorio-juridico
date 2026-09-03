package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

// Resuelve el alcance desde la autenticación, no desde datos enviados por el cliente.
@Service
public class EstadisticasPerfilActualQueryService {

    private final UsuarioActualService usuarioActualService;
    private final EstadisticasPerfilQueryService estadisticasPerfilQueryService;

    public EstadisticasPerfilActualQueryService(
            UsuarioActualService usuarioActualService,
            EstadisticasPerfilQueryService estadisticasPerfilQueryService) {
        this.usuarioActualService = usuarioActualService;
        this.estadisticasPerfilQueryService = estadisticasPerfilQueryService;
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtener(int año, int semestre) {
        PerfilUsuarioActual perfilActual =
                usuarioActualService.obtenerPerfilActual();

        return switch (perfilActual.getTipoPerfil()) {
            case ESTUDIANTE ->
                    estadisticasPerfilQueryService.obtenerPorEstudiante(
                            año,
                            semestre,
                            perfilActual.getPerfilId());

            case ASESOR ->
                    estadisticasPerfilQueryService.obtenerPorAsesor(
                            año,
                            semestre,
                            perfilActual.getPerfilId());

            case MONITOR ->
                    estadisticasPerfilQueryService.obtenerPorMonitor(
                            año,
                            semestre,
                            perfilActual.getPerfilId());

            case ADMINISTRATIVO, CONCILIADOR ->
                    throw new AccessDeniedException(
                            "El perfil autenticado no dispone de estadísticas personales");
        };
    }
}