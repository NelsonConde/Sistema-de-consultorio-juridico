package co.edu.ufps.legal_cases.business.model.seguimiento.notificacion;

public enum MomentoNotificacionSeguimiento {

    // Se envia cuando se crea o actualiza el seguimiento.
    INMEDIATA,

    // Se envia segun fechaEntrega - diasNotificacion.
    RECORDATORIO
}