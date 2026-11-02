import { getResponseCorrelationId, readResponseBody } from "@/lib/api";
import { apiClient } from "@/lib/apiClient";

const ACTIVATION_ENDPOINT = "/auth/activar-cuenta";

export const ACTIVATION_FAILURE = Object.freeze({
  EXPIRED: "expired",
  INVALID: "invalid",
  REVOKED: "revoked",
  UNAVAILABLE: "unavailable",
  USED: "used",
});

export class AccountActivationError extends Error {
  constructor(reason, correlationId = null) {
    super("No fue posible activar la cuenta");
    this.name = "AccountActivationError";
    this.reason = reason;
    this.correlationId = correlationId;
  }
}

function getFunctionalCode(payload) {
  if (!payload || typeof payload !== "object") return "";

  return String(
    payload.detalles?.codigo ??
      payload.details?.code ??
      payload.codigo ??
      payload.code ??
      "",
  ).toUpperCase();
}

function codeContains(code, fragments) {
  return fragments.some((fragment) => code.includes(fragment));
}

function resolveFailureReason(status, payload) {
  const code = getFunctionalCode(payload);

  if (codeContains(code, ["VENCID", "EXPIRED"])) {
    return ACTIVATION_FAILURE.EXPIRED;
  }

  if (codeContains(code, ["REVOC", "REVOKED"])) {
    return ACTIVATION_FAILURE.REVOKED;
  }

  if (codeContains(code, ["USADO", "UTILIZADO", "USED", "CONSUMED"])) {
    return ACTIVATION_FAILURE.USED;
  }

  if (status === 410) return ACTIVATION_FAILURE.EXPIRED;
  if (status === 409) return ACTIVATION_FAILURE.USED;
  if ([400, 404, 422].includes(status)) return ACTIVATION_FAILURE.INVALID;

  return ACTIVATION_FAILURE.UNAVAILABLE;
}

/**
 * Activa una cuenta sin enviar la confirmación de contraseña, que solo existe
 * para validar la entrada en el navegador.
 */
export async function activateAccount({ token, password }) {
  let response;

  try {
    response = await apiClient.post(ACTIVATION_ENDPOINT, {
      token,
      passwordNueva: password,
    });
  } catch {
    throw new AccountActivationError(ACTIVATION_FAILURE.UNAVAILABLE);
  }

  const payload = await readResponseBody(response);

  if (response.ok) return;

  throw new AccountActivationError(
    resolveFailureReason(response.status, payload),
    getResponseCorrelationId(response, payload),
  );
}
