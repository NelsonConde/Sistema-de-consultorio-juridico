package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

@Service
public class ConsultaQueryService {

    private final ConsultaRepository consultaRepository;
    private final ConsultaAccessService consultaAccessService;
    private final ConsultaMapper consultaMapper;

    public ConsultaQueryService(
            ConsultaRepository consultaRepository,
            ConsultaAccessService consultaAccessService,
            ConsultaMapper consultaMapper) {
        this.consultaRepository = consultaRepository;
        this.consultaAccessService = consultaAccessService;
        this.consultaMapper = consultaMapper;
    }

    // Busca consultas por texto libre y devuelve solo las que pertenecen al alcance del usuario actual.
    @Transactional(readOnly = true)
    public List<ConsultaBusquedaDTO> buscarParaUsuarioActual(String search) {
        consultaAccessService.validarPuedeBuscarConsultas();

        String termino = normalizarTexto(search);
        PerfilUsuarioActual perfil = consultaAccessService.obtenerPerfilActual();

        if (consultaAccessService.usuarioEsAdministrador()) {
            return consultaRepository.buscarParaAdministrador(termino)
                    .stream()
                    .map(consultaMapper::convertirABusquedaDTO)
                    .toList();
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ESTUDIANTE) {
            return consultaRepository.buscarParaEstudiante(termino, perfil.getPerfilId())
                    .stream()
                    .map(consultaMapper::convertirABusquedaDTO)
                    .toList();
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ASESOR) {
            return consultaRepository.buscarParaAsesor(termino, perfil.getPerfilId())
                    .stream()
                    .map(consultaMapper::convertirABusquedaDTO)
                    .toList();
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.MONITOR) {
            return consultaRepository.buscarParaMonitor(termino, perfil.getPerfilId())
                    .stream()
                    .map(consultaMapper::convertirABusquedaDTO)
                    .toList();
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.CONCILIADOR) {
            // Cuando conciliaciones tenga alcance real, aquí se listarán las consultas asociadas.
            return List.of();
        }

        return List.of();
    }

    // Se conserva temporalmente para compatibilidad interna si alguna clase todavía lo llama.
    @Transactional(readOnly = true)
    public List<ConsultaBusquedaDTO> buscar(String search) {
        return buscarParaUsuarioActual(search);
    }

    @Transactional(readOnly = true)
    public List<ConsultaDTO> listar() {
        consultaAccessService.validarPuedeBuscarConsultas();

        return consultaRepository.findAll()
                .stream()
                .filter(consultaAccessService::puedeAccederAConsulta)
                .map(consultaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultaDTO obtenerPorId(Long id) {
        consultaAccessService.validarPuedeVerConsulta(id);

        Consulta consulta = consultaRepository.findByIdConPartes(id)
                .orElseThrow(() -> new BusinessException("Consulta no encontrada con id: " + id));

        // Esta consulta carga la colección en el contexto para que el mapper tenga contrapartes disponibles.
        consultaRepository.findByIdConContrapartes(id);

        return consultaMapper.convertirADTO(consulta);
    }

    @Transactional(readOnly = true)
    public List<ConsultaBusquedaDTO> listarArchivadas() {
        consultaAccessService.validarPuedeListarConsultasArchivadas();

        return consultaRepository.findByEstado(EstadoConsulta.ARCHIVADO)
                .stream()
                .map(consultaMapper::convertirABusquedaDTO)
                .toList();
    }
}