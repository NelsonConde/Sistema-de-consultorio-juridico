const { test, expect } = require("@playwright/test");

const PERFILES = [
  "ADMINISTRATIVO",
  "ASESOR",
  "MONITOR",
  "ESTUDIANTE",
  "CONCILIADOR",
];

function perfilFixture(
  tipoPerfil,
  email = `${tipoPerfil.toLowerCase()}@prueba.local`,
) {
  return {
    username: `${tipoPerfil.toLowerCase()}.prueba`,
    rolNombre: `Rol ${tipoPerfil}`,
    tipoPerfil,
    nombre: `Usuario ${tipoPerfil}`,
    email,
    telefono: "+57 300 000 0000",
    sede: "Sede Central",
    codigo: `COD-${tipoPerfil}`,
  };
}

async function mockPerfilApi(page, tipoPerfil, options = {}) {
  const {
    contactFailure = null,
    passwordFailure = null,
    profileDelayMs = 0,
    profileFailures = 0,
  } = options;
  let actual = perfilFixture(tipoPerfil);
  let remainingProfileFailures = profileFailures;
  const patchBodies = [];
  const passwordBodies = [];
  const profileRequests = [];

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());

    if (url.pathname.endsWith("/api/auth/me")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ id: 1, tipoPerfil, permisos: [] }),
      });
      return;
    }

    if (url.pathname.endsWith("/api/auth/csrf")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          headerName: "X-CSRF-TOKEN",
          token: "test-token",
        }),
      });
      return;
    }

    if (url.pathname.endsWith("/api/mi-perfil")) {
      profileRequests.push({ method: request.method(), path: url.pathname });

      if (profileDelayMs > 0) {
        await new Promise((resolve) => setTimeout(resolve, profileDelayMs));
      }

      if (remainingProfileFailures > 0) {
        remainingProfileFailures -= 1;
        await route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({ mensaje: "Perfil no disponible" }),
        });
        return;
      }

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(actual),
      });
      return;
    }

    if (url.pathname.endsWith("/api/mi-perfil/contacto")) {
      const body = request.postDataJSON();
      patchBodies.push(body);

      if (contactFailure) {
        await route.fulfill({
          status: contactFailure.status,
          contentType: "application/json",
          body: JSON.stringify(contactFailure.body),
        });
        return;
      }

      actual = { ...actual, email: body.email, telefono: body.telefono };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(actual),
      });
      return;
    }

    if (url.pathname.endsWith("/api/auth/cambiar-password")) {
      const body = request.postDataJSON();
      passwordBodies.push(body);

      if (passwordFailure) {
        await route.fulfill({
          status: passwordFailure.status,
          contentType: "application/json",
          body: JSON.stringify(passwordFailure.body),
        });
        return;
      }

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ mensaje: "Contraseña actualizada" }),
      });
      return;
    }

    await route.fulfill({
      status: 404,
      contentType: "application/json",
      body: "{}",
    });
  });

  return {
    patchBodies,
    passwordBodies,
    profileRequests,
    getActual: () => actual,
  };
}

test.describe("Autogestión de perfil y protección de datos", () => {
  for (const tipoPerfil of PERFILES) {
    test(`Perfil ${tipoPerfil}: lectura y actualización autorizada de contacto`, async ({
      page,
    }) => {
      const api = await mockPerfilApi(page, tipoPerfil);

      await page.goto("/mi-perfil");

      // Verifica visualización de atributos de solo lectura
      await expect(page.getByText(`Usuario ${tipoPerfil}`)).toBeVisible();
      await expect(page.getByText(`COD-${tipoPerfil}`)).toBeVisible();
      await expect(page.getByText("Sede Central")).toBeVisible();

      // Edita contacto propio
      const nuevoEmail = `${tipoPerfil.toLowerCase()}.nuevo@prueba.local`;
      const nuevoTelefono = "+57 310 987 6543";

      await page.getByLabel("Correo electrónico").fill(nuevoEmail);
      await page.getByLabel("Teléfono").fill(nuevoTelefono);
      await page.getByRole("button", { name: "Guardar contacto" }).click();

      // Verifica que solo email y teléfono fueron enviados
      await expect.poll(() => api.patchBodies.length).toBe(1);
      expect(api.patchBodies[0]).toEqual({
        email: nuevoEmail,
        telefono: nuevoTelefono,
      });

      // Verifica que atributos protegidos de solo lectura no fueron alterados
      expect(api.getActual().codigo).toBe(`COD-${tipoPerfil}`);
      expect(api.getActual().sede).toBe("Sede Central");
      expect(api.getActual().tipoPerfil).toBe(tipoPerfil);
      await expect(
        page.getByText("Los datos de contacto se guardaron correctamente."),
      ).toBeVisible();
    });
  }

  test("Validación de formulario: bloquea correo y teléfono inválidos antes de enviar", async ({
    page,
  }) => {
    const api = await mockPerfilApi(page, "ESTUDIANTE");
    await page.goto("/mi-perfil");

    await page
      .getByLabel("Correo electrónico")
      .fill("correo-invalido-sin-dominio");
    await page.getByLabel("Teléfono").fill("abc-sin-numeros");
    await page.getByRole("button", { name: "Guardar contacto" }).click();

    await expect(
      page.getByText("Ingrese un correo electrónico válido"),
    ).toBeVisible();
    await expect(page.getByText("Ingresa un teléfono válido")).toBeVisible();
    expect(api.patchBodies).toHaveLength(0);
  });

  test("Protección de atributos: los campos protegidos no tienen inputs editables", async ({
    page,
  }) => {
    await mockPerfilApi(page, "ADMINISTRATIVO");
    await page.goto("/mi-perfil");

    // Verificar que campos protegidos no son editables ni existen como inputs
    await expect(page.locator("input[name='id']")).toHaveCount(0);
    await expect(page.locator("input[name='rol']")).toHaveCount(0);
    await expect(page.locator("input[name='perfil']")).toHaveCount(0);
    await expect(page.locator("input[name='documento']")).toHaveCount(0);
    await expect(page.locator("input[name='codigo']")).toHaveCount(0);
    await expect(page.locator("input[name='sede']")).toHaveCount(0);
    await expect(page.locator("input[name='activo']")).toHaveCount(0);

    // Solo email y teléfono son inputs del formulario de contacto
    await expect(page.locator("input[name='email']")).toBeVisible();
    await expect(page.locator("input[name='telefono']")).toBeVisible();
  });

  test("conserva el borrador de contacto y limpia contraseñas al cambiar de pestaña", async ({
    page,
  }) => {
    await mockPerfilApi(page, "ASESOR");
    await page.goto("/mi-perfil");

    const draftEmail = "borrador.asesor@prueba.local";
    await page.getByLabel("Correo electrónico").fill(draftEmail);

    await page.getByRole("tab", { name: "Seguridad" }).click();
    await page.getByLabel("Contraseña actual").fill("Actual123");
    await page.getByLabel("Nueva contraseña").fill("NuevaClave123");
    await page.getByLabel("Confirmar nueva contraseña").fill("NuevaClave123");

    await page.getByRole("tab", { name: "Contacto" }).click();
    await expect(page.getByLabel("Correo electrónico")).toHaveValue(draftEmail);

    await page.getByRole("tab", { name: "Seguridad" }).click();
    await expect(page.getByLabel("Contraseña actual")).toHaveValue("");
    await expect(page.getByLabel("Nueva contraseña")).toHaveValue("");
    await expect(page.getByLabel("Confirmar nueva contraseña")).toHaveValue("");
  });

  test("cambia la contraseña usando únicamente el contrato vigente", async ({
    page,
  }) => {
    const api = await mockPerfilApi(page, "MONITOR");
    await page.goto("/mi-perfil");
    await page.getByRole("tab", { name: "Seguridad" }).click();

    await page.getByLabel("Contraseña actual").fill("Actual123");
    await page.getByLabel("Nueva contraseña").fill("NuevaClave123");
    await page.getByLabel("Confirmar nueva contraseña").fill("NuevaClave123");
    await page.getByRole("button", { name: "Cambiar contraseña" }).click();

    await expect.poll(() => api.passwordBodies.length).toBe(1);
    expect(api.passwordBodies[0]).toEqual({
      passwordActual: "Actual123",
      passwordNueva: "NuevaClave123",
    });
    await expect(
      page.getByText("La contraseña se cambió correctamente."),
    ).toBeVisible();
    await expect(page.getByLabel("Contraseña actual")).toHaveValue("");
    await expect(page.getByLabel("Nueva contraseña")).toHaveValue("");
    await expect(page.getByLabel("Confirmar nueva contraseña")).toHaveValue("");
  });

  test("muestra errores largos sin generar desplazamiento horizontal", async ({
    page,
  }) => {
    const longMessage =
      "No fue posible actualizar el contacto porque la información recibida no cumple las reglas vigentes de validación institucional. Revise el correo y el teléfono antes de intentarlo nuevamente.";

    await mockPerfilApi(page, "CONCILIADOR", {
      contactFailure: {
        status: 400,
        body: { mensaje: longMessage },
      },
    });
    await page.setViewportSize({ width: 360, height: 800 });
    await page.goto("/mi-perfil");
    await page.getByRole("button", { name: "Guardar contacto" }).click();

    await expect(
      page.getByTestId("profile-panel").getByRole("alert"),
    ).toContainText(longMessage);
    await expect
      .poll(() =>
        page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      )
      .toBe(true);
  });

  test("presenta loading, error y recuperación del perfil", async ({
    page,
  }) => {
    await mockPerfilApi(page, "ADMINISTRATIVO", {
      profileDelayMs: 250,
      profileFailures: 1,
    });
    await page.goto("/mi-perfil");

    await expect(page.getByTestId("profile-loading")).toBeVisible();
    await expect(
      page.getByText("No fue posible cargar tu perfil"),
    ).toBeVisible();
    await page.getByRole("button", { name: "Reintentar" }).click();
    await expect(page.getByText("Usuario ADMINISTRATIVO")).toBeVisible();
  });

  test("mantiene una distribución responsive en 360, 768 y 1280 px", async ({
    page,
  }) => {
    await mockPerfilApi(page, "ESTUDIANTE");

    for (const width of [360, 768, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      await page.goto("/mi-perfil");

      const summary = await page.getByTestId("profile-summary").boundingBox();
      const panel = await page.getByTestId("profile-panel").boundingBox();
      expect(summary).not.toBeNull();
      expect(panel).not.toBeNull();

      if (width === 1280) {
        expect(panel.x).toBeGreaterThan(summary.x + summary.width - 2);
      } else {
        expect(panel.y).toBeGreaterThan(summary.y + summary.height - 2);
      }

      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
    }
  });

  test("permite navegar las pestañas con teclado en claro y oscuro", async ({
    page,
  }) => {
    await mockPerfilApi(page, "ADMINISTRATIVO");
    await page.goto("/mi-perfil");

    const contactoTab = page.getByRole("tab", { name: "Contacto" });
    const seguridadTab = page.getByRole("tab", { name: "Seguridad" });
    await contactoTab.focus();
    await page.keyboard.press("ArrowRight");
    await expect(seguridadTab).toHaveAttribute("data-state", "active");

    await page.evaluate(() => document.documentElement.classList.add("dark"));
    await expect(page.getByTestId("profile-summary")).toBeVisible();
    await expect(page.getByTestId("profile-panel")).toBeVisible();
  });
});
