package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResumenDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoResumenProjection;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoAgendaProjection;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.AlcanceAlertasDisciplinarias;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;

@Service
public class SeguimientoQueryService {

        private static final EstadoConsulta ESTADO_ARCHIVADO = EstadoConsulta.ARCHIVADO;
        private static final int PAGINA_MINIMA = 1;
        private static final int TAMANO_MINIMO = 1;
        private static final int TAMANO_MAXIMO = 50;
        private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

        private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
                        "id", "id",
                        "fechaCreacion", "fechaCreacion",
                        "fechaEntrega", "fechaEntrega",
                        "estado", "estado",
                        "categoria", "categoriaSeguimiento.nombre",
                        "autor", "autor.username");

        private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
                        "categoria",
                        "autor");

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

        // Listado paginado principal – Bloque A (SCRUM-269).
        @Transactional(readOnly = true)
        public PageResponseDTO<SeguimientoResumenDTO> buscarParaUsuarioActual(
                        String search,
                        int page,
                        int size,
                        String sortBy,
                        String direction,
                        EstadoSeguimiento estado,
                        LocalDate fechaDesde,
                        LocalDate fechaHasta,
                        Long consultaId,
                        Long autorId) {
                seguimientoAccessService.validarTienePermisoVerSeguimientos();

                validarPaginacion(page, size);
                validarRangoFechas(fechaDesde, fechaHasta);

                String termino = normalizarBusqueda(search);
                Sort sort = construirSort(sortBy, direction);
                PageRequest pageable = PageRequest.of(page - 1, size, sort);
                LocalDateTime fechaDesdeInicio = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
                LocalDateTime fechaHastaExclusiva = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null;

                boolean alcanceGlobal = seguimientoAccessService.usuarioEsAdministrador();
                String tipoPerfil = null;
                Long perfilId = null;

                if (!alcanceGlobal) {
                        PerfilUsuarioActual perfil = seguimientoAccessService.obtenerPerfilActual();
                        if (perfil != null) {
                                tipoPerfil = perfil.getTipoPerfil() != null ? perfil.getTipoPerfil().name() : null;
                                perfilId = perfil.getPerfilId();
                        }
                }

                Page<SeguimientoResumenProjection> resultado = seguimientoRepository.buscarResumenPaginado(
                                termino,
                                estado,
                                fechaDesdeInicio,
                                fechaHastaExclusiva,
                                consultaId,
                                autorId,
                                alcanceGlobal,
                                tipoPerfil,
                                perfilId,
                                ESTADO_ARCHIVADO,
                                pageable);

                List<SeguimientoResumenDTO> contenido = resultado.getContent()
                                .stream()
                                .map(seguimientoMapper::convertirAResumenDTO)
                                .toList();

                return new PageResponseDTO<>(
                                contenido,
                                page,
                                size,
                                resultado.getTotalElements(),
                                resultado.getTotalPages());
        }

        // Calendario por rango – Bloque B (SCRUM-269).
        // Reemplaza listarParaCalendario() que usaba findAll() + filtros en memoria.
        // El endpoint GET /api/seguimientos/calendario ahora requiere from y to.
        @Transactional(readOnly = true)
        public List<SeguimientoResponseDTO> listarCalendarioPorRango(LocalDate from, LocalDate to) {
                seguimientoAccessService.validarTienePermisoVerSeguimientos();

                if (from == null || to == null || !from.isBefore(to)) {
                        throw new BusinessException("El rango debe ser válido y no vacío");
                }
                if (from.plusMonths(3).isBefore(to)) {
                        throw new BusinessException("El rango no puede superar tres meses");
                }

                boolean alcanceGlobal = seguimientoAccessService.usuarioEsAdministrador();
                String tipoPerfil = null;
                Long perfilId = null;

                if (!alcanceGlobal) {
                        PerfilUsuarioActual perfil = seguimientoAccessService.obtenerPerfilActual();
                        if (perfil == null || perfil.getTipoPerfil() == null) {
                                // fail-closed: perfil no soportado no ve nada
                                return List.of();
                        }
                        tipoPerfil = perfil.getTipoPerfil().name();
                        perfilId = perfil.getPerfilId();
                }

                return seguimientoRepository
                        .buscarParaCalendarioPorRangoConScope(
                                from,
                                to,
                                alcanceGlobal,
                                tipoPerfil,
                                perfilId,
                                ESTADO_ARCHIVADO)
                        .stream()
                        .map(seguimientoMapper::convertirAResponseDTO)
                        .toList();
        }

        // Lista seguimientos activos de una consulta después de validar alcance sobre
        // esa consulta. No expone seguimientos de consultas archivadas en flujos operativos.
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
        // El post-filtrado puedeVerSeguimiento fue eliminado: la query ya garantiza scope.
        @Transactional(readOnly = true)
        public List<SeguimientoResponseDTO> listarVisiblesParaEstudiantePorConsulta(Long consultaId) {
                seguimientoAccessService.validarPuedeListarSeguimientosVisiblesParaEstudiante(consultaId);

                return seguimientoRepository
                                .findByConsulta_IdAndNotificarEstudianteTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                                                consultaId,
                                                ESTADO_ARCHIVADO)
                                .stream()
                                .map(seguimientoMapper::convertirAResponseDTO)
                                .toList();
        }

        // Listar por autor con scope de consulta en SQL – Bloque B (SCRUM-269).
        // La validación de autorId (ADMIN vs no-admin) se preserva en SeguimientoAccessService.
        // El post-filtrado puedeVerSeguimiento fue eliminado: el scope está en la query.
        @Transactional(readOnly = true)
        public List<SeguimientoResponseDTO> listarPorAutor(Long autorId) {
                seguimientoAccessService.validarPuedeListarSeguimientosPorAutor(autorId);

                boolean alcanceGlobal = seguimientoAccessService.usuarioEsAdministrador();
                String tipoPerfil = null;
                Long perfilId = null;

                if (!alcanceGlobal) {
                        PerfilUsuarioActual perfil = seguimientoAccessService.obtenerPerfilActual();
                        if (perfil != null && perfil.getTipoPerfil() != null) {
                                tipoPerfil = perfil.getTipoPerfil().name();
                                perfilId = perfil.getPerfilId();
                        }
                }

                return seguimientoRepository
                                .buscarPorAutorConScope(
                                        autorId,
                                        alcanceGlobal,
                                        tipoPerfil,
                                        perfilId,
                                        ESTADO_ARCHIVADO)
                                .stream()
                                .map(seguimientoMapper::convertirAResponseDTO)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<SeguimientoResponseDTO> listarAlertasDisciplinarias() {
                AlcanceAlertasDisciplinarias alcance = seguimientoAccessService.resolverAlcanceAlertasDisciplinarias();

                List<Seguimiento> alertas = switch (alcance.tipo()) {
                        case GLOBAL -> seguimientoRepository
                                        .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                                                        ESTADO_ARCHIVADO);

                        case ASESOR -> seguimientoRepository.findAlertasDisciplinariasByAsesorScope(
                                        alcance.perfilId(),
                                        ESTADO_ARCHIVADO);

                        case MONITOR -> seguimientoRepository.findAlertasDisciplinariasByMonitorScope(
                                        alcance.perfilId(),
                                        ESTADO_ARCHIVADO);
                };

                return alertas.stream()
                                .map(seguimientoMapper::convertirAResponseDTO)
                                .toList();
        }

        // Listar por fecha de entrega con scope en SQL – Bloque B (SCRUM-269).
        // El post-filtrado puedeVerSeguimiento fue eliminado.
        @Transactional(readOnly = true)
        public List<SeguimientoResponseDTO> listarPorFechaEntrega(LocalDate fechaEntrega) {
                seguimientoAccessService.validarPuedeListarSeguimientosPorFechaEntrega();
                seguimientoValidator.validarFechaEntregaObligatoria(fechaEntrega);

                boolean alcanceGlobal = seguimientoAccessService.usuarioEsAdministrador();
                String tipoPerfil = null;
                Long perfilId = null;

                if (!alcanceGlobal) {
                        PerfilUsuarioActual perfil = seguimientoAccessService.obtenerPerfilActual();
                        if (perfil != null && perfil.getTipoPerfil() != null) {
                                tipoPerfil = perfil.getTipoPerfil().name();
                                perfilId = perfil.getPerfilId();
                        }
                }

                return seguimientoRepository
                                .buscarPorFechaEntregaConScope(
                                        fechaEntrega,
                                        alcanceGlobal,
                                        tipoPerfil,
                                        perfilId,
                                        ESTADO_ARCHIVADO)
                                .stream()
                                .map(seguimientoMapper::convertirAResponseDTO)
                                .toList();
        }

        // Consulta para Agenda: rango y scope resueltos en SQL – Bloque B (SCRUM-269).
        // AgendaQueryService llama este método; no filtra fechaEntrega en Java.
        @Transactional(readOnly = true)
        public List<SeguimientoAgendaProjection> buscarParaAgenda(LocalDate from, LocalDate to) {
                seguimientoAccessService.validarTienePermisoVerSeguimientos();

                boolean alcanceGlobal = seguimientoAccessService.usuarioEsAdministrador();
                String tipoPerfil = null;
                Long perfilId = null;

                if (!alcanceGlobal) {
                        PerfilUsuarioActual perfil = seguimientoAccessService.obtenerPerfilActual();
                        if (perfil == null || perfil.getTipoPerfil() == null) {
                                // fail-closed: perfil no soportado no ve nada en la agenda
                                return List.of();
                        }
                        tipoPerfil = perfil.getTipoPerfil().name();
                        perfilId = perfil.getPerfilId();
                }

                return seguimientoRepository.buscarParaAgenda(
                        from,
                        to,
                        alcanceGlobal,
                        tipoPerfil,
                        perfilId,
                        ESTADO_ARCHIVADO);
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

        private void validarPaginacion(int page, int size) {
                if (page < PAGINA_MINIMA) {
                        throw new BusinessException("La pagina debe ser mayor o igual a 1");
                }

                if (size < TAMANO_MINIMO || size > TAMANO_MAXIMO) {
                        throw new BusinessException("El tamano de pagina debe estar entre 1 y 50");
                }
        }

        private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
                if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
                        throw new BusinessException("La fecha desde no puede ser posterior a la fecha hasta");
                }
        }

        private Sort construirSort(String sortBy, String direction) {
                if (sortBy == null || sortBy.isBlank()) {
                        throw new BusinessException("El campo de ordenamiento no puede estar vacio");
                }

                String campoLimpio = sortBy.trim();
                if (!CAMPOS_ORDENAMIENTO.containsKey(campoLimpio)) {
                        throw new BusinessException("El campo de ordenamiento '" + campoLimpio + "' no es valido");
                }

                if (direction == null || direction.isBlank()) {
                        throw new BusinessException("La direccion de ordenamiento no puede estar vacia");
                }

                String direccionLimpia = direction.trim().toLowerCase(Locale.ROOT);
                Sort.Direction sortDirection;
                if ("asc".equals(direccionLimpia)) {
                        sortDirection = Sort.Direction.ASC;
                } else if ("desc".equals(direccionLimpia)) {
                        sortDirection = Sort.Direction.DESC;
                } else {
                        throw new BusinessException("La direccion de ordenamiento debe ser 'asc' o 'desc'");
                }

                String propiedadJPA = CAMPOS_ORDENAMIENTO.get(campoLimpio);
                Sort.Order ordenPrincipal;
                if (CAMPOS_IGNORE_CASE.contains(campoLimpio)) {
                        ordenPrincipal = new Sort.Order(sortDirection, propiedadJPA).ignoreCase();
                } else {
                        ordenPrincipal = new Sort.Order(sortDirection, propiedadJPA);
                }

                if ("id".equals(campoLimpio)) {
                        return Sort.by(ordenPrincipal);
                }

                return Sort.by(ordenPrincipal, Sort.Order.asc("id"));
        }

        private String normalizarBusqueda(String search) {
                if (search == null) {
                        return null;
                }

                String termino = search.trim().replaceAll("\\s+", " ");

                if (termino.isEmpty()) {
                        return null;
                }

                if (termino.length() > LONGITUD_MAXIMA_BUSQUEDA) {
                        throw new BusinessException("La busqueda no puede superar los 100 caracteres");
                }

                return termino;
        }
}
