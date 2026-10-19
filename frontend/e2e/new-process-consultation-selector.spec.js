const { test, expect } = require("@playwright/test");

const AUTHORIZED_CONSULTATIONS = [
  {
    id: 101,
    consulta: "Orientación sobre contrato laboral",
    nombre: "Ana",
    apellido: "Pérez",
    cedula: "10000001",
  },
  {
    id: 102,
    consulta: "Revisión de liquidación laboral",
    nombre: "Carlos",
    apellido: "Rojas",
    cedula: "10000002",
  },
];

const FOREIGN_CONSULTATION = {
  id: 999,
  consulta: "Consulta fuera del alcance del usuario",
};

function paged(content, page = 1, totalElements = content.length, totalPages = 1) {
  return {
    content,
    page,
    size: 10,
    totalElements,
    totalPages,
  };
}

async function mockNewProcessApi(page, profile, options = {}) {
  const { failFirstConsultationRequest = false, emptySearch = "" } = options;
  const consultationRequests = [];
  const processBodies = [];
  let remainingFailures = failFirstConsultationRequest ? 1 : 0;

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;

    if (path.endsWith("/api/auth/me")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: 1,
          tipoPerfil: profile,
          permisos: [
            "Acceder procesos",
            "Gestionar procesos",
            "Ver catálogos",
            "Ver consultas",
          ],
        }),
      });
      return;
    }

    if (path.endsWith("/api/auth/csrf")) {
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

    if (path.endsWith("/api/departamentos")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([{ id: 7, nombre: "Derecho privado", activo: true }]),
      });
      return;
    }

    if (path.endsWith("/api/organos-control") || path.endsWith("/api/especialidades")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "[]",
      });
      return;
    }

    if (path.endsWith("/api/consultas")) {
      const query = Object.fromEntries(url.searchParams.entries());
      consultationRequests.push(query);

      // La simulación mantiene la misma lista blanca del backend.
      if (query.sortBy !== "fecha") {
        await route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({
            mensaje: `El campo de ordenamiento '${query.sortBy}' no es válido`,
          }),
        });
        return;
      }

      if (query.direction !== "desc") {
        await route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ mensaje: "Dirección de orden inválida" }),
        });
        return;
      }

      if (remainingFailures > 0) {
        remainingFailures -= 1;
        await route.abort("connectionfailed");
        return;
      }

      const search = (query.search || "").toLowerCase();
      if (emptySearch && search === emptySearch.toLowerCase()) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(paged([], 1, 0, 0)),
        });
        return;
      }

      if (search) {
        const matches = AUTHORIZED_CONSULTATIONS.filter((item) =>
          `${item.consulta} ${item.nombre} ${item.apellido} ${item.cedula}`
            .toLowerCase()
            .includes(search),
        );
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(paged(matches)),
        });
        return;
      }

      const requestedPage = Number(query.page || 1);
      const content = requestedPage === 2
        ? [AUTHORIZED_CONSULTATIONS[1]]
        : [AUTHORIZED_CONSULTATIONS[0]];

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(paged(content, requestedPage, 11, 2)),
      });
      return;
    }

    if (path.endsWith("/api/procesos") && request.method() === "POST") {
      processBodies.push(request.postDataJSON());
      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify({ id: 501 }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "[]",
    });
  });

  return { consultationRequests, processBodies };
}

async function selectDepartment(page) {
  await page.getByRole("combobox").first().selectOption("7");
}

for (const profile of ["ADMINISTRATIVO", "ASESOR"]) {
  test(`${profile}: carga una consulta autorizada y crea el proceso`, async ({ page }) => {
    const api = await mockNewProcessApi(page, profile);
    await page.goto("/nuevoproceso");

    await selectDepartment(page);
    await page.getByRole("button", { name: "Consulta", exact: true }).click();
    await page
      .getByRole("button", { name: /Orientación sobre contrato laboral/ })
      .click();
    await page.getByRole("button", { name: "Crear proceso" }).click();

    await expect.poll(() => api.processBodies.length).toBe(1);
    expect(api.processBodies[0]).toEqual({
      numeroRadicado: null,
      departamentoId: 7,
      consultaId: 101,
      organoControlId: null,
      especialidadId: null,
    });
    expect(api.consultationRequests[0]).toMatchObject({
      page: "1",
      size: "10",
      sortBy: "fecha",
      direction: "desc",
    });
  });
}

test("busca y pagina sin solicitar sortBy=id ni mostrar consultas ajenas", async ({ page }) => {
  const api = await mockNewProcessApi(page, "ADMINISTRATIVO");
  await page.goto("/nuevoproceso");
  await page.getByRole("button", { name: "Consulta", exact: true }).click();

  await expect(
    page.getByRole("button", { name: /Orientación sobre contrato laboral/ }),
  ).toBeVisible();
  await page.getByRole("button", { name: "›", exact: true }).click();
  await expect(
    page.getByRole("button", { name: /Revisión de liquidación laboral/ }),
  ).toBeVisible();

  await page.getByPlaceholder("Buscar...").fill("Ana Pérez");
  await expect(
    page.getByRole("button", { name: /Orientación sobre contrato laboral/ }),
  ).toBeVisible();
  await expect(page.getByText(FOREIGN_CONSULTATION.consulta)).toHaveCount(0);

  expect(api.consultationRequests.some((query) => query.page === "2")).toBe(true);
  expect(
    api.consultationRequests.some(
      (query) => query.page === "1" && query.search === "Ana Pérez",
    ),
  ).toBe(true);
  expect(api.consultationRequests.every((query) => query.sortBy === "fecha")).toBe(true);
});

test("un error de red permite reintentar sin perder los datos del formulario", async ({ page }) => {
  const api = await mockNewProcessApi(page, "ASESOR", {
    failFirstConsultationRequest: true,
  });
  await page.goto("/nuevoproceso");
  await selectDepartment(page);
  await page.getByRole("button", { name: "Consulta", exact: true }).click();

  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByRole("button", { name: "Reintentar" })).toBeVisible();
  await expect(page.getByRole("combobox").first()).toHaveValue("7");

  await page.getByRole("button", { name: "Reintentar" }).click();
  await expect(
    page.getByRole("button", { name: /Orientación sobre contrato laboral/ }),
  ).toBeVisible();
  await expect(page.getByRole("combobox").first()).toHaveValue("7");
  expect(api.consultationRequests).toHaveLength(2);
});

test("el resultado vacío conserva la búsqueda y los demás datos", async ({ page }) => {
  await mockNewProcessApi(page, "ADMINISTRATIVO", { emptySearch: "inexistente" });
  await page.goto("/nuevoproceso");
  await selectDepartment(page);
  await page.getByRole("button", { name: "Consulta", exact: true }).click();
  await page.getByPlaceholder("Buscar...").fill("inexistente");

  await expect(page.getByText("No se encontraron resultados.")).toBeVisible();
  await expect(page.getByPlaceholder("Buscar...")).toHaveValue("inexistente");
  await expect(page.getByRole("combobox").first()).toHaveValue("7");
});
