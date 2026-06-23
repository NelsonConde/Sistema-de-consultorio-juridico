package co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion;

import lombok.AllArgsConstructor;
import lombok.Getter;

// dto interno para enviar datos del destinatario de la notificacion 
@Getter
@AllArgsConstructor
public class SeguimientoDestinatarioDTO {

    private String email;

    private String nombre;
}