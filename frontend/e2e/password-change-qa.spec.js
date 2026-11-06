const { test, expect } = require("@playwright/test");

const CURRENT_PASSWORD = "FixtureCurrent1";
const NEW_PASSWORD = "FixtureUpdated2";
const DIFFERENT_CONFIRMATION = "FixtureMismatch3";

function profileFixture() {
  return {
    username: "qa.frontend",
    rolNombre: "Rol de prueba",
    tipoPerfil: "ADMINISTRATIVO",
    nombre: "Usuario QA",
    email: "qa.frontend@prueba.local",
    telefono: "+57 300 000 0000",
    sede: "Sede Central",
    codigo: "QA-FE-365",
  };
}

async function mockPasswordChangeApi(page, options = {}) {
  const {
    csrfStatus = 200,
    passwordStatus = 204,
    passwordBody = { mensaje: "Contraseña actualizada" },
  } = options;
  const state = {
    csrfRequests: 0,
    passwordRequests: [],
  };

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;

    if (path.endsWith("/api/auth/me")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: 1,
          tipoPerfil: "ADMINISTRATIVO",
          permisos: [],
        }),
      });
      return;
    }

    if (path.endsWith("/api/mi-perfil")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(profileFixture()),
      });
      return;
    }

    if (path.endsWith("/api/auth/csrf")) {
      state.csrfRequests += 1;
      await route.fulfill({
        status: csrfStatus,
        contentType: "application/json",
        body: JSON.stringify(
          csrfStatus === 200
            ? { headerName: "X-CSRF-TOKEN", token: "synthetic-csrf-token" }
            : { mensaje: "No fue posible validar la solicitud" },
        ),
      });
      return;
    }

    if (
      path.endsWith("/api/auth/cambiar-password") &&
      request.method() === "PATCH"
    ) {
      state.passwordRequests.push({
        body: request.postDataJSON(),
        csrfHeader: request.headers()["x-csrf-token"] ?? null,
      });
      if (passwordStatus === 204) {
        await route.fulfill({ status: 204 });
      } else {
        await route.fulfill({
          status: passwordStatus,
          contentType: "application/json",
          body: JSON.stringify(passwordBody),
        });
      }
      return;
    }

    await route.fulfill({
      status: 404,
      contentType: "application/json",
      body: "{}",
    });
  });

  return state;
}

async function openSecurityTab(page) {
  await page.goto("/mi-perfil");
  await page.getByRole("tab", { name: "Seguridad" }).click();
  await expect(
    page.getByRole("heading", { name: "Cambiar contraseña" }),
  ).toBeVisible();
}

async function fillPasswordForm(page, confirmation = NEW_PASSWORD) {
  await page.locator('input[name="passwordActual"]').fill(CURRENT_PASSWORD);
  await page.locator('input[name="passwordNueva"]').fill(NEW_PASSWORD);
  await page.locator('input[name="confirmarPassword"]').fill(confirmation);
}

async function expectNoSecretsInBrowserStorage(page) {
  const storedValues = await page.evaluate(() =>
    JSON.stringify({
      localStorage: { ...window.localStorage },
      sessionStorage: { ...window.sessionStorage },
    }),
  );

  expect(storedValues).not.toContain(CURRENT_PASSWORD);
  expect(storedValues).not.toContain(NEW_PASSWORD);
  expect(storedValues).not.toContain(DIFFERENT_CONFIRMATION);
}

test.describe("SCRUM-365: QA frontend de cambio de contraseña", () => {
  test("valida la confirmación localmente y no envía secretos adicionales", async ({
    page,
  }) => {
    const api = await mockPasswordChangeApi(page);
    await openSecurityTab(page);
    await fillPasswordForm(page, DIFFERENT_CONFIRMATION);

    await page.getByRole("button", { name: "Cambiar contraseña" }).click();

    await expect(page.getByText("Las contraseñas no coinciden")).toBeVisible();
    expect(api.csrfRequests).toBe(0);
    expect(api.passwordRequests).toHaveLength(0);
    await expectNoSecretsInBrowserStorage(page);
  });

  test("permite mostrar y ocultar los campos mediante controles accesibles", async ({
    page,
  }) => {
    await mockPasswordChangeApi(page);
    await openSecurityTab(page);

    const currentPassword = page.locator('input[name="passwordActual"]');
    const toggle = page.getByRole("button", {
      name: "Mostrar contraseña actual",
    });

    await expect(currentPassword).toHaveAttribute("type", "password");
    await toggle.focus();
    await page.keyboard.press("Enter");
    await expect(currentPassword).toHaveAttribute("type", "text");
    await expect(
      page.getByRole("button", { name: "Ocultar contraseña actual" }),
    ).toBeFocused();

    await page.keyboard.press("Enter");
    await expect(currentPassword).toHaveAttribute("type", "password");
  });

  test("envía solo el payload permitido con CSRF y limpia secretos tras éxito", async ({
    page,
  }) => {
    const consoleMessages = [];
    page.on("console", (message) => consoleMessages.push(message.text()));

    const api = await mockPasswordChangeApi(page);
    await openSecurityTab(page);
    await fillPasswordForm(page);

    await page.getByRole("button", { name: "Cambiar contraseña" }).click();

    await expect.poll(() => api.passwordRequests.length).toBe(1);
    expect(api.passwordRequests[0]).toEqual({
      body: {
        passwordActual: CURRENT_PASSWORD,
        passwordNueva: NEW_PASSWORD,
      },
      csrfHeader: "synthetic-csrf-token",
    });
    await expect(
      page.getByText("La contraseña se cambió correctamente."),
    ).toBeVisible();
    await expect(page.locator('input[name="passwordActual"]')).toHaveValue("");
    await expect(page.locator('input[name="passwordNueva"]')).toHaveValue("");
    await expect(page.locator('input[name="confirmarPassword"]')).toHaveValue("");
    await expectNoSecretsInBrowserStorage(page);

    const consoleOutput = consoleMessages.join("\n");
    expect(consoleOutput).not.toContain(CURRENT_PASSWORD);
    expect(consoleOutput).not.toContain(NEW_PASSWORD);
  });

  test("presenta el rechazo funcional sin limpiar los campos ni revelar detalles internos", async ({
    page,
  }) => {
    const api = await mockPasswordChangeApi(page, {
      passwordStatus: 400,
      passwordBody: {
        mensaje:
          "No fue posible cambiar la contraseña con los datos suministrados",
      },
    });
    await openSecurityTab(page);
    await fillPasswordForm(page);

    await page.getByRole("button", { name: "Cambiar contraseña" }).click();

    await expect.poll(() => api.passwordRequests.length).toBe(1);
    await expect(
      page.getByRole("alert").filter({ hasText: "No fue posible cambiar" }),
    ).toBeVisible();
    await expect(page.locator('input[name="passwordActual"]')).toHaveValue(
      CURRENT_PASSWORD,
    );
    await expect(page.locator('input[name="passwordNueva"]')).toHaveValue(NEW_PASSWORD);
    await expectNoSecretsInBrowserStorage(page);
  });

  test("maneja 401 con una sola petición y redirección controlada", async ({
    page,
  }) => {
    const api = await mockPasswordChangeApi(page, {
      passwordStatus: 401,
      passwordBody: { mensaje: "La sesión ya no es válida" },
    });
    await openSecurityTab(page);
    await fillPasswordForm(page);

    await page.getByRole("button", { name: "Cambiar contraseña" }).click();

    await expect.poll(() => api.passwordRequests.length).toBe(1);
    await expect(page).toHaveURL(/\/$/);
    expect(api.passwordRequests).toHaveLength(1);
    await expectNoSecretsInBrowserStorage(page);
    expect(page.url()).not.toContain(CURRENT_PASSWORD);
    expect(page.url()).not.toContain(NEW_PASSWORD);
  });

  test("detiene el PATCH y muestra un error controlado cuando falla CSRF", async ({
    page,
  }) => {
    const api = await mockPasswordChangeApi(page, { csrfStatus: 403 });
    await openSecurityTab(page);
    await fillPasswordForm(page);

    await page.getByRole("button", { name: "Cambiar contraseña" }).click();

    await expect.poll(() => api.csrfRequests).toBe(1);
    expect(api.passwordRequests).toHaveLength(0);
    await expect(page.getByText("Error de conexión")).toBeVisible();
    await expect(
      page.getByRole("alert").filter({
        hasText: "No fue posible cambiar la contraseña.",
      }),
    ).toBeVisible();
    await expect(page.locator('input[name="passwordActual"]')).toHaveValue(
      CURRENT_PASSWORD,
    );
    await expectNoSecretsInBrowserStorage(page);
  });
});
