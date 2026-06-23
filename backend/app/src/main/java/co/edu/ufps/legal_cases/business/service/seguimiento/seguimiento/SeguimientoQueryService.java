package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class SeguimientoQueryService {

    private static final EstadoConsulta ESTADO_ARCHIVADO = EstadoConsulta.ARCHIVADO;

    private final SeguimientoRepository seguimientoRepository;
    private final SeguimientoAccessService seguimientoAccessService;
    private final SeguimientoMapper seguimientoMapper;
    private final SeguimientoValidator seguimientoValidator;

    public SeguimientoQueryService(
            SeguimientoRepository seguimientoRepository,
            SeguimientoAccessService seguimientoAccessService,
            SeguimientoMapper seguimientoMapper,
            SeguimientoValidator seguimientoValidator) {
        this.seguimientoRepository = seguimientoRepository;
        this.seguimientoAccessService = seguimientoAccessService;
        this.seguimientoMapper = seguimientoMapper;
        this.seguimientoValidator = seguimientoValidator;
    }

    @Transactional(readOnly = true)
    public List<SeguimientoResponseDTO> listarParaCalendario() {
        // Valida que el usuario tenga el permiso de ver seguimientos.
        seguimientoAccessService.validarTienePermisoVerSeguimientos();

        // Se buscan todos los seguimientos activos, se filtran por alcance
        // con `puedeVerSeguimiento` y se ordenan por fecha de entrega.
        return seguimientoRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActivo()))
                .filter(seguimientoAccessService::puedeVerSeguimiento)
                .sorted(Comparator.comparing(
                        // Ordenar por fechaEntrega, colocando nulos al final
                        s -> s.getFechaEntrega(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(seguimientoMapper::convertirAResponseDTO)
                .toList();
    }

    // Lista seguimientos activos de una consulta después de validar alcance sobre esa consulta.
    // No expone seguimientos de consultas archivadas en flujos operativos.
    @Transactional(readOnly = true)
    public List<SeguimientoResponseDTO> listarPorConsulta(Long consultaId) {
        seguimientoAccessService.validarPuedeListarSeguimientosDeConsulta(consultaId);

        return seguimientoRepository
                .findByConsulta_IdAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        consultaId,
                        ESTADO_ARCHIVADO)
                .stream()
                .map(seguimientoMapper::convertirAResponseDTO)
                .toList();
    }

    // Para estudiante solo se muestran los seguimientos marcados como visibles.
    // Además se filtra por alcance para evitar exponer seguimientos de otra consulta.
    // También se excluyen consultas archivadas para evitar contaminación visual.
    @Transactional(readOnly = true)
    public List<SeguimientoResponseDTO> listarVisiblesParaEstudiantePorConsulta(Long consultaId) {
        seguimientoAccessService.validarPuedeListarSeguimientosVisiblesParaEstudiante(consultaId);

        return seguimientoRepository
                .findByConsulta_IdAndNotificarEstudianteTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        consultaId,
                        ESTADO_ARCHIVADO)
                .stream()
                .filter(seguimientoAccessService::puedeVerSeguimiento)
                .map(seguimientoMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeguimientoResponseDTO> listarPorAutor(Long autorId) {
        seguimientoAccessService.validarPuedeListarSeguimientosPorAutor(autorId);

        return seguimientoRepository
                .findByAutor_IdAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        autorId,
                        ESTADO_ARCHIVADO)
                .stream()
                .filter(seguimientoAccessService::puedeVerSeguimiento)
                .map(seguimientoMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeguimientoResponseDTO> listarAlertasDisciplinarias() {
        seguimientoAccessService.validarPuedeListarAlertasDisciplinarias();

        return seguimientoRepository
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        ESTADO_ARCHIVADO)
                .stream()
                .map(seguimientoMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeguimientoResponseDTO> listarPorFechaEntrega(LocalDate fechaEntrega) {
        seguimientoAccessService.validarPuedeListarSeguimientosPorFechaEntrega();
        seguimientoValidator.validarFechaEntregaObligatoria(fechaEntrega);

        return seguimientoRepository
                .findByFechaEntregaAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        fechaEntrega,
                        ESTADO_ARCHIVADO)
                .stream()
                .filter(seguimientoAccessService::puedeVerSeguimiento)
                .map(seguimientoMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeguimientoResponseDTO obtenerPorId(Long id) {
        seguimientoAccessService.validarPuedeVerSeguimiento(id);

        return seguimientoMapper.convertirAResponseDTO(buscarPorId(id));
    }

    private Seguimiento buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del seguimiento es obligatorio");
        }

        return seguimientoRepository.findByIdAndActivoTrueAndConsulta_EstadoNot(id, ESTADO_ARCHIVADO)
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado con id: " + id));
    }
}