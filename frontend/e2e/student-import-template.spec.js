const fs = require("node:fs");
const path = require("node:path");
const { inflateRawSync } = require("node:zlib");
const { test, expect } = require("@playwright/test");

const TEMPLATE_FILENAME = "plantilla_importacion_estudiantes.xlsx";
const EXPECTED_HEADERS = [
  "Nombre",
  "TipoDocumentoId",
  "Documento",
  "Email",
  "Telefono",
  "Usuario",
  "SedeId",
  "Codigo",
  "AsesorId",
  "Activo",
  "Conciliacion",
];

function json(route, body, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

function findEndOfCentralDirectory(buffer) {
  const minimumOffset = Math.max(0, buffer.length - 65_557);

  for (let offset = buffer.length - 22; offset >= minimumOffset; offset -= 1) {
    if (buffer.readUInt32LE(offset) === 0x06054b50) return offset;
  }

  throw new Error("El archivo no contiene un directorio central ZIP valido");
}

function readZipEntries(buffer) {
  const endOffset = findEndOfCentralDirectory(buffer);
  const entryCount = buffer.readUInt16LE(endOffset + 10);
  let offset = buffer.readUInt32LE(endOffset + 16);
  const entries = new Map();

  for (let index = 0; index < entryCount; index += 1) {
    if (buffer.readUInt32LE(offset) !== 0x02014b50) {
      throw new Error("Entrada invalida en el directorio central ZIP");
    }

    const method = buffer.readUInt16LE(offset + 10);
    const compressedSize = buffer.readUInt32LE(offset + 20);
    const filenameLength = buffer.readUInt16LE(offset + 28);
    const extraLength = buffer.readUInt16LE(offset + 30);
    const commentLength = buffer.readUInt16LE(offset + 32);
    const localHeaderOffset = buffer.readUInt32LE(offset + 42);
    const filename = buffer.toString(
      "utf8",
      offset + 46,
      offset + 46 + filenameLength,
    );

    if (buffer.readUInt32LE(localHeaderOffset) !== 0x04034b50) {
      throw new Error(`Cabecera local ZIP invalida para ${filename}`);
    }

    const localFilenameLength = buffer.readUInt16LE(localHeaderOffset + 26);
    const localExtraLength = buffer.readUInt16LE(localHeaderOffset + 28);
    const dataOffset =
      localHeaderOffset + 30 + localFilenameLength + localExtraLength;
    const compressed = buffer.subarray(dataOffset, dataOffset + compressedSize);

    if (method === 0) {
      entries.set(filename, compressed);
    } else if (method === 8) {
      entries.set(filename, inflateRawSync(compressed));
    } else {
      throw new Error(`Metodo de compresion ZIP no soportado: ${method}`);
    }

    offset += 46 + filenameLength + extraLength + commentLength;
  }

  return entries;
}

function decodeXmlText(value) {
  return value
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", '"')
    .replaceAll("&apos;", "'")
    .replaceAll("&amp;", "&");
}

function extractBackendHeaders() {
  const servicePath = path.resolve(
    process.cwd(),
    "../backend/app/src/main/java/co/edu/ufps/legal_cases/business/service/perfil/estudiante/EstudianteExcelService.java",
  );
  const source = fs.readFileSync(servicePath, "utf8");
  const declaration = source.match(
    /String\[\]\s+esperados\s*=\s*\{([\s\S]*?)\};/,
  );

  if (!declaration) {
    throw new Error("No se encontro el contrato de encabezados del backend");
  }

  return [...declaration[1].matchAll(/"([^"]+)"/g)].map((match) => match[1]);
}

async function mockStudentImportPage(page) {
  await page.route("**/api/**", async (route) => {
    const url = new URL(route.request().url());

    if (url.pathname.endsWith("/api/auth/me")) {
      await json(route, {
        id: 1,
        username: "admin@ufps.edu.co",
        tipoPerfil: "ADMINISTRATIVO",
        permisos: ["Crear usuarios", "Ver catálogos"],
      });
      return;
    }

    if (url.pathname.endsWith("/api/tipos-documento/activos")) {
      await json(route, [{ id: 1, nombre: "Cedula de ciudadania" }]);
      return;
    }

    if (url.pathname.endsWith("/api/sedes")) {
      await json(route, [{ id: 1, nombre: "Sede principal" }]);
      return;
    }

    if (url.pathname.endsWith("/api/areas")) {
      await json(route, [{ id: 1, nombre: "Derecho privado" }]);
      return;
    }

    await json(route, []);
  });
}

test("descarga una plantilla XLSX vacia compatible con EstudianteExcelService", async ({
  page,
}) => {
  await mockStudentImportPage(page);
  await page.goto("/roles");

  await page.locator('select[name="rol"]').selectOption("estudiantes");
  await page.getByRole("tab", { name: "Cargue masivo" }).click();

  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.getByRole("button", { name: "Descargar plantilla" }).click(),
  ]);

  expect(download.suggestedFilename()).toBe(TEMPLATE_FILENAME);
  const downloadedPath = await download.path();
  expect(downloadedPath).not.toBeNull();
  const workbook = fs.readFileSync(downloadedPath);

  expect(workbook.subarray(0, 2).toString("ascii")).toBe("PK");
  expect(
    workbook.subarray(0, 100).toString("utf8").toLowerCase(),
  ).not.toContain("<html");

  const entries = readZipEntries(workbook);
  expect([...entries.keys()]).toEqual(
    expect.arrayContaining([
      "[Content_Types].xml",
      "xl/workbook.xml",
      "xl/styles.xml",
      "xl/worksheets/sheet1.xml",
    ]),
  );

  const contentTypes = entries.get("[Content_Types].xml").toString("utf8");
  expect(contentTypes).toContain(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml",
  );

  const sheet = entries.get("xl/worksheets/sheet1.xml").toString("utf8");
  const rows = [...sheet.matchAll(/<row\b/g)];
  const headers = [...sheet.matchAll(/<t(?:\s[^>]*)?>([\s\S]*?)<\/t>/g)].map(
    (match) => decodeXmlText(match[1]),
  );
  const backendHeaders = extractBackendHeaders();

  expect(rows).toHaveLength(1);
  expect(backendHeaders).toEqual(EXPECTED_HEADERS);
  expect(headers).toEqual(backendHeaders);
  expect(sheet.match(/<col\b/g) ?? []).toHaveLength(11);
  expect(sheet).toMatch(/<pane\b[^>]*ySplit="1"[^>]*state="frozen"/);
  expect(sheet).toContain('autoFilter ref="A1:K1"');
});
