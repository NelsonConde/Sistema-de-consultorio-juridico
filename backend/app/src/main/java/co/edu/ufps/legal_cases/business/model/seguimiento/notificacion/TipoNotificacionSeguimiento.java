package co.edu.ufps.legal_cases.business.model.seguimiento.notificacion;

public enum TipoNotificacionSeguimiento {

    // Para persona principal, partes y contrapartes.
    PARTES,

    // Para el estudiante asociado a la consulta.
    ESTUDIANTE,

    // Para notificar a administrativos.
    ALERTA_DISCIPLINARIA,

    // Para recordar a la persona que creo el seguimiento.
    AUTOR
}