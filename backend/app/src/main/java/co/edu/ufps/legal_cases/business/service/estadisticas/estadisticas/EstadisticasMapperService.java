package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.estadisticas.ConteoDTO;

// Convierte resultados de queries nativas (Object[]) a ConteoDTO.
// Centraliza el mapeo para que los QueryServices no repitan lógica.
@Service
public class EstadisticasMapperService {

    // Para queries con 2 columnas: [nombre, cantidad].
    public List<ConteoDTO> mapear2(List<Object[]> filas) {
        List<ConteoDTO> resultado = new ArrayList<>();
        if (filas == null) return resultado;
        for (Object[] fila : filas) {
            String nombre = fila[0] != null ? fila[0].toString() : "Sin nombre";
            long cantidad = fila[1] != null ? ((Number) fila[1]).longValue() : 0L;
            resultado.add(new ConteoDTO(nombre, cantidad));
        }
        return resultado;
    }

    // Para queries con 3 columnas: [id, nombre, cantidad].
    public List<ConteoDTO> mapear3(List<Object[]> filas) {
        List<ConteoDTO> resultado = new ArrayList<>();
        if (filas == null) return resultado;
        for (Object[] fila : filas) {
            String nombre = fila[1] != null ? fila[1].toString() : "Sin nombre";
            long cantidad = fila[2] != null ? ((Number) fila[2]).longValue() : 0L;
            resultado.add(new ConteoDTO(nombre, cantidad));
        }
        return resultado;
    }

    // Extrae un conteo escalar de una query que retorna una sola fila con una columna.
    public long extraerEscalar(List<Object[]> resultado) {
        if (resultado == null || resultado.isEmpty()) return 0L;
        Object[] fila = resultado.get(0);
        return fila != null && fila[0] != null ? ((Number) fila[0]).longValue() : 0L;
    }

    // Extrae finalizadas y pendientes de la query de conteo de consultas.
    public long[] extraerFinalizadasYPendientes(List<Object[]> resultado) {
        Object[] fila = (resultado != null && !resultado.isEmpty()) ? resultado.get(0) : null;
        long finalizadas = fila != null && fila[0] != null ? ((Number) fila[0]).longValue() : 0L;
        long pendientes = fila != null && fila[1] != null ? ((Number) fila[1]).longValue() : 0L;
        return new long[]{finalizadas, pendientes};
    }
}