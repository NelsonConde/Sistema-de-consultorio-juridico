package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.equalsIgnoreCase;
import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.mismoId;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoRequestDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class SeguimientoValidator {

    public void validarCreacion(SeguimientoRequestDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, SeguimientoRequestDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del seguimiento");
        }
    }

    public String normalizarDescripcion(String descripcion) {
        String descripcionNormalizada = normalizarTexto(descripcion);

        if (descripcionNormalizada == null || descripcionNormalizada.isBlank()) {
            throw new BusinessException("La descripción del seguimiento es obligatoria");
        }

        if (descripcionNormalizada.length() > 200) {
            throw new BusinessException("La descripción del seguimiento no puede superar 200 caracteres");
        }

        return descripcionNormalizada;
    }

    public void validarDatosSeguimiento(
            LocalDate fechaEntrega,
            Integer diasNotificacion) {

        if (fechaEntrega != null && fechaEntrega.isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha de entrega no puede ser anterior a la fecha actual");
        }

        if (diasNotificacion != null && diasNotificacion < 0) {
            throw new BusinessException("Los días de notificación no pueden ser negativos");
        }

        if (diasNotificacion != null && fechaEntrega == null) {
            throw new BusinessException("No se pueden definir días de notificación sin fecha de entrega");
        }
    }

    public void validarNoCambieConsulta(Seguimiento seguimiento, SeguimientoRequestDTO dto) {
        if (dto.getConsultaId() == null) {
            throw new BusinessException("La consulta es obligatoria");
        }

        Long consultaActualId = seguimiento.getConsulta() != null
                ? seguimiento.getConsulta().getId()
                : null;

        // La consulta define el contexto del seguimiento, por eso no se mueve en edición.
        if (!Objects.equals(consultaActualId, dto.getConsultaId())) {
            throw new BusinessException("No se permite cambiar la consulta de un seguimiento existente");
        }
    }

    public void validarExistenCambios(Seguimiento seguimiento, DatosSeguimiento datos) {
        if (sinCambios(seguimiento, datos)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarFechaEntregaObligatoria(LocalDate fechaEntrega) {
        if (fechaEntrega == null) {
            throw new BusinessException("La fecha de entrega es obligatoria");
        }
    }

    public void validarSeguimientoEditable(Seguimiento seguimiento) {
        if (!EstadoSeguimiento.PENDIENTE.equals(seguimiento.getEstado())) {
            throw new BusinessException("Solo se pueden editar seguimientos pendientes");
        }
    }

    public void validarCambioEstadoSeguimiento(Seguimiento seguimiento, EstadoSeguimiento estado) {
        if (estado == null) {
            throw new BusinessException("El estado del seguimiento es obligatorio");
        }

        if (Objects.equals(seguimiento.getEstado(), estado)) {
            throw new BusinessException("El seguimiento ya tiene ese estado");
        }
    }

    public void validarNotificarEstudianteConConsulta(
            Boolean notificarEstudiante,
            Consulta consulta) {

        if (!Boolean.TRUE.equals(notificarEstudiante)) {
            return;
        }

        if (consulta == null) {
            throw new BusinessException("No se puede notificar al estudiante porque la consulta es obligatoria");
        }

        if (consulta.getEstudiante() == null) {
            throw new BusinessException(
                    "No se puede notificar al estudiante porque la consulta no tiene estudiante asignado");
        }

        if (!Boolean.TRUE.equals(consulta.getEstudiante().getActivo())) {
            throw new BusinessException(
                    "No se puede notificar al estudiante porque el estudiante asignado está inactivo");
        }
    }

    private void validarDtoObligatorio(SeguimientoRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del seguimiento son obligatorios");
        }
    }

    private boolean sinCambios(Seguimiento seguimiento, DatosSeguimiento datos) {
        return equalsIgnoreCase(seguimiento.getDescripcion(), datos.descripcion())
                && Objects.equals(seguimiento.getFechaEntrega(), datos.fechaEntrega())
                && Objects.equals(seguimiento.getDiasNotificacion(), datos.diasNotificacion())
                && Objects.equals(seguimiento.getNotificarPartes(), datos.notificarPartes())
                && Objects.equals(seguimiento.getNotificarEstudiante(), datos.notificarEstudiante())
                && Objects.equals(seguimiento.getAlertaDisciplinaria(), datos.alertaDisciplinaria())
                && mismoId(seguimiento.getCategoriaSeguimiento(), datos.categoria(), CategoriaSeguimiento::getId)
                && mismoId(seguimiento.getConsulta(), datos.consulta(), Consulta::getId);
    }
}