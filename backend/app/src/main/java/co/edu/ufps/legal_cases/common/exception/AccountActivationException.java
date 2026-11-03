package co.edu.ufps.legal_cases.common.exception;

public class AccountActivationException
        extends BusinessException {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        INVALIDO,
        REVOCADO,
        USADO,
        VENCIDO
    }

    private final Motivo motivo;

    private AccountActivationException(
            Motivo motivo,
            String mensaje) {

        super(mensaje);
        this.motivo = motivo;
    }

    public static AccountActivationException invalido() {
        return new AccountActivationException(
                Motivo.INVALIDO,
                "El enlace de activación no es válido");
    }

    public static AccountActivationException revocado() {
        return new AccountActivationException(
                Motivo.REVOCADO,
                "El enlace de activación ya no está disponible");
    }

    public static AccountActivationException usado() {
        return new AccountActivationException(
                Motivo.USADO,
                "El enlace de activación ya fue utilizado");
    }

    public static AccountActivationException vencido() {
        return new AccountActivationException(
                Motivo.VENCIDO,
                "El enlace de activación ha vencido");
    }

    public Motivo getMotivo() {
        return motivo;
    }

    public String getCodigo() {
        return "TOKEN_ACTIVACION_" + motivo.name();
    }
}