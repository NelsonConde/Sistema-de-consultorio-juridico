package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.concurrency.ConcurrenciaOptimistaValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;

// Este servicio maneja los cambios de Consulta en la BD
// a diferencia del QueryService que solo lee.
@Service
@AllArgsConstructor
public class ConsultaCommandService {

    private static final EstadoConsulta ESTADO_ARCHIVADO = EstadoConsulta.ARCHIVADO;

    private final ConsultaRepository consultaRepository;
    private final ConsultaAccessService consultaAccessService;
    private final ConsultaValidator consultaValidator;
    private final ConsultaMapper consultaMapper;
    private final ConsultaEstadoService consultaEstadoService;
    private final ConsultaConstruccionService consultaConstruccionService;
    private final ConsultaActividadService consultaActividadService;
    private final ConsultaCambioEstructuralValidator consultaCambioEstructuralValidator;
    private final ConcurrenciaOptimistaValidator concurrenciaOptimistaValidator;
    private final EntityManager entityManager;

    @Transactional
    @Auditable(action = "CREAR_CONSULTA", entityName = "Consulta")
    public ConsultaDTO crear(ConsultaDTO dto) {
        consultaAccessService.validarPuedeCrearConsulta();

        concurrenciaOptimistaValidator
                .validarVersionNoEnviadaEnCreacion(dto.getVersion());

        consultaValidator.validarIdNoEnviadoEnCreacion(dto.getId());
        consultaValidator.validarCamposObligatorios(dto);
        consultaValidator.validarEstadoInicialPendienteSiFueEnviado(dto.getEstado());

        boolean solicitaAsignacionResponsables =
                consultaValidator.tieneResponsablesEnDto(dto);

        consultaAccessService
                .validarPuedeAsignarResponsablesConsultaSiAplica(
                        solicitaAsignacionResponsables);

        boolean puedeAsignarResponsables =
                consultaAccessService.usuarioPuedeAsignarResponsables();

        Consulta consulta = consultaConstruccionService.aplicarDatos(
                new Consulta(),
                dto,
                puedeAsignarResponsables);

        // Toda consulta nueva entra primero a revisión administrativa.
        consulta.setEstado(EstadoConsulta.PENDIENTE);

        // Valida relaciones cruzadas del dominio antes de guardar.
        // Ejemplo: tema-área, tipo-tema, asesor-área y personas repetidas.
        consultaValidator.validarCoherenciaDominio(consulta);

        Consulta guardada = consultaRepository.save(consulta);

        // El flush garantiza que la versión asignada por Hibernate ya esté
        // disponible antes de construir la respuesta.
        entityManager.flush();

        return consultaMapper.convertirADTO(guardada);
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_CONSULTA", entityName = "Consulta")
    public ConsultaDTO actualizar(Long id, ConsultaDTO dto) {
        consultaAccessService.validarPuedeEditarConsulta(id);

        Consulta existente = consultaRepository.findByIdConPartes(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "Consulta no encontrada con id: " + id));

        // El cliente debe actualizar exactamente la versión que consultó.
        concurrenciaOptimistaValidator.validarVersion(
                dto.getVersion(),
                existente.getVersion(),
                "consulta");

        // Solo vale la pena cargar la segunda colección si la versión recibida
        // todavía corresponde con la versión persistida.
        consultaRepository.findByIdConContrapartes(id);

        consultaValidator.validarNoArchivada(existente);
        consultaEstadoService.validarPermiteOperacionOperativa(existente);
        consultaValidator.validarCamposObligatorios(dto);
        consultaValidator.validarIdNoCambiado(existente.getId(), dto.getId());

        EstadoConsulta estadoActual = existente.getEstado();

        consultaValidator.validarEstadoNoCambiadoEnActualizacion(
                estadoActual,
                dto.getEstado());

        boolean solicitaCambioResponsables =
                consultaValidator.cambiaResponsablesEnDto(existente, dto);

        consultaAccessService
                .validarPuedeAsignarResponsablesConsultaSiAplica(
                        solicitaCambioResponsables);

        boolean puedeAsignarResponsables =
                consultaAccessService.usuarioPuedeAsignarResponsables();

        boolean tieneActividadAsociada =
                consultaActividadService.tieneActividadAsociada(id);

        // Si la consulta ya tiene actividad, solo se permiten cambios narrativos o
        // complementarios. Los datos estructurales requieren un flujo formal aparte.
        consultaCambioEstructuralValidator.validarSiTieneActividad(
                existente,
                dto,
                tieneActividadAsociada,
                puedeAsignarResponsables);

        consultaConstruccionService.aplicarDatos(
                existente,
                dto,
                puedeAsignarResponsables);

        // Actualizar datos de la consulta no debe cambiar el estado.
        // Para eso existe cambiarEstado().
        existente.setEstado(estadoActual);

        // Valida relaciones cruzadas después de aplicar los cambios del DTO.
        consultaValidator.validarCoherenciaDominio(existente);

        Consulta guardada = consultaRepository.save(existente);

        /*
         * Fuerza el UPDATE antes de construir el DTO.
         *
         * Además de devolver la nueva versión correctamente, este flush hace que
         * un conflicto @Version producido entre la lectura y el UPDATE se detecte
         * dentro de esta operación.
         */
        entityManager.flush();

        return consultaMapper.convertirADTO(guardada);
    }

    @Transactional
    @Auditable(action = "CAMBIAR_ESTADO_CONSULTA", entityName = "Consulta")
    public ConsultaDTO cambiarEstado(Long id, EstadoConsulta estado, Long versionEsperada) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "Consulta no encontrada con id: " + id));

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                consulta.getVersion(),
                "consulta");

        consultaValidator.validarNoArchivada(consulta);
        consultaValidator.validarCambioEstadoPermitido(consulta, estado);
        consultaEstadoService.validarCambioEstado(consulta, estado);
        consultaValidator.validarRequisitosParaEstadoOperativo(
                consulta,
                estado);

        consulta.setEstado(estado);

        Consulta guardada = consultaRepository.save(consulta);
        entityManager.flush();

        return consultaMapper.convertirADTO(guardada);
    }

    // Se conserva el nombre eliminar por compatibilidad con el endpoint antiguo.
    // Para evitar pérdida de información, funciona como archivado lógico.
    @Transactional
    @Auditable(action = "ELIMINAR_CONSULTA", entityName = "Consulta")
    public void eliminar(Long id, Long versionEsperada) {
        consultaAccessService.validarPuedeArchivarConsulta(id);

        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "Consulta no encontrada con id: " + id));

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                consulta.getVersion(),
                "consulta");

        consultaValidator.validarNoArchivadaParaArchivar(consulta);
        consultaEstadoService.validarPuedeArchivar(consulta);

        consulta.setEstado(ESTADO_ARCHIVADO);
        consultaRepository.save(consulta);

        entityManager.flush();
    }

    @Transactional
    @Auditable(action = "ARCHIVAR_CONSULTA", entityName = "Consulta")
    public ConsultaDTO archivar(Long id, Long versionEsperada) {
        consultaAccessService.validarPuedeArchivarConsulta(id);

        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "Consulta no encontrada con id: " + id));

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                consulta.getVersion(),
                "consulta");

        consultaValidator.validarNoArchivadaParaArchivar(consulta);
        consultaEstadoService.validarPuedeArchivar(consulta);

        consulta.setEstado(ESTADO_ARCHIVADO);

        Consulta guardada = consultaRepository.save(consulta);
        entityManager.flush();

        return consultaMapper.convertirADTO(guardada);
    }

    @Transactional
    @Auditable(action = "DESARCHIVAR_CONSULTA", entityName = "Consulta")
    public ConsultaDTO desarchivar(Long id, Long versionEsperada) {
        consultaAccessService.validarPuedeDesarchivarConsulta(id);

        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "Consulta no encontrada con id: " + id));

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                consulta.getVersion(),
                "consulta");

        consultaEstadoService.validarPuedeDesarchivar(consulta);

        // Desarchivar no reabre la consulta.
        // Solo la devuelve al estado cerrado para consulta histórica.
        consulta.setEstado(EstadoConsulta.CERRADO);

        Consulta guardada = consultaRepository.save(consulta);
        entityManager.flush();

        return consultaMapper.convertirADTO(guardada);
    }
}