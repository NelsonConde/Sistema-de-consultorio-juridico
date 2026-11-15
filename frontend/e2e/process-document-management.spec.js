const { test, expect } = require("@playwright/test");

const PROCESS_ID = 50;
const DOCUMENT_ID = "11111111-1111-1111-1111-111111111111";

function pageOf(content) {
  return { content, page: 1, size: 10, totalElements: content.length, totalPages: content.length ? 1 : 0 };
}

function processSummary() {
  return {
    id: PROCESS_ID,
    numeroRadicado: "54001310500120260000101",
    departamentoNombre: "Norte de Santander",
    consultaId: 101,
    consulta: "Consulta documental",
    organoControlNombre: "Juzgado civil",
    especialidadNombre: "Civil",
    estado: "PENDIENTE",
    version: 3,
  };
}

function documentVersion(version, overrides = {}) {
  return {
    id: 7000 + version,
    documentoLogico: DOCUMENT_ID,
    version,
    tipoDocumental: "PROCESO_DOCUMENTO",
    origen: "CARGA_USUARIO",
    referenciaAnteriorId: version > 1 ? 7000 + version - 1 : null,
    fileName: version > 1 ? `demanda-v${version}.pdf` : "demanda.pdf",
    size: 2048 + version,
    contentType: "application/pdf",
    status: version > 1 ? "VIGENTE" : "HISTORICO",
    createdAt: `2026-11-${10 + version}T10:00:00`,
    ...overrides,
  };
}

async function mockProcessDocumentsApi(page, options = {}) {
  const state = {
    currentFiles: options.initialFiles ? [...options.initialFiles] : [],
    versions: options.initialVersions ? [...options.initialVersions] : [],
    initiations: [],
    uploadInitiations: {},
    completions: 0,
    completionUploadIds: [],
    storageUploads: 0,
    cancellations: [],
    deletes: 0,
    downloadDescriptors: 0,
    deleteStatus: options.deleteStatus || null,
  };
  let uploadCounter = 0;
  let confirmationFailures = options.confirmationFailures || 0;

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;

    if (path.endsWith("/api/auth/me")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: 5,
          username: "asesor.documentos",
          tipoPerfil: "ASESOR",
          permisos: options.readOnly
            ? ["Acceder procesos", "Ver procesos"]
            : ["Acceder procesos", "Ver procesos", "Gestionar procesos"],
        }),
      });
      return;
    }

    if (path.endsWith("/api/auth/csrf")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "csrf-process-documents" }),
      });
      return;
    }

    if (path === "/api/procesos" && request.method() === "GET") {
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(pageOf([processSummary()])) });
      return;
    }

    if (path === `/api/procesos/${PROCESS_ID}/archivos` && request.method() === "GET") {
      if (options.listStatus) {
        await route.fulfill({ status: options.listStatus, contentType: "application/json", body: JSON.stringify({ mensaje: "Acceso denegado" }) });
        return;
      }
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(state.currentFiles) });
      return;
    }

    if (path === `/api/procesos/${PROCESS_ID}/archivos/uploads` && request.method() === "POST") {
      const body = request.postDataJSON();
      state.initiations.push(body);
      if (options.initiationStatus) {
        await route.fulfill({
          status: options.initiationStatus,
          contentType: "application/json",
          body: JSON.stringify({ mensaje: "Archivo no permitido para este proceso" }),
        });
        return;
      }
      uploadCounter += 1;
      const uploadId = `00000000-0000-0000-0000-00000000000${uploadCounter}`;
      state.uploadInitiations[uploadId] = body;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          uploadId,
          uploadUrl: `https://storage.test/upload-${uploadCounter}`,
          expiresAt: "2026-11-15T18:00:00Z",
        }),
      });
      return;
    }

    if (/\/api\/file-uploads\/.+\/complete$/.test(path) && request.method() === "POST") {
      state.completions += 1;
      const uploadId = path.match(/\/api\/file-uploads\/([^/]+)\/complete$/)[1];
      state.completionUploadIds.push(uploadId);
      if (options.deferConfirmation) {
        await new Promise((resolve) => {
          state.releaseConfirmation = resolve;
        });
      }

      if (confirmationFailures > 0) {
        confirmationFailures -= 1;
        await route.fulfill({ status: 503, contentType: "application/json", body: JSON.stringify({ mensaje: "Confirmación temporalmente no disponible" }) });
        return;
      }

      const initiation = state.uploadInitiations[uploadId];
      const nextVersion = initiation.documentoLogico ? 2 : 1;
      if (nextVersion === 2) {
        state.versions = state.versions.map((file) => ({ ...file, status: "HISTORICO" }));
      }
      const created = documentVersion(nextVersion, {
        id: 7000 + nextVersion,
        documentoLogico: initiation.documentoLogico || DOCUMENT_ID,
        fileName: initiation.fileName,
        status: "VIGENTE",
      });
      state.versions = [created, ...state.versions.filter((file) => file.id !== created.id)];
      state.currentFiles = [created];
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(created) });
      return;
    }

    if (/\/api\/file-uploads\/[^/]+$/.test(path) && request.method() === "DELETE") {
      state.cancellations.push(path.match(/\/api\/file-uploads\/([^/]+)$/)[1]);
      if (options.cancelStatus) {
        await route.fulfill({ status: options.cancelStatus, contentType: "application/json", body: JSON.stringify({ mensaje: "No se pudo cancelar la carga" }) });
        return;
      }
      await route.fulfill({ status: 204, body: "" });
      return;
    }

    if (path === `/api/documentos/${DOCUMENT_ID}/versiones` && request.method() === "GET") {
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(state.versions) });
      return;
    }

    if (/\/api\/archivos\/\d+\/download$/.test(path) && request.method() === "GET") {
      state.downloadDescriptors += 1;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ fileName: "demanda.pdf", downloadUrl: "https://storage.test/download" }),
      });
      return;
    }

    if (/\/api\/archivos\/\d+$/.test(path) && request.method() === "DELETE") {
      state.deletes += 1;
      if (state.deleteStatus) {
        await route.fulfill({ status: state.deleteStatus, contentType: "application/json", body: JSON.stringify({ mensaje: "No se pudo eliminar el archivo" }) });
        return;
      }
      state.currentFiles = [];
      await route.fulfill({ status: 204, body: "" });
      return;
    }

    await route.fulfill({ status: 404, contentType: "application/json", body: JSON.stringify({ mensaje: "Ruta sintética no configurada" }) });
  });

  await page.route("https://storage.test/upload-*", async (route) => {
    state.storageUploads += 1;
    if (options.transferStatus) {
      await route.fulfill({ status: options.transferStatus, body: "transfer failed" });
      return;
    }
    await route.fulfill({ status: 200, body: "" });
  });

  await page.route("https://storage.test/download", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/pdf", body: "%PDF-test" });
  });

  return state;
}

async function openDocuments(page) {
  await page.goto("/procesos");
  await expect(page.getByText("Consulta documental")).toBeVisible();
  await page.getByRole("button", { name: "Documentos" }).click();
  await expect(page.getByRole("dialog", { name: `Documentos del proceso #${PROCESS_ID}` })).toBeVisible();
}

async function selectPdf(page, name) {
  await page.locator('input[type="file"]').setInputFiles({
    name,
    mimeType: "application/pdf",
    lastModified: 1_700_000_000_000,
    buffer: Buffer.from("%PDF-synthetic-process-document"),
  });
}

test.describe("H-03: gestión documental de procesos", () => {
  test("carga, descarga y versiona sin sobrescribir el historial", async ({ page }) => {
    const state = await mockProcessDocumentsApi(page);
    await openDocuments(page);

    await selectPdf(page, "demanda.pdf");
    await page.getByRole("button", { name: "Cargar archivos" }).click();
    await expect.poll(() => state.completions).toBe(1);
    await expect.poll(() => state.currentFiles.length).toBe(1);

    expect(state.initiations).toHaveLength(1);
    expect(state.initiations[0]).toEqual({
      fileName: "demanda.pdf",
      size: 31,
      contentType: "application/pdf",
      tipoDocumental: "PROCESO_DOCUMENTO",
    });
    expect(state.storageUploads).toBe(1);
    expect(state.completions).toBe(1);

    await page.getByRole("button", { name: "Descargar", exact: true }).first().click();
    await expect.poll(() => state.downloadDescriptors).toBe(1);

    await page.getByRole("button", { name: "Nueva versión" }).click();
    await selectPdf(page, "demanda-v2.pdf");
    await page.getByRole("button", { name: "Cargar nueva versión" }).click();
    await expect.poll(() => state.completions).toBe(2);
    await expect(page.getByText("demanda-v2.pdf", { exact: true }).first()).toBeVisible();

    expect(state.initiations).toHaveLength(2);
    expect(state.initiations[1]).toMatchObject({
      fileName: "demanda-v2.pdf",
      documentoLogico: DOCUMENT_ID,
      tipoDocumental: "PROCESO_DOCUMENTO",
    });

    await page.getByRole("button", { name: "Historial" }).click();
    await expect(page.getByText(/Versión 2 · demanda-v2\.pdf · VIGENTE/)).toBeVisible();
    await expect(page.getByText(/Versión 1 · demanda\.pdf · HISTORICO/)).toBeVisible();
    await expect(page.getByText(/Versión 1 · demanda\.pdf · HISTORICO/).locator("..").getByRole("button", { name: "Descargar" })).toBeDisabled();
  });

  test("reintenta únicamente la confirmación y evita transferir dos veces", async ({ page }) => {
    const state = await mockProcessDocumentsApi(page, { confirmationFailures: 1 });
    await openDocuments(page);

    await selectPdf(page, "memorial.pdf");
    await page.getByRole("button", { name: "Cargar archivos" }).click();
    await expect.poll(() => state.completions).toBe(1);
    await expect(page.getByRole("button", { name: "Reintentar confirmación" })).toBeVisible();
    const originalUploadId = state.completionUploadIds[0];
    expect(state.initiations).toHaveLength(1);
    expect(state.storageUploads).toBe(1);
    await expect(page.getByRole("button", { name: "Cargar archivos" })).toBeDisabled();

    await selectPdf(page, "memorial.pdf");
    await expect(page.getByText(/confirmación pendiente/i).first()).toBeVisible();
    await expect(page.getByRole("button", { name: "Cargar archivos" })).toBeDisabled();
    expect(state.initiations).toHaveLength(1);
    expect(state.storageUploads).toBe(1);

    await selectPdf(page, "otro-memorial.pdf");
    await page.getByRole("button", { name: "Cargar archivos" }).click();
    await expect.poll(() => state.completions).toBe(2);
    expect(state.initiations.map((initiation) => initiation.fileName)).toEqual([
      "memorial.pdf",
      "otro-memorial.pdf",
    ]);
    expect(state.storageUploads).toBe(2);
    const otherUploadId = state.completionUploadIds[1];

    await page.getByRole("button", { name: "Reintentar confirmación" }).click();
    await expect.poll(() => state.completions).toBe(3);
    await expect.poll(() => state.currentFiles.length).toBe(1);
    expect(state.initiations).toHaveLength(2);
    expect(state.storageUploads).toBe(2);
    expect(state.completions).toBe(3);
    expect(state.completionUploadIds).toEqual([originalUploadId, otherUploadId, originalUploadId]);

    await expect(page.getByRole("button", { name: "Cerrar documentos" })).toBeEnabled();
    await page.getByRole("button", { name: "Cerrar documentos" }).click();
    await expect(page.getByRole("dialog", { name: `Documentos del proceso #${PROCESS_ID}` })).toHaveCount(0);
  });

  test("no permite cancelar durante la confirmación en curso", async ({ page }) => {
    const state = await mockProcessDocumentsApi(page, { deferConfirmation: true });
    await openDocuments(page);

    await selectPdf(page, "confirmacion-en-curso.pdf");
    await page.getByRole("button", { name: "Cargar archivos" }).click();
    await expect.poll(() => state.completions).toBe(1);
    await expect(page.getByText("Confirmando…")).toBeVisible();
    await expect(page.getByRole("button", { name: "Cancelar carga" })).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Cerrar documentos" })).toBeDisabled();

    state.releaseConfirmation();
    await expect.poll(() => state.currentFiles.length).toBe(1);
    await expect(page.getByText("Carga confirmada")).toBeVisible();
    expect(state.cancellations).toHaveLength(0);
    expect(state.storageUploads).toBe(1);
  });

  test("bloquea el cierre de una sesión pendiente y permite cerrarlo después de cancelar", async ({ page }) => {
    const state = await mockProcessDocumentsApi(page, { confirmationFailures: 1 });
    await openDocuments(page);

    await selectPdf(page, "memorial-pendiente.pdf");
    await page.getByRole("button", { name: "Cargar archivos" }).click();
    await expect.poll(() => state.completions).toBe(1);
    const originalUploadId = state.completionUploadIds[0];

    await page.getByRole("button", { name: "Cerrar documentos" }).click();
    await expect(page.getByRole("status")).toContainText("No puedes cerrar mientras haya una carga pendiente");
    await expect(page.getByRole("dialog", { name: `Documentos del proceso #${PROCESS_ID}` })).toBeVisible();
    expect(state.completionUploadIds).toEqual([originalUploadId]);

    await page.getByRole("button", { name: "Cancelar carga" }).click();
    await expect.poll(() => state.cancellations.length).toBe(1);
    expect(state.cancellations).toEqual([originalUploadId]);
    await expect(page.getByText("Carga cancelada")).toBeVisible();
    await expect(page.getByRole("button", { name: "Reintentar confirmación" })).toHaveCount(0);

    await selectPdf(page, "memorial-pendiente.pdf");
    await page.getByRole("button", { name: "Cargar archivos" }).click();
    await expect.poll(() => state.completions).toBe(2);
    expect(state.initiations).toHaveLength(2);
    expect(state.storageUploads).toBe(2);
    await expect(page.getByText("Carga confirmada")).toBeVisible();

    await page.getByRole("button", { name: "Cerrar documentos" }).click();
    await expect(page.getByRole("dialog", { name: `Documentos del proceso #${PROCESS_ID}` })).toHaveCount(0);
  });

  test("conserva la vista ante error y la actualiza después de eliminar con éxito", async ({ page }) => {
    const current = documentVersion(1, { status: "VIGENTE" });
    const state = await mockProcessDocumentsApi(page, {
      initialFiles: [current],
      initialVersions: [current],
      deleteStatus: 500,
    });
    await openDocuments(page);

    await page.getByRole("button", { name: "Eliminar archivo" }).first().click();
    await page.getByRole("button", { name: "Eliminar archivo" }).last().click();

    await expect(page.getByText("No se pudo eliminar el archivo", { exact: true })).toBeVisible();
    await expect(page.getByText("demanda.pdf", { exact: true })).toBeVisible();
    expect(state.deletes).toBe(1);

    state.deleteStatus = null;
    await page.getByRole("button", { name: "Eliminar archivo" }).last().click();
    await expect(page.getByText("No hay documentos vigentes para este proceso.")).toBeVisible();
    expect(state.deletes).toBe(2);
  });

  test("un usuario de solo lectura consulta y descarga, pero no puede mutar documentos", async ({ page }) => {
    const current = documentVersion(1, { status: "VIGENTE" });
    const state = await mockProcessDocumentsApi(page, {
      readOnly: true,
      initialFiles: [current],
      initialVersions: [current],
    });
    await openDocuments(page);

    await expect(page.getByText("demanda.pdf", { exact: true })).toBeVisible();
    await expect(page.getByLabel("Carga documental")).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Nueva versión" })).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Eliminar archivo" })).toHaveCount(0);
    await page.getByRole("button", { name: "Descargar", exact: true }).click();
    expect(state.initiations).toHaveLength(0);
    expect(state.deletes).toBe(0);
  });

  test("un 403 al listar no se representa como expediente vacío", async ({ page }) => {
    await mockProcessDocumentsApi(page, { listStatus: 403 });
    await openDocuments(page);

    await expect(page.getByText("No tienes autorización para realizar esta operación documental.")).toBeVisible();
    await expect(page.getByText("No hay documentos vigentes para este proceso.")).toHaveCount(0);
  });

  test("un rechazo al iniciar conserva el archivo para reintentar y no transfiere bytes", async ({ page }) => {
    const state = await mockProcessDocumentsApi(page, { initiationStatus: 413 });
    await openDocuments(page);

    await selectPdf(page, "archivo-no-permitido.pdf");
    await page.getByRole("button", { name: "Cargar archivos" }).click();

    await expect(page.getByText("Archivo no permitido para este proceso", { exact: true }).first()).toBeVisible();
    await expect(page.getByText("archivo-no-permitido.pdf", { exact: true }).first()).toBeVisible();
    expect(state.initiations).toHaveLength(1);
    expect(state.storageUploads).toBe(0);
    expect(state.completions).toBe(0);
  });

  test("un fallo de transferencia no confirma ni muestra el archivo como vigente", async ({ page }) => {
    const state = await mockProcessDocumentsApi(page, { transferStatus: 500 });
    await openDocuments(page);

    await selectPdf(page, "transferencia-fallida.pdf");
    await page.getByRole("button", { name: "Cargar archivos" }).click();

    await expect(page.getByText(/No se pudo transferir el archivo al almacenamiento/).first()).toBeVisible();
    await expect(page.getByText("No hay documentos vigentes para este proceso.")).toBeVisible();
    expect(state.initiations).toHaveLength(1);
    expect(state.storageUploads).toBe(1);
    expect(state.completions).toBe(0);
  });
});
