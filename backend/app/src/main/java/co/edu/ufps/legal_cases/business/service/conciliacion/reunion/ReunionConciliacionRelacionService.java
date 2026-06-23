package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacionCodigo;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion.ConciliacionRelacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import lombok.AllArgsConstructor;

// Centraliza lookups usados por la HU de reunión de conciliación.
@Service
@AllArgsConstructor
public class ReunionConciliacionRelacionService {

    private final SedeRepository sedeRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final ReunionConciliacionRepository reunionConciliacionRepository;
    private final ConciliacionRelacionService conciliacionRelacionService;

    public Conciliacion obtenerConciliacionActiva(Long id) {
        return conciliacionRelacionService.obtenerConciliacionActiva(id);
    }

    public Sede obtenerSedeActiva(Long id) {
        if (id == null) {
            throw new BusinessException("La sede de la reunión es obligatoria");
        }

        return sedeRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Sede no encontrada o inactiva con id: " + id));
    }

    public ReunionConciliacion obtenerReunion(Long conciliacionId) {
        if (conciliacionId == null) {
            throw new BusinessException("La conciliación es obligatoria");
        }

        return reunionConciliacionRepository.findByConciliacion_Id(conciliacionId)
                .orElseThrow(() -> new BusinessException(
                        "La conciliación no tiene reunión programada para reprogramar"));
    }

    public EstadoConciliacion obtenerEstadoReunionProgramada() {
        return conciliacionRelacionService.obtenerEstadoActivoPorCodigo(
                EstadoConciliacionCodigo.REUNION_PROGRAMADA);
    }

    public UsuarioSistema obtenerUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new BusinessException("El usuario es obligatorio");
        }

        return usuarioSistemaRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado con id: " + usuarioId));
    }
}
