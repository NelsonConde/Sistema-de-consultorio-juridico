const { test, expect } = require("@playwright/test");

const ACTIVATION_TOKEN = "synthetic-activation-token";
const PASSWORD = "FixtureActivation1";
const DIFFERENT_PASSWORD = "FixtureActivation2";

function json(route, body, status = 200, headers = {}) {
  return route.fulfill({
    status,
    headers,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function mockActivationApi(
  page,
  {
    activationStatus = 200,
    activationBody = { mensaje: "Cuenta activada" },
    activationHeaders = {},
    responseDelay = 0,
  } = {},
) {
  const state = {
    csrfRequests: 0,
    activationRequests: [],
  };

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;

    if (path.endsWith("/api/auth/csrf")) {
      state.csrfRequests += 1;
      await json(route, {
        headerName: "X-CSRF-TOKEN",
        token: "synthetic-csrf-token",
      });
      return;
    }

    if (
      path.endsWith("/api/auth/activar-cuenta") &&
      request.method() === "POST"
    ) {
      state.activationRequests.push({
        body: request.postDataJSON(),
        csrfHeader: request.headers()["x-csrf-token"] ?? null,
      });

      if (responseDelay > 0) {
        await new Promise((resolve) => setTimeout(resolve, responseDelay));
      }

      await json(route, activationBody, activationStatus, activationHeaders);
      return;
    }

    await json(route, {}, 404);
  });

  return state;
}

async function fillActivationForm(page, confirmation = PASSWORD) {
  await page
      .getByLabel("Nueva contraseña", { exact: true })
      .fill(PASSWORD);

  await page
      .getByLabel("Confirmar nueva contraseña", { exact: true })
      .fill(confirmation);
}

async function expectNoSecretsInStorage(page) {
  const storage = await page.evaluate(() =>
    JSON.stringify({
      localStorage: { ...window.localStorage },
      sessionStorage: { ...window.sessionStorage },
    }),
  );

  expect(storage).not.toContain(ACTIVATION_TOKEN);
  expect(storage).not.toContain(PASSWORD);
  expect(storage).not.toContain(DIFFERENT_PASSWORD);
}

test.describe("SCRUM-295: activación pública de cuenta", () => {
  test("retira el token de la URL y conserva los secretos únicamente en memoria", async ({
    page,
  }) => {
    await mockActivationApi(page);

    await page.goto(`/activar-cuenta?token=${ACTIVATION_TOKEN}`);

    await expect(page).toHaveURL(/\/activar-cuenta$/);
    await expect(
        page.getByRole("button", { name: "Activar cuenta", exact: true }),
    ).toBeVisible();
    await expectNoSecretsInStorage(page);
  });

  test("valida la política y la confirmación antes de iniciar una petición", async ({
    page,
  }) => {
    const api = await mockActivationApi(page);
    await page.goto(`/activar-cuenta?token=${ACTIVATION_TOKEN}`);
    await fillActivationForm(page, DIFFERENT_PASSWORD);

    await page.getByRole("button", { name: "Activar cuenta" }).click();

    await expect(page.getByText("Las contraseñas no coinciden")).toBeVisible();
    expect(api.csrfRequests).toBe(0);
    expect(api.activationRequests).toHaveLength(0);
    await expectNoSecretsInStorage(page);
  });

  test("bloquea el doble envío y transmite solo token, contraseña y CSRF", async ({
    page,
  }) => {
    const consoleMessages = [];
    page.on("console", (message) => consoleMessages.push(message.text()));

    const api = await mockActivationApi(page, { responseDelay: 150 });
    await page.goto(`/activar-cuenta?token=${ACTIVATION_TOKEN}`);
    await fillActivationForm(page);

    await page
      .getByRole("button", { name: "Activar cuenta" })
      .click({ clickCount: 2 });

    await expect.poll(() => api.activationRequests.length).toBe(1);
    expect(api.activationRequests[0]).toEqual({
      body: {
        token: ACTIVATION_TOKEN,
        passwordNueva: PASSWORD,
      },
      csrfHeader: "synthetic-csrf-token",
    });
    await expect(
        page.getByText("Cuenta activada", { exact: true })    ).toBeVisible();
    await expect(
      page.getByRole("link", { name: "Ir al inicio de sesión" }),
    ).toHaveAttribute("href", "/");
    await expectNoSecretsInStorage(page);

    const consoleOutput = consoleMessages.join("\n");
    expect(consoleOutput).not.toContain(ACTIVATION_TOKEN);
    expect(consoleOutput).not.toContain(PASSWORD);
  });

  for (const scenario of [
    {
      name: "vencido",
      status: 410,
      body: { codigo: "TOKEN_ACTIVACION_VENCIDO" },
      heading: "El enlace venció",
    },
    {
      name: "usado",
      status: 409,
      body: { codigo: "TOKEN_ACTIVACION_USADO" },
      heading: "El enlace ya fue utilizado",
    },
    {
      name: "revocado",
      status: 403,
      body: { codigo: "TOKEN_ACTIVACION_REVOCADO" },
      heading: "El enlace ya no está disponible",
    },
  ]) {
    test(`presenta el estado ${scenario.name} sin enumerar la cuenta`, async ({
      page,
    }) => {
      await mockActivationApi(page, {
        activationStatus: scenario.status,
        activationBody: scenario.body,
      });
      await page.goto(`/activar-cuenta?token=${ACTIVATION_TOKEN}`);
      await fillActivationForm(page);

      await page.getByRole("button", { name: "Activar cuenta" }).click();

      await expect(
          page.getByText(scenario.heading, { exact: true })
      ).toBeVisible();
      await expect(page.locator("body")).not.toContainText(ACTIVATION_TOKEN);
      await expectNoSecretsInStorage(page);
    });
  }

  test("mantiene los campos para un reintento explícito ante error de red", async ({
    page,
  }) => {
    await page.route("**/api/auth/csrf", (route) => route.abort("failed"));
    await page.goto(`/activar-cuenta?token=${ACTIVATION_TOKEN}`);
    await fillActivationForm(page);

    await page.getByRole("button", { name: "Activar cuenta" }).click();

    await expect(
        page.getByRole("alert").filter({
          hasText: "No pudimos completar la activación",
        }),
    ).toBeVisible();
    await expect(
        page.getByLabel("Nueva contraseña", { exact: true }),
    ).toHaveValue(PASSWORD);

    await expect(
        page.getByLabel("Confirmar nueva contraseña", { exact: true }),
    ).toHaveValue(PASSWORD);
    await expectNoSecretsInStorage(page);
  });

  test("muestra un estado controlado cuando falta el token", async ({
    page,
  }) => {
    await page.goto("/activar-cuenta");

    await expect(
        page.getByText("El enlace no es válido", { exact: true })
    ).toBeVisible();
    await expect(
        page.getByRole("button", { name: "Activar cuenta", exact: true })
    ).toHaveCount(0);
  });

  test("no genera desplazamiento horizontal en los viewports requeridos", async ({
    page,
  }) => {
    await mockActivationApi(page);

    for (const viewport of [
      { width: 360, height: 800 },
      { width: 768, height: 900 },
      { width: 1280, height: 900 },
    ]) {
      await page.setViewportSize(viewport);
      await page.goto(`/activar-cuenta?token=${ACTIVATION_TOKEN}`);
      await expect(
          page.getByRole("button", { name: "Activar cuenta", exact: true }),
      ).toBeVisible();

      const dimensions = await page.evaluate(() => ({
        clientWidth: document.documentElement.clientWidth,
        scrollWidth: document.documentElement.scrollWidth,
      }));
      expect(dimensions.scrollWidth).toBeLessThanOrEqual(
        dimensions.clientWidth,
      );
    }
  });
});
