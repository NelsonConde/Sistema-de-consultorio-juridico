const { test, expect } = require("@playwright/test");

const USER_WITH_REPORTS = {
  id: 901,
  username: "qa.statistics",
  tipoPerfil: "ADMINISTRATIVO",
  permisos: ["Ver reportes"],
};

const SEMESTERS = [{
  año: 2026,
  semestre: 2,
  etiqueta: "2026-2",
  periodoInicio: "2026-06-01",
  periodoFin: "2026-12-31",
}];

const STATISTICS = {
  totalConsultas: 17,
  consultasFinalizadas: 7,
  consultasPendientes: 10,
  consultasPorEstado: [{ nombre: "ACTIVO", cantidad: 10 }],
  consultasPorTipoViolencia: [],
  consultasPorArea: [],
  totalPersonasAtendidas: 13,
  personasPorGenero: [],
  personasPorEstrato: [],
  personasPorZona: [],
  personasPorGrupoEtnico: [],
  personasPorCondicion: [],
  personasPorMunicipio: [],
  totalConciliaciones: 4,
  conciliacionesPorEstado: [],
  totalSeguimientos: 9,
  seguimientosPorEstado: [{ nombre: "PENDIENTE", cantidad: 3 }],
  procesosPorEstado: [{ nombre: "EN_TRAMITE", cantidad: 6 }],
  totalEstudiantesActivos: 23,
  totalEstudiantesHabilitadosConciliacion: 8,
};

function json(route, body) {
  return route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function mockStatisticsApi(page) {
  const requests = [];

  await page.route("**/api/**", async (route) => {
    const url = new URL(route.request().url());
    requests.push({ path: url.pathname, query: Object.fromEntries(url.searchParams) });

    if (url.pathname.endsWith("/api/auth/me")) return json(route, USER_WITH_REPORTS);
    if (url.pathname.endsWith("/api/estadisticas/semestres")) return json(route, SEMESTERS);
    if (url.pathname.endsWith("/api/estadisticas/2026/semestre/2")) return json(route, STATISTICS);
    if (url.pathname.endsWith("/api/estadisticas/reporte")) return json(route, STATISTICS);

    return route.fulfill({ status: 404, contentType: "application/json", body: "{}" });
  });

  return requests;
}

async function assertSemantics(page) {
  const studentCard = page.getByRole("button", { name: /Estudiantes activos/ });
  await expect(studentCard).toContainText("23");
  await studentCard.click();
  await expect(page.getByText("Estado actual (sin filtro de período)")).toBeVisible();

  const trackingCard = page.getByRole("button", { name: /Seguimientos/ });
  await expect(trackingCard).toContainText("9");
  await trackingCard.click();
  await expect(page.getByText("Período seleccionado")).toBeVisible();
  await expect(page.getByText("Sin filtro semestral")).toHaveCount(0);
  await expect(page.getByText("6", { exact: true })).toBeVisible();

  await expect(page.getByRole("button", { name: /Consultas/ })).toContainText("17");
  await expect(page.getByRole("button", { name: /Personas atendidas/ })).toContainText("13");
  await expect(page.getByRole("button", { name: /Conciliaciones/ })).toContainText("4");
}

test.describe("H-06: semántica temporal de estadísticas", () => {
  test("distingue estado actual y período en la modalidad semestral", async ({ page }) => {
    const requests = await mockStatisticsApi(page);
    await page.goto("/estadisticas");

    await assertSemantics(page);
    expect(requests.some(({ path }) => path.endsWith("/api/estadisticas/2026/semestre/2"))).toBe(true);
  });

  test("conserva la semántica y los filtros del rango personalizado", async ({ page }) => {
    const requests = await mockStatisticsApi(page);
    await page.goto("/estadisticas");
    await page.getByRole("tab", { name: "Rango personalizado" }).click();

    const dateInputs = page.locator('input[type="date"]');
    await dateInputs.nth(0).fill("2026-07-01");
    await dateInputs.nth(1).fill("2026-07-31");
    await page.getByRole("button", { name: "Consultar" }).click();

    await assertSemantics(page);
    expect(requests.some(({ path, query }) =>
      path.endsWith("/api/estadisticas/reporte") &&
      query.fechaInicio === "2026-07-01" &&
      query.fechaFin === "2026-07-31",
    )).toBe(true);
  });
});
