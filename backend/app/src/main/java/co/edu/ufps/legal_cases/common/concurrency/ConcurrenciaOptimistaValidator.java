package co.edu.ufps.legal_cases.common.concurrency;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ConcurrenciaOptimistaException;

@Component
public class ConcurrenciaOptimistaValidator {

    public void validarVersionNoEnviadaEnCreacion(Long version) {
        if (version != null) {
            throw new BusinessException(
                    "La versión no debe enviarse en la creación");
        }
    }

    public void validarVersion(
            Long versionEsperada,
            Long versionActual,
            String nombreRecurso) {

        if (versionEsperada == null) {
            throw new BusinessException(
                    "La versión del recurso es obligatoria para actualizar");
        }

        if (versionActual == null) {
            throw new BusinessException(
                    "El recurso no tiene una versión de concurrencia configurada");
        }

        if (!Objects.equals(versionEsperada, versionActual)) {
            throw new ConcurrenciaOptimistaException(
                    construirMensajeConflicto(nombreRecurso));
        }
    }

    private String construirMensajeConflicto(String nombreRecurso) {
        String recurso = nombreRecurso == null || nombreRecurso.isBlank()
                ? "recurso"
                : nombreRecurso.trim();

        return "La " + recurso
                + " fue modificada por otro usuario. "
                + "Recargue la información y revise sus cambios.";
    }
}