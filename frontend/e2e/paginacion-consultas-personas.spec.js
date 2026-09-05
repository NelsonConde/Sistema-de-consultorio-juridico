const { test, expect } = require("@playwright/test");

const USER = {
  id: 1,
  tipoPerfil: "ADMINISTRATIVO",
  permisos: [
    "Acceder consultas jurídicas",
    "Ver consultas",
    "Editar consultas",
    "Archivar consultas",
    "Cambiar estado consultas",
    "Acceder personas",
    "Ver personas",
    "Editar personas",
    "Cambiar estado personas",
  ],
};

function consulta(id, overrides = {}) {
  return {
    id,
    version: 1,
    consulta: `Consulta ${id}`,
    fecha: "2026-09-04",
    nombre: `Nombre ${id}`,
    apellido: `Apellido ${id}`,
    cedula: `***${String(id).padStart(4, "0")}`,
    estado: "ACTIVO",
    ...overrides,
  };
}

function persona(id, overrides = {}) {
  return {
    id,
    nombres: `Nombre ${id}`,
    apellidos: `Apellido ${id}`,
    tipoDocumento: "CC",
    numeroDocumentoEnmascarado: `***${String(id).padStart(4, "0")}`,
    tipoPersona: "NATURAL",
    activo: true,
    ...overrides,
  };
}

async function mockBaseApi(page) {
  await page.route("**/api/**", async (route) => {
    const url = new URL(route.request().url());

    if (url.pathname.endsWith("/api/auth/me")) {
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(USER) });
      return;
    }

    if (url.pathname.endsWith("/api/auth/csrf")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "test-token" }),
      });
      return;
    }

    await route.fulfill({ status: 200, contentType: "application/json", body: "[]" });
  });
}

test.describe("Paginación remota de Consultas", () => {
  test("carga, pagina y busca usando el contrato del backend", async ({ page }) => {
    const requests = [];

    await mockBaseApi(page);
    await page.route("**/api/consultas?**", async (route) => {
      const url = new URL(route.request().url());
      requests.push(url);

      const currentPage = Number(url.searchParams.get("page") || 1);
      const size = Number(url.searchParams.get("size") || 10);
      const search = url.searchParams.get("search") || "";
      const start = (currentPage - 1) * size + 1;
      const totalElements = search ? 3 : 25;
      const count = Math.min(size, Math.max(totalElements - (start - 1), 0));
      const content = Array.from({ length: count }, (_, index) =>
        consulta(start + index, search ? { consulta: `${search} ${start + index}` } : {})
      );

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          content,
          page: currentPage,
          size,
          totalElements,
          totalPages: Math.ceil(totalElements / size),
        }),
      });
    });

    await page.goto("/consultasjuridicas");

    await expect.poll(() => requests.length).toBeGreaterThan(0);
    const initial = requests[0];
    expect(initial.searchParams.get("page")).toBe("1");
    expect(initial.searchParams.get("size")).toBe("10");
    expect(initial.searchParams.get("sortBy")).toBe("fecha");
    expect(initial.searchParams.get("direction")).toBe("desc");
    await expect(page.getByText("25 registro(s)")).toBeVisible();

    await page.getByRole("button", { name: "2", exact: true }).click();
    await expect.poll(() => requests.some((url) => url.searchParams.get("page") === "2")).toBeTruthy();
    await expect(page.getByText("Consulta 11", { exact: true })).toBeVisible();
    await expect(page.getByText("Consulta 1", { exact: true })).toHaveCount(0);

    await page.getByPlaceholder("Nombre, apellido, cédula o descripción...").fill("contrato");
    await expect.poll(() => requests.some((url) => url.searchParams.get("search") === "contrato"), {
      timeout: 2500,
    }).toBeTruthy();

    const searchRequest = requests.findLast((url) => url.searchParams.get("search") === "contrato");
    expect(searchRequest.searchParams.get("page")).toBe("1");
  });

  test("una respuesta obsoleta no reemplaza la búsqueda más reciente", async ({ page }) => {
    await mockBaseApi(page);

    await page.route("**/api/consultas?**", async (route) => {
      const url = new URL(route.request().url());
      const search = url.searchParams.get("search") || "";

      if (search === "ana") {
        await new Promise((resolve) => setTimeout(resolve, 900));
      }

      const label = search || "inicial";
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          content: [consulta(search === "beatriz" ? 2 : 1, { consulta: label })],
          page: 1,
          size: 10,
          totalElements: 1,
          totalPages: 1,
        }),
      }).catch(() => {});
    });

    await page.goto("/consultasjuridicas");
    const search = page.getByPlaceholder("Nombre, apellido, cédula o descripción...");

    await search.fill("ana");
    await page.waitForTimeout(450);
    await search.fill("beatriz");

    await expect(page.getByText("beatriz", { exact: true })).toBeVisible({ timeout: 2500 });
    await page.waitForTimeout(1000);
    await expect(page.getByText("beatriz", { exact: true })).toBeVisible();
    await expect(page.getByText("ana", { exact: true })).toHaveCount(0);
  });
});

test.describe("Paginación remota de Personas", () => {
  test("usa /personas para el listado general y conserva totales del servidor", async ({ page }) => {
    const requests = [];

    await mockBaseApi(page);
    await page.route("**/api/personas?**", async (route) => {
      const url = new URL(route.request().url());
      requests.push(url);

      const currentPage = Number(url.searchParams.get("page") || 1);
      const size = Number(url.searchParams.get("size") || 10);
      const search = url.searchParams.get("search") || "";
      const totalElements = search ? 2 : 21;
      const start = (currentPage - 1) * size + 1;
      const count = Math.min(size, Math.max(totalElements - (start - 1), 0));

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          content: Array.from({ length: count }, (_, index) => persona(start + index)),
          page: currentPage,
          size,
          totalElements,
          totalPages: Math.ceil(totalElements / size),
        }),
      });
    });

    await page.goto("/personas");

    await expect.poll(() => requests.length).toBeGreaterThan(0);
    const initial = requests[0];
    expect(initial.pathname.endsWith("/api/personas")).toBeTruthy();
    expect(initial.searchParams.get("page")).toBe("1");
    expect(initial.searchParams.get("size")).toBe("10");
    expect(initial.searchParams.get("sortBy")).toBe("nombres");
    expect(initial.searchParams.get("direction")).toBe("asc");
    await expect(page.getByText("21 registro(s)")).toBeVisible();

    await page.getByRole("button", { name: "2", exact: true }).click();
    await expect.poll(() => requests.some((url) => url.searchParams.get("page") === "2")).toBeTruthy();

    await page.getByPlaceholder("Buscar por nombre, apellido o documento...").fill("Laura");
    await expect.poll(() => requests.some((url) => url.searchParams.get("search") === "Laura"), {
      timeout: 2500,
    }).toBeTruthy();
  });

  test("el detalle de persona se solicita solo al editar", async ({ page }) => {
    let detailRequests = 0;

    await mockBaseApi(page);
    await page.route("**/api/personas?**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          content: [persona(1)],
          page: 1,
          size: 10,
          totalElements: 1,
          totalPages: 1,
        }),
      });
    });
    await page.route("**/api/personas/1", async (route) => {
      detailRequests += 1;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...persona(1),
          version: 1,
          fechaNacimiento: null,
          departamentoId: null,
          municipioId: null,
        }),
      });
    });

    await page.goto("/personas");
    await expect(page.getByText("Nombre 1 Apellido 1")).toBeVisible();
    expect(detailRequests).toBe(0);

    await page.getByRole("button", { name: "Editar" }).click();
    await expect.poll(() => detailRequests).toBe(1);
  });
});
