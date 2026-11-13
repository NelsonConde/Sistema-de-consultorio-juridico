const { test, expect } = require("@playwright/test");

function json(route, body, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function mockAdminApi(page, { permissions, resendStatus = 200 } = {}) {
  const state = {
    pendingQueries: [],
    resendRequests: [],
    csrfRequests: 0,
  };

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());

    if (url.pathname.endsWith("/api/auth/me")) {
      await json(route, {
        id: 1,
        username: "admin@ufps.edu.co",
        tipoPerfil: "ADMINISTRATIVO",
        permisos: permissions ?? [
          "Crear usuarios",
          "Gestionar usuarios",
          "Ver catálogos",
        ],
      });
      return;
    }

    if (url.pathname.endsWith("/api/auth/csrf")) {
      state.csrfRequests += 1;
      await json(route, {
        headerName: "X-CSRF-TOKEN",
        token: "csrf-invitations",
      });
      return;
    }

    if (
      url.pathname.endsWith("/api/usuarios-sistema") &&
      request.method() === "GET"
    ) {
      state.pendingQueries.push(Object.fromEntries(url.searchParams));
      await json(route, {
        content: [
          {
            id: 27,
            username: "pendiente@ufps.edu.co",
            rolNombre: "Estudiante",
            tipoPerfil: "ESTUDIANTE",
            activo: true,
            estadoActivacion: "PENDIENTE",
          },
        ],
        page: 1,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      });
      return;
    }

    if (
      url.pathname.endsWith(
        "/api/usuarios-sistema/27/invitacion-activacion/reenviar",
      ) &&
      request.method() === "POST"
    ) {
      state.resendRequests.push({
        csrf: request.headers()["x-csrf-token"] ?? null,
      });
      await new Promise((resolve) => setTimeout(resolve, 150));
      await json(
        route,
        resendStatus === 200
          ? { mensaje: "Solicitud procesada" }
          : { mensaje: "Proveedor no disponible" },
        resendStatus,
      );
      return;
    }

    if (
      url.pathname.endsWith("/api/tipos-documento/activos") ||
      url.pathname.endsWith("/api/sedes") ||
      url.pathname.endsWith("/api/areas")
    ) {
      await json(route, []);
      return;
    }

    await json(route, [], 200);
  });

  return state;
}

test.describe("gestión administrativa de invitaciones", () => {
  test("identifica pendientes y evita solicitudes duplicadas con CSRF", async ({
    page,
  }) => {
    const api = await mockAdminApi(page);

    await page.goto("/roles");

    await expect(
      page.getByRole("heading", { name: "Cuentas pendientes de activación" }),
    ).toBeVisible();
    await expect(page.getByText("pendiente@ufps.edu.co")).toBeVisible();
    await expect(page.getByText("Pendiente", { exact: true })).toBeVisible();
    await expect.poll(() => api.pendingQueries.length).toBeGreaterThan(0);
    expect(api.pendingQueries[0]).toMatchObject({
      activo: "true",
      estadoActivacion: "PENDIENTE",
      page: "1",
      size: "10",
    });

    const resend = page.getByRole("button", { name: "Reenviar invitación" });
    await resend.evaluate((button) => {
      button.click();
      button.click();
    });

    await expect.poll(() => api.resendRequests.length).toBe(1);
    expect(api.resendRequests[0].csrf).toBe("csrf-invitations");
    expect(api.csrfRequests).toBe(1);
    await expect(
      page.getByText("Solicitud de reenvío procesada"),
    ).toBeVisible();
    await expect(
      page.getByText(/La entrega depende del proveedor de correo/),
    ).toBeVisible();
  });

  test("oculta la gestión sin el permiso exigido por el endpoint", async ({
    page,
  }) => {
    const api = await mockAdminApi(page, {
      permissions: ["Crear usuarios", "Ver catálogos"],
    });

    await page.goto("/roles");

    await expect(
      page.getByRole("heading", { name: "Crear Usuario" }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Cuentas pendientes de activación" }),
    ).toHaveCount(0);
    expect(api.pendingQueries).toHaveLength(0);
  });

  test("muestra el fallo HTTP y permite reintentar", async ({ page }) => {
    await mockAdminApi(page, { resendStatus: 503 });
    await page.goto("/roles");

    await page.getByRole("button", { name: "Reenviar invitación" }).click();

    await expect(
      page.getByRole("alert").filter({ hasText: "Proveedor no disponible" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Reintentar carga" }),
    ).toBeVisible();
  });
});
