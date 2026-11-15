const { test, expect } = require("@playwright/test");

test.use({ timezoneId: "America/Bogota" });

const USER_WITH_REPORTS = {
  id: 901,
  username: "qa.statistics",
  tipoPerfil: "ADMINISTRATIVO",
  permisos: ["Ver reportes"],
};

const SEMESTERS = [
  {
    año: 2026,
    semestre: 2,
    etiqueta: "2026-2",
    periodoInicio: "2026-06-01",
    periodoFin: "2026-12-31",
  },
];

const EMPTY_STATISTICS = {
  totalConsultas: 0,
  consultasFinalizadas: 0,
  consultasPendientes: 0,
  consultasPorEstado: [],
  consultasPorTipoViolencia: [],
  consultasPorArea: [],
  totalPersonasAtendidas: 0,
  personasPorGenero: [],
  personasPorEstrato: [],
  personasPorZona: [],
  personasPorGrupoEtnico: [],
  personasPorCondicion: [],
  personasPorMunicipio: [],
  totalConciliaciones: 0,
  conciliacionesPorEstado: [],
  totalSeguimientos: 0,
  seguimientosPorEstado: [],
  procesosPorEstado: [],
  totalEstudiantesActivos: 0,
  totalEstudiantesHabilitadosConciliacion: 0,
};

function json(route, body, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function mockStatisticsApi(page) {
  const state = {
    rangeQueries: [],
  };

  await page.route("**/api/**", async (route) => {
    const url = new URL(route.request().url());
    const pathname = url.pathname;

    if (pathname.endsWith("/api/auth/me")) {
      await json(route, USER_WITH_REPORTS);
      return;
    }

    if (pathname.endsWith("/api/estadisticas/semestres")) {
      await json(route, SEMESTERS);
      return;
    }

    if (pathname.endsWith("/api/estadisticas/2026/semestre/2")) {
      await json(route, EMPTY_STATISTICS);
      return;
    }

    if (pathname.endsWith("/api/estadisticas/reporte")) {
      state.rangeQueries.push(Object.fromEntries(url.searchParams));
      await json(route, EMPTY_STATISTICS);
      return;
    }

    await json(route, { mensaje: "Ruta sintética no configurada" }, 404);
  });

  return state;
}

async function verifyLocalCalendarDate(page, utcInstant, expectedLocalDate) {
  const api = await mockStatisticsApi(page);
  await page.clock.setFixedTime(new Date(utcInstant));
  await page.goto("/estadisticas");
  await page.getByRole("tab", { name: "Rango personalizado" }).click();

  const dateInputs = page.locator('input[type="date"]');
  const startDate = dateInputs.nth(0);
  const endDate = dateInputs.nth(1);

  await expect(startDate).toHaveValue("2024-01-01");
  await expect(endDate).toHaveValue(expectedLocalDate);
  await expect(startDate).toHaveAttribute("max", expectedLocalDate);
  await expect(endDate).toHaveAttribute("max", expectedLocalDate);

  await page.getByRole("button", { name: "Consultar" }).click();

  await expect.poll(() => api.rangeQueries.length).toBeGreaterThan(0);
  expect(api.rangeQueries.length).toBeLessThanOrEqual(2);
  for (const query of api.rangeQueries) {
    expect(query).toEqual({
      fechaInicio: "2024-01-01",
      fechaFin: expectedLocalDate,
    });
  }
}

test.describe("SCRUM-385: fecha local en estadísticas", () => {
  test("usa el día anterior local cuando UTC ya avanzó de fecha", async ({
    page,
  }) => {
    await verifyLocalCalendarDate(page, "2026-11-16T02:30:00Z", "2026-11-15");
  });

  test("conserva la fecha cuando UTC y Bogotá están en el mismo día", async ({
    page,
  }) => {
    await verifyLocalCalendarDate(page, "2026-11-15T17:00:00Z", "2026-11-15");
  });
});
