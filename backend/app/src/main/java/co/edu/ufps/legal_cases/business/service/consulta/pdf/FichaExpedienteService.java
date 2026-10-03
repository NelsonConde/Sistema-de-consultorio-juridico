package co.edu.ufps.legal_cases.business.service.consulta.pdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ClasificacionFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ConciliacionFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.DocumentoIndiceDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.EstadoResultadoFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.IdentificacionFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.PartesFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.PersonaDetalleDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ProcesoFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.RespuestaFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ResponsableDetalleDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ResponsablesFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.SeguimientoFichaDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.proceso.Proceso;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;

/**
 * Servicio de consolidación de datos y orquestación para la generación de la ficha
 * integral del expediente (PB-35 / CC-OPT-004).
 * Valida autorización de acceso antes de leer datos o generar el documento.
 */
@Service
public class FichaExpedienteService {

    private static final List<FileAssetStatus> ESTADOS_DOCUMENTOS_VIGENTES = List.of(
            FileAssetStatus.READY,
            FileAssetStatus.ACTIVE
    );

    private final ConsultaAccessService consultaAccessService;
    private final ConsultaRepository consultaRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private final ProcesoRepository procesoRepository;
    private final ConciliacionRepository conciliacionRepository;
    private final FileAssetRepository fileAssetRepository;
    private final FichaExpedientePdfService fichaExpedientePdfService;

    public FichaExpedienteService(
            ConsultaAccessService consultaAccessService,
            ConsultaRepository consultaRepository,
            SeguimientoRepository seguimientoRepository,
            SeguimientoRespuestaRepository seguimientoRespuestaRepository,
            ProcesoRepository procesoRepository,
            ConciliacionRepository conciliacionRepository,
            FileAssetRepository fileAssetRepository,
            FichaExpedientePdfService fichaExpedientePdfService) {
        this.consultaAccessService = consultaAccessService;
        this.consultaRepository = consultaRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.seguimientoRespuestaRepository = seguimientoRespuestaRepository;
        this.procesoRepository = procesoRepository;
        this.conciliacionRepository = conciliacionRepository;
        this.fileAssetRepository = fileAssetRepository;
        this.fichaExpedientePdfService = fichaExpedientePdfService;
    }

    /**
     * Consolida toda la información vigente del expediente para PB-35.
     * Valida autorización sobre la consulta antes de efectuar cualquier lectura.
     */
    @Transactional(readOnly = true)
    @Auditable(action = "CONSULTAR_FICHA_EXPEDIENTE", entityName = "Consulta", entityId = "#consultaId")
    public FichaExpedienteDTO consolidarFichaExpediente(Long consultaId) {
        if (consultaId == null) {
            throw new BusinessException("La consulta es obligatoria");
        }

        // 1. Validar acceso autorizado antes de leer datos (rechaza acceso ajeno)
        consultaAccessService.validarPuedeVerConsulta(consultaId);

        // 2. Consulta y relaciones de partes
        Consulta consulta = consultaRepository.findByIdConPartes(consultaId)
                .orElseThrow(() -> new BusinessException("Consulta no encontrada con id: " + consultaId));
        consultaRepository.findByIdConContrapartes(consultaId);

        // 3. Seguimientos y respuestas
        List<Seguimiento> seguimientos = seguimientoRepository
                .findByConsulta_IdAndActivoTrueOrderByFechaCreacionDesc(consultaId);

        List<SeguimientoFichaDTO> seguimientosDTO = new ArrayList<>();
        List<Long> seguimientoIds = new ArrayList<>();
        List<Long> respuestaIds = new ArrayList<>();

        for (Seguimiento s : seguimientos) {
            seguimientoIds.add(s.getId());
            List<SeguimientoRespuesta> respuestas = seguimientoRespuestaRepository
                    .findBySeguimiento_IdAndActivoTrueOrderByFechaCreacionDesc(s.getId());

            List<RespuestaFichaDTO> respuestasDTO = new ArrayList<>();
            for (SeguimientoRespuesta r : respuestas) {
                respuestaIds.add(r.getId());
                respuestasDTO.add(RespuestaFichaDTO.builder()
                        .id(r.getId())
                        .fechaCreacion(r.getFechaCreacion())
                        .estado(r.getEstado() != null ? r.getEstado().name() : null)
                        .contenido(r.getContenido())
                        .observacionRevision(r.getObservacionRevision())
                        .estudianteNombre(r.getEstudiante() != null ? r.getEstudiante().getNombre() : null)
                        .build());
            }

            seguimientosDTO.add(SeguimientoFichaDTO.builder()
                    .id(s.getId())
                    .fechaCreacion(s.getFechaCreacion())
                    .fechaEntrega(s.getFechaEntrega())
                    .categoria(s.getCategoriaSeguimiento() != null ? s.getCategoriaSeguimiento().getNombre() : null)
                    .estado(s.getEstado() != null ? s.getEstado().name() : null)
                    .autor(s.getAutor() != null ? s.getAutor().getUsername() : null)
                    .descripcion(s.getDescripcion())
                    .alertaDisciplinaria(s.getAlertaDisciplinaria())
                    .respuestas(respuestasDTO)
                    .build());
        }

        // 4. Procesos judiciales
        List<Proceso> procesos = procesoRepository.findByConsulta_IdAndActivoTrueOrderByIdDesc(consultaId);
        List<ProcesoFichaDTO> procesosDTO = new ArrayList<>();
        List<Long> procesoIds = new ArrayList<>();

        for (Proceso p : procesos) {
            procesoIds.add(p.getId());
            procesosDTO.add(ProcesoFichaDTO.builder()
                    .id(p.getId())
                    .numeroRadicado(p.getNumeroRadicado())
                    .departamento(p.getDepartamento() != null ? p.getDepartamento().getNombre() : null)
                    .organoControl(p.getOrganoControl() != null ? p.getOrganoControl().getNombre() : null)
                    .especialidad(p.getEspecialidad() != null ? p.getEspecialidad().getNombre() : null)
                    .estado(p.getEstado() != null ? p.getEstado().name() : null)
                    .build());
        }

        // 5. Conciliaciones
        List<Conciliacion> conciliaciones = conciliacionRepository.findByConsulta_IdAndActivoTrueOrderByIdDesc(consultaId);
        List<ConciliacionFichaDTO> conciliacionesDTO = new ArrayList<>();
        List<Long> conciliacionIds = new ArrayList<>();

        for (Conciliacion c : conciliaciones) {
            conciliacionIds.add(c.getId());
            conciliacionesDTO.add(ConciliacionFichaDTO.builder()
                    .id(c.getId())
                    .estado(c.getEstado() != null ? c.getEstado().getNombre() : null)
                    .fechaConciliacion(c.getFechaConciliacion())
                    .fechaCreacion(c.getFechaCreacion())
                    .conciliadorNombre(c.getConciliador() != null ? c.getConciliador().getNombre() : null)
                    .estudianteNombre(c.getEstudiante() != null ? c.getEstudiante().getNombre() : null)
                    .build());
        }

        // 6. Índice Documental
        List<DocumentoIndiceDTO> documentosDTO = consolidarIndiceDocumental(
                consultaId, seguimientoIds, respuestaIds, procesoIds, conciliacionIds);

        // 7. Ensamble de secciones
        return FichaExpedienteDTO.builder()
                .identificacion(IdentificacionFichaDTO.builder()
                        .consultaId(consulta.getId())
                        .fechaRadicacion(consulta.getFecha())
                        .tramite(consulta.getTramite())
                        .descripcion(consulta.getDescripcion())
                        .hechos(consulta.getHechos())
                        .pretensiones(consulta.getPretensiones())
                        .conceptoJuridico(consulta.getConceptoJuridico())
                        .observaciones(consulta.getObservaciones())
                        .build())
                .estadoResultado(EstadoResultadoFichaDTO.builder()
                        .estado(consulta.getEstado() != null ? consulta.getEstado().name() : null)
                        .resultado(consulta.getResultado())
                        .ultimaActualizacion(consulta.getLastUpdatedAt())
                        .build())
                .partes(PartesFichaDTO.builder()
                        .solicitantePrincipal(mapearPersona(consulta.getPersona()))
                        .partesAdicionales(consulta.getPartes() != null ? consulta.getPartes().stream().map(this::mapearPersona).toList() : Collections.emptyList())
                        .contrapartes(consulta.getContrapartes() != null ? consulta.getContrapartes().stream().map(this::mapearPersona).toList() : Collections.emptyList())
                        .build())
                .clasificacion(ClasificacionFichaDTO.builder()
                        .sede(consulta.getSede() != null ? consulta.getSede().getNombre() : null)
                        .area(consulta.getArea() != null ? consulta.getArea().getNombre() : null)
                        .tema(consulta.getTema() != null ? consulta.getTema().getNombre() : null)
                        .tipo(consulta.getTipo() != null ? consulta.getTipo().getNombre() : null)
                        .tipoViolencia(consulta.getTipoViolencia())
                        .build())
                .responsables(ResponsablesFichaDTO.builder()
                        .estudiante(mapearEstudiante(consulta.getEstudiante()))
                        .asesor(mapearAsesor(consulta.getAsesor()))
                        .monitor(mapearMonitor(consulta.getMonitor()))
                        .build())
                .seguimientos(seguimientosDTO)
                .procesos(procesosDTO)
                .conciliaciones(conciliacionesDTO)
                .indiceDocumental(documentosDTO)
                .build();
    }

    /**
     * Genera el archivo PDF bajo demanda para PB-35 sin almacenar copia permanente
     * ni exponer rutas del almacenamiento interno.
     */
    @Transactional(readOnly = true)
    @Auditable(action = "GENERAR_FICHA_EXPEDIENTE_PDF", entityName = "Consulta", entityId = "#consultaId")
    public byte[] generarFichaPdf(Long consultaId) {
        FichaExpedienteDTO ficha = consolidarFichaExpediente(consultaId);
        return fichaExpedientePdfService.generarPdf(ficha);
    }

    private List<DocumentoIndiceDTO> consolidarIndiceDocumental(
            Long consultaId,
            List<Long> seguimientoIds,
            List<Long> respuestaIds,
            List<Long> procesoIds,
            List<Long> conciliacionIds) {

        List<FileAsset> assets = new ArrayList<>();

        // Documentos de la consulta
        assets.addAll(fileAssetRepository.findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
                "CONSULTA", consultaId, ESTADOS_DOCUMENTOS_VIGENTES));

        // Documentos de seguimientos
        if (!seguimientoIds.isEmpty()) {
            assets.addAll(fileAssetRepository.findByResourceTypeAndResourceIdInAndStatusInOrderByCreatedAtDesc(
                    "SEGUIMIENTO", seguimientoIds, ESTADOS_DOCUMENTOS_VIGENTES));
        }

        // Documentos de respuestas
        if (!respuestaIds.isEmpty()) {
            assets.addAll(fileAssetRepository.findByResourceTypeAndResourceIdInAndStatusInOrderByCreatedAtDesc(
                    "RESPUESTA", respuestaIds, ESTADOS_DOCUMENTOS_VIGENTES));
        }

        // Documentos de procesos
        if (!procesoIds.isEmpty()) {
            assets.addAll(fileAssetRepository.findByResourceTypeAndResourceIdInAndStatusInOrderByCreatedAtDesc(
                    "PROCESO", procesoIds, ESTADOS_DOCUMENTOS_VIGENTES));
        }

        // Documentos de conciliaciones
        if (!conciliacionIds.isEmpty()) {
            assets.addAll(fileAssetRepository.findByResourceTypeAndResourceIdInAndStatusInOrderByCreatedAtDesc(
                    "CONCILIACION", conciliacionIds, ESTADOS_DOCUMENTOS_VIGENTES));
        }

        // Ordenar por fecha descendente
        assets.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        List<DocumentoIndiceDTO> dtos = new ArrayList<>();
        for (FileAsset a : assets) {
            dtos.add(DocumentoIndiceDTO.builder()
                    .id(a.getId())
                    .nombreArchivo(a.getOriginalFileName())
                    .recursoOrigen(a.getResourceType())
                    .recursoId(a.getResourceId())
                    .contentType(a.getContentType())
                    .tamañoBytes(a.getSize())
                    .tamañoFormateado(formatearTamaño(a.getSize()))
                    .fechaCarga(a.getCreatedAt())
                    .cargadoPor(a.getUploadedBy() != null ? a.getUploadedBy().getUsername() : "Sistema")
                    .build());
        }

        return dtos;
    }

    private PersonaDetalleDTO mapearPersona(Persona p) {
        if (p == null) return null;
        String nombreCompleto = ((p.getNombres() != null ? p.getNombres() : "") + " "
                + (p.getApellidos() != null ? p.getApellidos() : "")).trim();

        return PersonaDetalleDTO.builder()
                .id(p.getId())
                .nombreCompleto(nombreCompleto)
                .tipoDocumento(p.getTipoDocumento())
                .numeroDocumento(p.getNumeroDocumento())
                .telefono(p.getTelefono())
                .correo(p.getCorreo())
                .direccion(p.getDireccion())
                .municipio(p.getMunicipio() != null ? p.getMunicipio().getNombre() : null)
                .barrio(p.getBarrio() != null ? p.getBarrio().getNombre() : null)
                .estrato(p.getEstrato())
                .build();
    }

    private ResponsableDetalleDTO mapearEstudiante(Estudiante e) {
        if (e == null) return null;
        return ResponsableDetalleDTO.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .documento(e.getDocumento())
                .email(e.getEmail())
                .telefono(e.getTelefono())
                .codigo(e.getCodigo())
                .build();
    }

    private ResponsableDetalleDTO mapearAsesor(Asesor a) {
        if (a == null) return null;
        return ResponsableDetalleDTO.builder()
                .id(a.getId())
                .nombre(a.getNombre())
                .documento(a.getDocumento())
                .email(a.getEmail())
                .telefono(a.getTelefono())
                .build();
    }

    private ResponsableDetalleDTO mapearMonitor(Monitor m) {
        if (m == null) return null;
        return ResponsableDetalleDTO.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .documento(m.getDocumento())
                .email(m.getEmail())
                .telefono(m.getTelefono())
                .build();
    }

    private String formatearTamaño(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
