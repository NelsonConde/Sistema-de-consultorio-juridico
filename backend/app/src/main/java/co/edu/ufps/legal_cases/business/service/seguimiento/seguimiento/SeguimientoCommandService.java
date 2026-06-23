package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoRequestDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.CategoriaSeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaEstadoService;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoNotificacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SeguimientoCommandService {

    private final SeguimientoRepository seguimientoRepository;
    private final CategoriaSeguimientoRepository categoriaSeguimientoRepository;
    private final ConsultaRepository consultaRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final SeguimientoNotificacionService seguimientoNotificacionService;
    private final SeguimientoAccessService seguimientoAccessService;
    private final SeguimientoEstadoService seguimientoEstadoService;
    private final SeguimientoMapper seguimientoMapper;
    private final SeguimientoValidator seguimientoValidator;
    private final ConsultaEstadoService consultaEstadoService;

    @Transactional
    @Auditable(action = "CREAR_SEGUIMIENTO", entityName = "Seguimiento")
    public SeguimientoResponseDTO crear(SeguimientoRequestDTO dto) {
        seguimientoValidator.validarCreacion(dto);
        seguimientoAccessService.validarPuedeCrearSeguimiento(dto.getConsultaId());

        DatosSeguimiento datos = prepararDatos(dto);
        UsuarioSistema autor = obtenerAutorActual();

        Seguimiento seguimiento = new Seguimiento();
        seguimientoMapper.aplicarDatos(seguimiento, datos);
        seguimiento.setAutor(autor);

        // Todo seguimiento nuevo inicia pendiente y activo.
        // El estado representa el flujo real; activo representa borrado lógico.
        seguimiento.setEstado(EstadoSeguimiento.PENDIENTE);
        seguimiento.setActivo(true);

        Seguimiento seguimientoGuardado = seguimientoRepository.save(seguimiento);

        // Las notificaciones dependen del id del seguimiento, por eso se sincronizan
        // después de guardar.
        seguimientoNotificacionService.sincronizarNotificaciones(seguimientoGuardado.getId());

        return seguimientoMapper.convertirAResponseDTO(seguimientoGuardado);
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_SEGUIMIENTO", entityName = "Seguimiento")
    public SeguimientoResponseDTO actualizar(Long id, SeguimientoRequestDTO dto) {
        seguimientoAccessService.validarPuedeEditarSeguimiento(id);

        Seguimiento seguimiento = buscarPorId(id);

        consultaEstadoService.validarPermiteOperacionOperativa(seguimiento.getConsulta());

        seguimientoValidator.validarSeguimientoEditable(seguimiento);
        seguimientoValidator.validarActualizacion(id, dto);
        seguimientoValidator.validarNoCambieConsulta(seguimiento, dto);

        DatosSeguimiento datos = prepararDatos(dto);

        seguimientoValidator.validarExistenCambios(seguimiento, datos);

        seguimientoMapper.aplicarDatos(seguimiento, datos);

        Seguimiento seguimientoGuardado = seguimientoRepository.save(seguimiento);

        // Solo los seguimientos pendientes conservan notificaciones activas.
        seguimientoEstadoService.aplicarEfectosPorEstado(seguimientoGuardado);

        return seguimientoMapper.convertirAResponseDTO(seguimientoGuardado);
    }

    @Transactional
    @Auditable(action = "CAMBIAR_ESTADO_SEGUIMIENTO", entityName = "Seguimiento")
    public SeguimientoResponseDTO cambiarEstadoSeguimiento(Long id, EstadoSeguimiento estado) {
        seguimientoAccessService.validarPuedeEditarSeguimiento(id);

        Seguimiento seguimiento = seguimientoEstadoService.cambiarEstado(id, estado);

        return seguimientoMapper.convertirAResponseDTO(seguimiento);
    }

    @Transactional
    @Auditable(action = "ELIMINAR_SEGUIMIENTO", entityName = "Seguimiento")
    public void eliminar(Long id) {
        seguimientoAccessService.validarPuedeEliminarSeguimiento(id);

        Seguimiento seguimiento = buscarPorId(id);

        consultaEstadoService.validarPermiteOperacionOperativa(seguimiento.getConsulta());

        // Primero se cancelan pendientes; las enviadas quedan como historial.
        seguimientoNotificacionService.cancelarNotificacionesPendientes(seguimiento.getId());

        // El seguimiento queda inactivo para no perder trazabilidad dentro de la
        // consulta.
        seguimiento.setActivo(false);

        seguimientoRepository.save(seguimiento);
    }

    private DatosSeguimiento prepararDatos(SeguimientoRequestDTO dto) {
        String descripcion = seguimientoValidator.normalizarDescripcion(dto.getDescripcion());

        seguimientoValidator.validarDatosSeguimiento(
                dto.getFechaEntrega(),
                dto.getDiasNotificacion());

        CategoriaSeguimiento categoria = obtenerCategoriaActiva(dto.getCategoriaSeguimientoId());
        Consulta consulta = obtenerConsulta(dto.getConsultaId());

        Boolean notificarPartes = valorBooleano(dto.getNotificarPartes());
        Boolean notificarEstudiante = valorBooleano(dto.getNotificarEstudiante());
        Boolean alertaDisciplinaria = valorBooleano(dto.getAlertaDisciplinaria());

        seguimientoValidator.validarNotificarEstudianteConConsulta(
                notificarEstudiante,
                consulta);

        return new DatosSeguimiento(
                descripcion,
                dto.getFechaEntrega(),
                dto.getDiasNotificacion(),
                notificarPartes,
                notificarEstudiante,
                alertaDisciplinaria,
                categoria,
                consulta);
    }

    private Seguimiento buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del seguimiento es obligatorio");
        }

        return seguimientoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado con id: " + id));
    }

    private CategoriaSeguimiento obtenerCategoriaActiva(Long categoriaId) {
        if (categoriaId == null) {
            throw new BusinessException("La categoría del seguimiento es obligatoria");
        }

        return categoriaSeguimientoRepository.findByIdAndActivoTrue(categoriaId)
                .orElseThrow(() -> new BusinessException(
                        "Categoría de seguimiento no encontrada o inactiva con id: " + categoriaId));
    }

    private Consulta obtenerConsulta(Long consultaId) {
        if (consultaId == null) {
            throw new BusinessException("La consulta es obligatoria");
        }

        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new BusinessException("Consulta no encontrada con id: " + consultaId));

        consultaEstadoService.validarPermiteOperacionOperativa(consulta);

        return consulta;
    }

    private UsuarioSistema obtenerAutorActual() {
        Long usuarioActualId = seguimientoAccessService.obtenerUsuarioActualId();

        return usuarioSistemaRepository.findById(usuarioActualId)
                .orElseThrow(() -> new BusinessException("Autor no encontrado con id: " + usuarioActualId));
    }

    private Boolean valorBooleano(Boolean valor) {
        return valor != null ? valor : false;
    }
}