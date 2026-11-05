package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaReasignacionHistorialDTO;
import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaReasignacionHistorialRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaReasignacionHistorialResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class ConsultaReasignacionHistorialQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;

    private final ConsultaReasignacionHistorialRepository historialRepository;
    private final ConsultaAccessService consultaAccessService;

    public ConsultaReasignacionHistorialQueryService(
            ConsultaReasignacionHistorialRepository historialRepository,
            ConsultaAccessService consultaAccessService) {
        this.historialRepository = historialRepository;
        this.consultaAccessService = consultaAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ConsultaReasignacionHistorialDTO> listar(
            Long consultaId,
            int page,
            int size) {
        consultaAccessService.validarPuedeVerConsulta(consultaId);
        validarPaginacion(page, size);

        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<ConsultaReasignacionHistorialResumenProjection> resultado =
                historialRepository.buscarResumenPorConsultaId(consultaId, pageable);

        List<ConsultaReasignacionHistorialDTO> contenido = resultado.getContent()
                .stream()
                .map(this::convertir)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    private ConsultaReasignacionHistorialDTO convertir(
            ConsultaReasignacionHistorialResumenProjection evento) {
        return new ConsultaReasignacionHistorialDTO(
                evento.getId(),
                TipoResponsableConsulta.valueOf(evento.getTipoResponsable()),
                evento.getResponsableAnteriorNombre(),
                evento.getResponsableNuevoNombre(),
                evento.getMotivo(),
                evento.getActorNombre(),
                evento.getOcurridoEn());
    }

    private void validarPaginacion(int page, int size) {
        if (page < PAGINA_MINIMA) {
            throw new BusinessException("La página debe ser mayor o igual a 1");
        }

        if (size < TAMANO_MINIMO || size > TAMANO_MAXIMO) {
            throw new BusinessException("El tamaño de página debe estar entre 1 y 50");
        }
    }
}
