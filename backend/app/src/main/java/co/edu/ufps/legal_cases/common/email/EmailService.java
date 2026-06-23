package co.edu.ufps.legal_cases.common.email;

// Contrato común para envío de correos del sistema.
// Los módulos de negocio dependen de esta interfaz y no del proveedor externo.
public interface EmailService {

    void enviarHtml(
            String destinatario,
            String nombreDestinatario,
            String asunto,
            String html,
            String tipoCorreo);
}