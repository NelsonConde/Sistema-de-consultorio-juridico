const { test, expect } = require("@playwright/test");

const VIEW_PERMISSIONS = [
  "Acceder conciliaciones",
  "Ver conciliaciones",
];

const PROGRAM_PERMISSION = "Programar reuniones de conciliación";
const RESCHEDULE_PERMISSION = "Reprogramar reuniones de conciliación";

const PENDING_SUMMARY = {
  id: 42,
  version: 7,
  consultaId: 142,
  consulta: "Conciliación pendiente de primera reunión",
  estadoCodigo: "ESPERANDO_REUNION",
  estadoNombre: "Esperando reunión",
  estudianteId: 12,
  estudianteNombre: "Estudiante Asignado",
  conciliadorId: 15,
  conciliadorNombre: "Conciliador Asignado",
  fechaCreacion: "2026-11-01T08:00:00",
  fechaConciliacion: null,
  fechaFinalizacion: null,
  activo: true,
};

const EXISTING_MEETINGS = [
  {
    conciliacionId: 77,
    version: 3,
    conciliacionVersion: 9,
    consultaId: 177,
    estadoCodigo: "REUNION_PROGRAMADA",
    estadoNombre: "Reunión programada",
    sedeId: 3,
    sedeNombre: "Sede principal",
    fechaReunion: "2099-11-10T09:00:00",
    observaciones: "Reunión principal",
    estudianteId: 12,
    estudianteNombre: "Estudiante Asignado",
    conciliadorId: 15,
    conciliadorNombre: "Conciliador Asignado",
    fechaCreacion: "2026-11-01T08:00:00",
    fechaActualizacion: "2026-11-01T08:00:00",
  },
  {
    conciliacionId: 88,
    version: 2,
    conciliacionVersion: 6,
    consultaId: 188,
    estadoCodigo: "COMPLETO_CONCILIADO",
    estadoNombre: "Completo conciliado",
    sedeId: 4,
    sedeNombre: "Sede centro",
    fechaReunion: "2099-12-12T15:30:00",
    observaciones: "Reunión finalizada",
    estudianteId: 22,
    estudianteNombre: "Estudiante Finalizado",
    conciliadorId: 25,
    conciliadorNombre: "Conciliador Finalizado",
    fechaCreacion: "2026-11-02T08:00:00",
    fechaActualizacion: "2026-11-02T08:00:00",
  },
];

function pageOf(content, page = 1, totalElements = content.length, totalPages = 1) {
  return {
    content,
    page,
    size: 10,
    totalElements,
    totalPages,
  };
}

function detailOf(summary, reunion = null) {
  return {
    ...summary,
    reunion,
    partes: [],
    contrapartes: [],
  };
}

function meetingSummaryFromDetail(detail) {
  if (!detail?.reunion) return null;
  return {
    conciliacionId: detail.id,
    version: detail.reunion.version,
    conciliacionVersion: detail.version,
    consultaId: detail.consultaId,
    estadoCodigo: detail.estadoCodigo,
    estadoNombre: detail.estadoNombre,
    sedeId: detail.reunion.sedeId,
    sedeNombre: detail.reunion.sedeNombre,
    fechaReunion: detail.reunion.fechaReunion,
    observaciones: detail.reunion.observaciones,
    estudianteId: detail.estudianteId,
    estudianteNombre: detail.estudianteNombre,
    conciliadorId: detail.conciliadorId,
    conciliadorNombre: detail.conciliadorNombre,
    fechaCreacion: detail.reunion.fechaCreacion,
    fechaActualizacion: detail.reunion.fechaActualizacion,
  };
}

function detailFromMeeting(meeting) {
  return detailOf({
    id: meeting.conciliacionId,
    version: meeting.conciliacionVersion,
    consultaId: meeting.consultaId,
    estadoCodigo: meeting.estadoCodigo,
    estadoNombre: meeting.estadoNombre,
    estudianteId: meeting.estudianteId,
    estudianteNombre: meeting.estudianteNombre,
    conciliadorId: meeting.conciliadorId,
    conciliadorNombre: meeting.conciliadorNombre,
    activo: true,
  }, {
    id: meeting.conciliacionId + 1000,
    version: meeting.version,
    fechaReunion: meeting.fechaReunion,
    sedeId: meeting.sedeId,
    sedeNombre: meeting.sedeNombre,
    observaciones: meeting.observaciones,
    fechaCreacion: meeting.fechaCreacion,
    fechaActualizacion: meeting.fechaActualizacion,
  });
}

async function fulfillJson(route, status, body) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function mockMeetingApi(page, options = {}) {
  const {
    permissions = [...VIEW_PERMISSIONS, PROGRAM_PERMISSION, RESCHEDULE_PERMISSION],
    initialDetail = detailOf(PENDING_SUMMARY),
    pendingListForPage = (requestedPage, current) =>
      requestedPage === 2
        ? pageOf(current.reunion ? [] : [PENDING_SUMMARY], 2, current.reunion ? 10 : 11, 2)
        : pageOf([
            {
              ...PENDING_SUMMARY,
              id: 10,
              consultaId: 110,
              version: 5,
            },
          ], 1, 11, 2),
    meetingListForPage = (_requestedPage, current) => {
      const createdMeeting = meetingSummaryFromDetail(current);
      return pageOf(
        createdMeeting
          ? [...EXISTING_MEETINGS, createdMeeting]
          : EXISTING_MEETINGS,
      );
    },
    detailForId,
    onMutation,
  } = options;

  let currentDetail = structuredClone(initialDetail);
  const pendingListRequests = [];
  const generalListRequests = [];
  const meetingListRequests = [];
  const mutations = [];
  let detailRequests = 0;

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;

    if (path.endsWith("/api/auth/me")) {
      await fulfillJson(route, 200, {
        id: 1,
        username: "admin@prueba.local",
        tipoPerfil: "ADMINISTRATIVO",
        permisos: permissions,
      });
      return;
    }

    if (path.endsWith("/api/auth/csrf")) {
      await fulfillJson(route, 200, {
        headerName: "X-CSRF-TOKEN",
        token: "csrf-reuniones",
      });
      return;
    }

    if (request.method() === "GET" && path.endsWith("/api/sedes")) {
      await fulfillJson(route, 200, [
        { id: 3, nombre: "Sede principal", activo: true },
      ]);
      return;
    }

    if (request.method() === "GET" && path.endsWith("/api/conciliaciones/reuniones")) {
      const query = Object.fromEntries(url.searchParams.entries());
      meetingListRequests.push(query);
      await fulfillJson(
        route,
        200,
        meetingListForPage(Number(query.page || 1), currentDetail),
      );
      return;
    }

    if (request.method() === "GET" && path.endsWith("/api/conciliaciones")) {
      const query = Object.fromEntries(url.searchParams.entries());
      generalListRequests.push(query);
      if (query.estado === "ESPERANDO_REUNION") {
        pendingListRequests.push(query);
      }
      await fulfillJson(
        route,
        200,
        pendingListForPage(Number(query.page || 1), currentDetail),
      );
      return;
    }

    const detailMatch = path.match(/\/api\/conciliaciones\/(\d+)$/);
    if (request.method() === "GET" && detailMatch) {
      detailRequests += 1;
      if (detailForId) {
        currentDetail = structuredClone(
          detailForId(Number(detailMatch[1]), currentDetail),
        );
      }
      await fulfillJson(route, 200, currentDetail);
      return;
    }

    const meetingMatch = path.match(/\/api\/conciliaciones\/(\d+)\/reunion$/);
    if (meetingMatch && ["POST", "PUT"].includes(request.method())) {
      const mutation = {
        method: request.method(),
        id: Number(meetingMatch[1]),
        body: request.postDataJSON(),
        csrf: request.headers()["x-csrf-token"],
      };
      mutations.push(mutation);

      if (onMutation) {
        const result = await onMutation({
          route,
          mutation,
          mutationNumber: mutations.length,
          currentDetail,
          setCurrentDetail: (nextDetail) => {
            currentDetail = structuredClone(nextDetail);
          },
        });
        if (result !== false) return;
      }

      const reunion = {
        id: currentDetail.reunion?.id || 501,
        version: currentDetail.reunion?.version || 0,
        fechaReunion: mutation.body.fechaReunion,
        sedeId: mutation.body.sedeId,
        sedeNombre: "Sede principal",
        observaciones: mutation.body.observaciones,
        fechaCreacion: "2026-11-15T09:00:00",
        fechaActualizacion: "2026-11-15T09:00:00",
      };
      currentDetail = {
        ...currentDetail,
        version: currentDetail.version + 1,
        estadoCodigo: "REUNION_PROGRAMADA",
        estadoNombre: "Reunión programada",
        reunion,
      };
      await fulfillJson(route, request.method() === "POST" ? 201 : 200, reunion);
      return;
    }

    await fulfillJson(route, 200, []);
  });

  return {
    pendingListRequests,
    generalListRequests,
    meetingListRequests,
    mutations,
    getDetailRequests: () => detailRequests,
  };
}

async function openMeetingsTab(page) {
  await page.goto("/conciliaciones");
  await page.getByRole("tab", { name: "Reuniones" }).click();
  await expect(page.getByRole("heading", { name: "Reuniones de conciliación" })).toBeVisible();
}

function sectionByHeading(page, name) {
  return page.locator("section").filter({
    has: page.getByRole("heading", { name, exact: true }),
  });
}

async function fillMeetingForm(page, observations = "Primera reunión programada") {
  await page.locator('input[name="fechaReunion"]').fill("2099-12-20T10:30");
  await page.locator('select[name="sedeId"]').selectOption("3");
  await page.locator('textarea[name="observaciones"]').fill(observations);
}

test("conserva fechas, sedes, filtros y ordenamientos del listado de reuniones", async ({ page }) => {
  const api = await mockMeetingApi(page, {
    detailForId: (id, current) => {
      const meeting = EXISTING_MEETINGS.find((item) => item.conciliacionId === id);
      return meeting ? detailFromMeeting(meeting) : current;
    },
  });
  await openMeetingsTab(page);

  const existingSection = sectionByHeading(page, "Reuniones existentes");
  const principalRow = existingSection.locator("tbody tr").filter({ hasText: "#77" });
  const centerRow = existingSection.locator("tbody tr").filter({ hasText: "#88" });

  await expect(principalRow).toContainText("Sede principal");
  await expect(principalRow).toContainText("2099");
  await expect(centerRow).toContainText("Sede centro");
  await expect(centerRow).toContainText("2099");
  await expect(existingSection.getByText("Sin programar")).toHaveCount(0);
  expect(api.getDetailRequests()).toBe(0);
  expect(api.meetingListRequests[0]).toMatchObject({
    page: "1",
    size: "10",
    sortBy: "fechaReunion",
    direction: "desc",
  });

  await existingSection
    .getByRole("combobox", { name: "Ordenar reuniones existentes por" })
    .selectOption("sede");
  await existingSection.getByLabel("Fecha de reunión desde").fill("2099-01-01");
  await existingSection.getByLabel("Fecha de reunión hasta").fill("2099-12-31");

  await expect.poll(() =>
    api.meetingListRequests.some((request) =>
      request.sortBy === "sede" &&
      request.fechaDesde === "2099-01-01" &&
      request.fechaHasta === "2099-12-31"
    )
  ).toBe(true);
  expect(api.pendingListRequests.every((request) => request.sortBy === "id")).toBe(true);
  expect(api.pendingListRequests.every((request) => request.estado === "ESPERANDO_REUNION")).toBe(true);

  await principalRow.getByRole("button", { name: "Ver reunión" }).click();
  await expect(page.getByRole("heading", { name: "Reunión de conciliación #77" })).toBeVisible();
  await centerRow.getByRole("button", { name: "Ver reunión" }).click();
  const secondDetail = page.locator("section").filter({
    has: page.getByRole("heading", { name: "Reunión de conciliación #88" }),
  });
  await expect(secondDetail).toContainText("Sede centro");
  await expect(secondDetail).toContainText("Reunión finalizada");
  await expect(secondDetail).not.toContainText("Reunión principal");
});

test("una conciliación no elegible no presenta una acción válida de programación", async ({ page }) => {
  const api = await mockMeetingApi(page, {
    pendingListForPage: () => pageOf([{
      ...PENDING_SUMMARY,
      id: 66,
      estadoCodigo: "EN_ESPERA",
      estadoNombre: "En espera",
      estudianteId: null,
      estudianteNombre: null,
    }]),
    meetingListForPage: () => pageOf([]),
  });
  await openMeetingsTab(page);

  const pendingSection = sectionByHeading(page, "Primeras reuniones pendientes");
  await expect(pendingSection.getByRole("button", { name: "No elegible" })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Programar reunión" })).toHaveCount(0);
  expect(api.getDetailRequests()).toBe(0);
  expect(api.pendingListRequests.every((request) => request.estado === "ESPERANDO_REUNION")).toBe(true);
});

test("programa la primera reunión desde una conciliación pendiente paginada", async ({ page }) => {
  const api = await mockMeetingApi(page);
  await openMeetingsTab(page);

  const pendingSection = sectionByHeading(page, "Primeras reuniones pendientes");
  const existingSection = sectionByHeading(page, "Reuniones existentes");
  await expect(pendingSection.getByText("#42")).toHaveCount(0);
  await pendingSection.getByRole("button", { name: "›", exact: true }).click();
  await expect(pendingSection.getByText("#42", { exact: true })).toBeVisible();
  await expect.poll(() => api.pendingListRequests.some((request) => request.page === "2")).toBe(true);
  expect(api.pendingListRequests.every((request) => request.sortBy === "id")).toBe(true);
  expect(api.pendingListRequests.every((request) => request.estado === "ESPERANDO_REUNION")).toBe(true);
  expect(api.meetingListRequests.every((request) => request.page === "1")).toBe(true);

  await pendingSection.getByRole("button", { name: "Ver y programar" }).click();
  await expect(page.getByText("Esta conciliación aún no tiene reunión programada.")).toBeVisible();
  await fillMeetingForm(page);
  const pendingRequestsBeforeSave = api.pendingListRequests.length;
  const meetingRequestsBeforeSave = api.meetingListRequests.length;
  await page.getByRole("button", { name: "Programar reunión" }).click();

  await expect.poll(() => api.mutations.length).toBe(1);
  expect(api.mutations[0]).toEqual({
    method: "POST",
    id: 42,
    csrf: "csrf-reuniones",
    body: {
      fechaReunion: "2099-12-20T10:30:00",
      sedeId: 3,
      observaciones: "Primera reunión programada",
      conciliacionVersion: 7,
    },
  });
  expect(api.mutations[0].body).not.toHaveProperty("version");
  const meetingsPanel = page.getByRole("tabpanel", { name: "Reuniones" });
  await expect(meetingsPanel.getByText("La reunión fue programada correctamente.")).toBeVisible();
  await expect(
    meetingsPanel.getByRole("paragraph").filter({ hasText: "Primera reunión programada" }),
  ).toBeVisible();
  await expect(meetingsPanel.locator("p", { hasText: "Sede principal" })).toBeVisible();
  await expect.poll(() => api.pendingListRequests.length).toBeGreaterThan(pendingRequestsBeforeSave);
  await expect.poll(() => api.meetingListRequests.length).toBeGreaterThan(meetingRequestsBeforeSave);
  await expect(pendingSection.getByText("#42", { exact: true })).toHaveCount(0);
  const createdRow = existingSection.locator("tbody tr").filter({ hasText: "#42" });
  await expect(createdRow).toContainText("Sede principal");
  await expect(createdRow).toContainText("2099");
});

test("conserva la reprogramación existente mediante PUT y ambas versiones", async ({ page }) => {
  const scheduledSummary = {
    ...PENDING_SUMMARY,
    id: 77,
    version: 9,
    estadoCodigo: "REUNION_PROGRAMADA",
    estadoNombre: "Reunión programada",
  };
  const api = await mockMeetingApi(page, {
    initialDetail: detailOf(scheduledSummary, {
      id: 707,
      version: 3,
      fechaReunion: "2099-11-10T09:00:00",
      sedeId: 3,
      sedeNombre: "Sede principal",
      observaciones: "Fecha original",
      fechaCreacion: "2026-11-01T08:00:00",
      fechaActualizacion: "2026-11-01T08:00:00",
    }),
    pendingListForPage: () => pageOf([]),
    meetingListForPage: () => pageOf([EXISTING_MEETINGS[0]]),
  });
  await openMeetingsTab(page);
  await sectionByHeading(page, "Reuniones existentes")
    .getByRole("button", { name: "Ver reunión" })
    .click();
  await fillMeetingForm(page, "Reprogramación confirmada");
  await page.getByRole("button", { name: "Reprogramar reunión" }).click();

  await expect.poll(() => api.mutations.length).toBe(1);
  expect(api.mutations[0]).toMatchObject({ method: "PUT", id: 77 });
  expect(api.mutations[0].body).toMatchObject({
    conciliacionVersion: 9,
    version: 3,
    observaciones: "Reprogramación confirmada",
  });
});

test("un usuario sin permiso de programación no puede ejecutar la acción", async ({ page }) => {
  const api = await mockMeetingApi(page, {
    permissions: VIEW_PERMISSIONS,
    pendingListForPage: () => pageOf([PENDING_SUMMARY]),
    meetingListForPage: () => pageOf([]),
  });
  await openMeetingsTab(page);
  await sectionByHeading(page, "Primeras reuniones pendientes")
    .getByRole("button", { name: "Ver y programar" })
    .click();

  await expect(page.getByText(/No tienes permisos para programar esta reunión/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Programar reunión" })).toHaveCount(0);
  expect(api.mutations).toHaveLength(0);
});

test("un conflicto conserva el formulario, actualiza la versión y no reintenta ni duplica", async ({ page }) => {
  const refreshedDetail = { ...detailOf(PENDING_SUMMARY), version: 8 };
  const api = await mockMeetingApi(page, {
    pendingListForPage: () => pageOf([PENDING_SUMMARY]),
    meetingListForPage: () => pageOf([]),
    onMutation: async ({ route, mutationNumber, setCurrentDetail }) => {
      if (mutationNumber === 1) {
        await new Promise((resolve) => setTimeout(resolve, 150));
        setCurrentDetail(refreshedDetail);
        await fulfillJson(route, 409, {
          estado: 409,
          mensaje: "La conciliación fue modificada por otra transacción",
          correlacionId: "conflict-381",
        });
        return true;
      }
      return false;
    },
  });
  await openMeetingsTab(page);
  await sectionByHeading(page, "Primeras reuniones pendientes")
    .getByRole("button", { name: "Ver y programar" })
    .click();
  await fillMeetingForm(page, "Borrador que debe conservarse");
  await page.getByRole("button", { name: "Programar reunión" }).dblclick();

  await expect(page.getByText(/La versión actual del servidor se cargó sin reemplazar tus campos/)).toBeVisible();
  await expect(page.locator('textarea[name="observaciones"]')).toHaveValue("Borrador que debe conservarse");
  await expect.poll(() => api.mutations.length).toBe(1);
  expect(api.getDetailRequests()).toBeGreaterThanOrEqual(2);

  await page.getByRole("button", { name: "Programar reunión" }).click();
  await expect.poll(() => api.mutations.length).toBe(2);
  expect(api.mutations[1].body.conciliacionVersion).toBe(8);
});

test("un error HTTP conserva el formulario y no produce envíos duplicados", async ({ page }) => {
  const api = await mockMeetingApi(page, {
    pendingListForPage: () => pageOf([PENDING_SUMMARY]),
    meetingListForPage: () => pageOf([]),
    onMutation: async ({ route }) => {
      await new Promise((resolve) => setTimeout(resolve, 150));
      await fulfillJson(route, 503, {
        estado: 503,
        mensaje: "Servicio temporalmente no disponible",
      });
      return true;
    },
  });
  await openMeetingsTab(page);
  await sectionByHeading(page, "Primeras reuniones pendientes")
    .getByRole("button", { name: "Ver y programar" })
    .click();
  await fillMeetingForm(page, "Borrador después del error");
  await page.getByRole("button", { name: "Programar reunión" }).dblclick();

  await expect(
    page.getByRole("tabpanel", { name: "Reuniones" }).getByText("Servicio temporalmente no disponible"),
  ).toBeVisible();
  await expect(page.locator('textarea[name="observaciones"]')).toHaveValue("Borrador después del error");
  await expect.poll(() => api.mutations.length).toBe(1);
});
