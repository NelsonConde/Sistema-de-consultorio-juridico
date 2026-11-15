const { test, expect } = require("@playwright/test");

const STUDENT = {
  id: 51,
  nombre: "Estudiante QA",
  username: "estudiante.qa",
  tipoPerfil: "ESTUDIANTE",
  permisos: ["Ver consultas", "Ver seguimientos"],
};

const ADVISOR = {
  id: 61,
  nombre: "Asesor QA",
  username: "asesor.qa",
  tipoPerfil: "ASESOR",
  permisos: ["Ver consultas", "Ver seguimientos"],
};

const STATISTICS = {
  totalConsultas: 6,
  consultasFinalizadas: 1,
  consultasPendientes: 5,
  totalPersonasAtendidas: 6,
  consultasPorArea: [],
  procesosPorEstado: [],
};

const CONSULTATIONS = Array.from({ length: 6 }, (_, index) => ({
  id: 101 + index,
  descripcion: `Consulta accesible ${index + 1}`,
  estado: "PENDIENTE",
}));

function json(route, body, status = 200, headers = {}) {
  return route.fulfill({
    status,
    headers,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

function pageResponse(content) {
  return {
    content,
    page: 1,
    size: 10,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
  };
}

function pendingTask(id, consultaId) {
  return {
    id,
    descripcion: `Tarea pendiente ${id}`,
    fechaEntrega: `2026-12-${String(id).padStart(2, "0")}`,
    consultaId,
    estado: "PENDIENTE",
  };
}

async function mockHomeApi(
  page,
  { user = STUDENT, tasks = [], taskStatus = 200 } = {},
) {
  const state = {
    consultationRequests: [],
    followUpRequests: [],
    responseRequests: 0,
  };

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const pathname = url.pathname;

    if (pathname.endsWith("/api/auth/me")) {
      await json(route, user);
      return;
    }

    if (pathname.includes("/api/estadisticas/")) {
      await json(route, STATISTICS);
      return;
    }

    if (pathname.endsWith("/api/consultas")) {
      state.consultationRequests.push(Object.fromEntries(url.searchParams));
      const content =
        url.searchParams.get("estado") === "PENDIENTE" ? CONSULTATIONS : [];
      await json(route, pageResponse(content));
      return;
    }

    if (pathname.endsWith("/api/seguimientos/respuestas/pendientes")) {
      state.responseRequests += 1;
      await json(route, [
        {
          id: 701,
          contenido: "Respuesta pendiente de revisión",
          consultaId: 101,
          estudianteNombre: "Estudiante QA",
        },
      ]);
      return;
    }

    if (pathname.endsWith("/api/seguimientos")) {
      state.followUpRequests.push(Object.fromEntries(url.searchParams));
      await json(
        route,
        taskStatus === 200
          ? pageResponse(tasks)
          : { mensaje: "No fue posible consultar los seguimientos" },
        taskStatus,
        taskStatus === 200 ? {} : { "X-Request-ID": "home-tasks-error" },
      );
      return;
    }

    await json(route, { mensaje: "Ruta sintética no configurada" }, 404);
  });

  return state;
}

function panel(page, title) {
  return page
    .getByText(title, { exact: true })
    .locator("xpath=ancestor::div[contains(@class, 'bg-card')][1]");
}

test.describe("SCRUM-384: tareas pendientes en Inicio", () => {
  test("consulta seguimientos pendientes y muestra una tarea de la sexta consulta", async ({
    page,
  }) => {
    const tasks = [
      pendingTask(1, 106),
      ...Array.from({ length: 10 }, (_, index) => pendingTask(index + 2, 101)),
      {
        ...pendingTask(21, 102),
        descripcion: "Tarea completada que no debe aparecer",
        estado: "COMPLETADO",
      },
      {
        ...pendingTask(22, 103),
        descripcion: "Tarea cancelada que no debe aparecer",
        estado: "CANCELADO",
      },
    ];
    const api = await mockHomeApi(page, { tasks });

    await page.goto("/inicio");

    const tasksPanel = panel(page, "Mis tareas pendientes");
    await expect(tasksPanel).toContainText("Tarea pendiente 1");
    await expect(tasksPanel).toContainText("Consulta #106");
    await expect(tasksPanel.locator("p.text-xs.font-medium")).toHaveCount(10);
    await expect(tasksPanel).not.toContainText(
      "Tarea completada que no debe aparecer",
    );
    await expect(tasksPanel).not.toContainText(
      "Tarea cancelada que no debe aparecer",
    );

    await expect(panel(page, "Consultas pendientes")).toContainText(
      "Consulta accesible 6",
    );
    await expect(
      page.getByText("Total consultas", { exact: true }),
    ).toBeVisible();

    await expect.poll(() => api.followUpRequests.length).toBeGreaterThan(0);
    expect(api.followUpRequests.length).toBeLessThanOrEqual(2);
    const expectedFollowUpQuery = {
      estado: "PENDIENTE",
      page: "1",
      size: "10",
      sortBy: "fechaEntrega",
      direction: "asc",
    };
    expect(
      api.followUpRequests.every(
        (query) =>
          JSON.stringify(query) === JSON.stringify(expectedFollowUpQuery),
      ),
    ).toBe(true);
    expect(api.consultationRequests.length).toBeGreaterThanOrEqual(4);
    expect(api.consultationRequests.length).toBeLessThanOrEqual(8);
    expect(
      api.consultationRequests.some(
        (query) => query.page === "1" && query.size === "5",
      ),
    ).toBe(false);
  });

  test("mantiene el estado vacío cuando el endpoint no devuelve tareas", async ({
    page,
  }) => {
    await mockHomeApi(page, { tasks: [] });

    await page.goto("/inicio");

    await expect(panel(page, "Mis tareas pendientes")).toContainText(
      "Sin tareas pendientes",
    );
  });

  test("muestra un error controlado sin presentarlo como lista vacía", async ({
    page,
  }) => {
    await mockHomeApi(page, { taskStatus: 503 });

    await page.goto("/inicio");

    const tasksPanel = panel(page, "Mis tareas pendientes");
    await expect(tasksPanel.getByRole("alert")).toContainText(
      "No se pudieron cargar las tareas pendientes.",
    );
    await expect(tasksPanel).not.toContainText("Sin tareas pendientes");
  });

  test("no solicita ni muestra tareas para un usuario que no es estudiante", async ({
    page,
  }) => {
    const api = await mockHomeApi(page, {
      user: ADVISOR,
      tasks: [pendingTask(1, 106)],
    });

    await page.goto("/inicio");

    await expect(
      page.getByText("Mis tareas pendientes", { exact: true }),
    ).toHaveCount(0);
    await expect(panel(page, "Consultas pendientes")).toBeVisible();
    await expect(panel(page, "Respuestas por calificar")).toContainText(
      "Respuesta pendiente de revisión",
    );
    expect(api.followUpRequests).toHaveLength(0);
    expect(api.responseRequests).toBeGreaterThanOrEqual(1);
    expect(api.responseRequests).toBeLessThanOrEqual(2);
  });
});
