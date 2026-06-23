package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacionHistorial;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.TipoEventoReunionConciliacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionHistorialRepository;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ReunionConciliacionHistorialService {

    private final ReunionConciliacionHistorialRepository reunionConciliacionHistorialRepository;

    public void registrarProgramacion(ReunionConciliacion reunion, UsuarioSistema usuario) {
        ReunionConciliacionHistorial historial = new ReunionConciliacionHistorial();
        historial.setConciliacion(reunion.getConciliacion());
        historial.setTipoEvento(TipoEventoReunionConciliacion.PROGRAMACION);
        historial.setFechaReunionNueva(reunion.getFechaReunion());
        historial.setSedeNueva(reunion.getSede());
        historial.setObservacionesNuevas(reunion.getObservaciones());
        historial.setRealizadoPor(usuario);
        historial.setFechaEvento(LocalDateTime.now());

        reunionConciliacionHistorialRepository.save(historial);
    }

    public void registrarReprogramacion(
            ReunionConciliacion reunion,
            LocalDateTime fechaAnterior,
            Sede sedeAnterior,
            String observacionesAnteriores,
            UsuarioSistema usuario) {
        ReunionConciliacionHistorial historial = new ReunionConciliacionHistorial();
        historial.setConciliacion(reunion.getConciliacion());
        historial.setTipoEvento(TipoEventoReunionConciliacion.REPROGRAMACION);
        historial.setFechaReunionAnterior(fechaAnterior);
        historial.setFechaReunionNueva(reunion.getFechaReunion());
        historial.setSedeAnterior(sedeAnterior);
        historial.setSedeNueva(reunion.getSede());
        historial.setObservacionesAnteriores(observacionesAnteriores);
        historial.setObservacionesNuevas(reunion.getObservaciones());
        historial.setRealizadoPor(usuario);
        historial.setFechaEvento(LocalDateTime.now());

        reunionConciliacionHistorialRepository.save(historial);
    }
}
