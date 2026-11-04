const { test, expect } = require("@playwright/test");

const SYNTHETIC_CORRELATION_ID = "diag-statistics-401";

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

function json(route, status, body, headers = {}) {
  return route.fulfill({
    status,
    headers: headers || {},
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function mockStatisticsApi(page, overrides = {}) {
  const config = {
    auth: { status: 200, body: USER_WITH_REPORTS },
    semesters: { status: 200, body: SEMESTERS },
    semesterReport: { status: 200, body: EMPTY_STATISTICS },
    rangeReport: { status: 200, body: EMPTY_STATISTICS },
    pdf: { status: 200, body: "%PDF-synthetic" },
    ...overrides,
  };

  const state = {
    requests: [],
  };

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const trace = {
      method: request.method(),
      path,
      query: Object.fromEntries(url.searchParams.entries()),
    };
    state.requests.push(trace);

    if (path.endsWith("/api/auth/me")) {
      await json(
        route,
        config.auth.status,
        config.auth.body,
        config.auth.headers,
      );
      return;
    }

    if (path.endsWith("/api/estadisticas/semestres")) {
      await json(
        route,
        config.semesters.status,
        config.semesters.body,
        config.semesters.headers,
      );
      return;
    }

    if (path.endsWith("/api/estadisticas/2026/semestre/2/pdf")) {
      await route.fulfill({
        status: config.pdf.status,
        headers: config.pdf.headers,
        contentType:
          config.pdf.status === 200 ? "application/pdf" : "application/json",
        body:
          config.pdf.status === 200
            ? config.pdf.body
            : JSON.stringify(config.pdf.body),
      });
      return;
    }

    if (path.endsWith("/api/estadisticas/2026/semestre/2")) {
      await json(
        route,
        config.semesterReport.status,
        config.semesterReport.body,
        config.semesterReport.headers,
      );
      return;
    }

    if (path.endsWith("/api/estadisticas/reporte")) {
      await json(
        route,
        config.rangeReport.status,
        config.rangeReport.body,
        config.rangeReport.headers,
      );
      return;
    }

    await json(route, 404, { mensaje: "Ruta sintética no configurada" });
  });

  return state;
}

function unauthorizedResponse() {
  return {
    status: 401,
    headers: { "X-Request-ID": SYNTHETIC_CORRELATION_ID },
    body: {
      estado: 401,
      mensaje: "Debe iniciar sesión para acceder a este recurso",
      correlacionId: SYNTHETIC_CORRELATION_ID,
    },
  };
}

async function openStatistics(page) {
  await page.clock.setFixedTime(new Date("2026-11-02T17:00:00Z"));
  await page.goto("/estadisticas");
}

test.describe("SCRUM-333: diagnóstico frontend del 401 en estadísticas", () => {
  test("redirige al login cuando la comprobación inicial de sesión responde 401", async ({
    page,
  }) => {
    const api = await mockStatisticsApi(page, {
      auth: unauthorizedResponse(),
    });

    await openStatistics(page);

    await expect(page).toHaveURL(/\/$/);
    expect(
      api.requests.filter((request) =>
        request.path.includes("/api/estadisticas"),
      ),
    ).toHaveLength(0);
  });

  test("un 401 al listar semestres permanece en la vista sin mensaje específico", async ({
    page,
  }) => {
    const api = await mockStatisticsApi(page, {
      semesters: unauthorizedResponse(),
    });

    await openStatistics(page);

    await expect(page).toHaveURL(/\/estadisticas$/);
    await expect(
      page.getByText("Selecciona un semestre para ver las estadísticas."),
    ).toBeVisible();
    await expect(page.getByText(/sesión/i)).toHaveCount(0);
    expect(
      api.requests.filter((request) =>
        request.path.endsWith("/api/estadisticas/semestres"),
      ),
    ).toHaveLength(1);
  });

  test("un 401 del reporte semestral muestra el error genérico y no redirige", async ({
    page,
  }) => {
    await mockStatisticsApi(page, {
      semesterReport: unauthorizedResponse(),
    });

    await openStatistics(page);

    await expect(page).toHaveURL(/\/estadisticas$/);
    await expect(page.getByText("Error al cargar estadísticas.")).toBeVisible();
    await expect(page.locator("body")).not.toContainText(
      SYNTHETIC_CORRELATION_ID,
    );
  });

  test("un 401 del reporte por rango muestra el mensaje recibido y no redirige", async ({
    page,
  }) => {
    await mockStatisticsApi(page, {
      rangeReport: unauthorizedResponse(),
    });
    await openStatistics(page);
    await expect(
      page.getByRole("button", { name: "Exportar PDF" }),
    ).toBeEnabled();

    await page.getByRole("tab", { name: "Rango personalizado" }).click();
    await page.getByRole("button", { name: "Consultar" }).click();

    await expect(page).toHaveURL(/\/estadisticas$/);
    await expect(
      page.getByText("Debe iniciar sesión para acceder a este recurso"),
    ).toBeVisible();
    await expect(page.locator("body")).not.toContainText(
      SYNTHETIC_CORRELATION_ID,
    );
  });

  test("un 401 del PDF usa el aviso genérico y permanece en estadísticas", async ({
    page,
  }) => {
    const api = await mockStatisticsApi(page, {
      pdf: unauthorizedResponse(),
    });
    await openStatistics(page);
    await expect(
      page.getByRole("button", { name: "Exportar PDF" }),
    ).toBeEnabled();

    const dialogPromise = page.waitForEvent("dialog");
    await page.getByRole("button", { name: "Exportar PDF" }).click();
    const dialog = await dialogPromise;

    expect(dialog.message()).toBe("El PDF no está disponible aún.");
    await dialog.dismiss();
    await expect(page).toHaveURL(/\/estadisticas$/);
    expect(
      api.requests.filter((request) => request.path.endsWith("/pdf")),
    ).toHaveLength(1);
  });
});
