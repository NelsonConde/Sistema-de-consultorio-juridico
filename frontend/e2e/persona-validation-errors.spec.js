const { test, expect } = require("@playwright/test");

const CATALOGOS = {
  "/api/tipos-documento/activos": [
    { codigo: "CC", nombre: "Cédula de ciudadanía" },
  ],
  "/api/tipos-persona": [{ id: 1, nombre: "Persona natural" }],
  "/api/nacionalidades": [{ id: 1, nombre: "Colombiana" }],
  "/api/condiciones": [{ id: 1, nombre: "Ninguna" }],
  "/api/ocupaciones": [{ id: 1, nombre: "Empleado" }],
  "/api/empresas": [{ id: 1, nombre: "Empresa de prueba" }],
  "/api/departamentos": [{ id: 1, nombre: "Norte de Santander" }],
  "/api/municipios/departamento/1": [{ id: 1, nombre: "Cúcuta" }],
  "/api/barrios/municipio/1": [{ id: 1, nombre: "Centro" }],
};

async function mockPersonaApi(page, respuestasPost = []) {
  const cuerpos = [];
  let intento = 0;

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;

    if (path.endsWith("/api/auth/me")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: 1,
          tipoPerfil: "ADMINISTRATIVO",
          permisos: ["Acceder recepción", "Crear personas"],
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
          token: "token-prueba",
        }),
      });
      return;
    }

    if (path.endsWith("/api/personas") && request.method() === "POST") {
      cuerpos.push(request.postDataJSON());
      const respuesta = respuestasPost[intento] ?? {
        status: 201,
        body: { id: 10 },
      };
      intento += 1;

      if (respuesta.abort) {
        await route.abort("connectionfailed");
        return;
      }

      await route.fulfill({
        status: respuesta.status,
        contentType: "application/json",
        headers: respuesta.requestId
            ? {
              "X-Request-ID": respuesta.requestId,
              "Access-Control-Expose-Headers": "X-Request-ID",
            }
            : {},
        body: JSON.stringify(respuesta.body),
      });
      return;
    }

    const catalogo = Object.entries(CATALOGOS).find(([suffix]) =>
      path.endsWith(suffix),
    );
    if (catalogo) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(catalogo[1]),
      });
      return;
    }

    await route.fulfill({
      status: 404,
      contentType: "application/json",
      body: "{}",
    });
  });

  return { cuerpos };
}

async function irASeccion(page, nombre) {
  await page.getByRole("button", { name: nombre, exact: true }).click();
}

async function completarFormulario(page) {
  await page.locator("#tipoPersonaId").selectOption("1");
  await page.locator("#tipoDocumento").selectOption("CC");
  await page.locator("#numeroDocumento").fill("1090123456");
  await page.locator("#fechaExpedicion").fill("2020-01-15");
  await page.locator("#ciudadExpedicion").fill("Cúcuta");
  await page.locator("#nombres").fill("María Fernanda");
  await page.locator("#apellidos").fill("Pérez Gómez");
  await page.locator("#fechaNacimiento").fill("1998-05-20");

  await irASeccion(page, "Identidad");
  await page.locator("#nombreIdentitario").fill("María Fernanda");
  await page.locator("#pronombre").selectOption("Ella");
  await page.locator("#sexo").selectOption("Mujer");
  await page.locator("#genero").selectOption("Femenino");
  await page.locator("#orientacionSexual").selectOption("Heterosexual");
  await page.locator("#nacionalidadId").selectOption("1");
  await page.locator("#estadoCivil").selectOption("Soltero/a");
  await page.locator("#escolaridad").selectOption("Universitario");
  await page.locator("#grupoEtnico").fill("No informa");
  await page.locator("#condicionActualId").selectOption("1");
  await page.locator("#discapacidad").fill("No informa");
  await page.locator("#caracterizacionPcd").fill("No informa");

  await irASeccion(page, "Contacto");
  await page.locator("#telefono").fill("3001234567");

  await irASeccion(page, "Vivienda");
  await page.locator("#departamentoId").selectOption("1");
  await expect(page.locator("#municipioId option")).toHaveCount(2);
  await page.locator("#municipioId").selectOption("1");
  await expect(page.locator("#barrioId option")).toHaveCount(2);
  await page.locator("#barrioId").selectOption("1");
  await page.locator("#direccion").fill("Calle 10 # 5-20");
  await page.locator("#comuna").fill("Comuna 1");
  await page.locator("#localidad").fill("Cúcuta");
  await page.locator("#estrato").fill("2");
  await page.locator("#tipoVivienda").fill("Casa");
  await page.locator("#zona").selectOption("Urbana");
  await page.locator("#tenencia").fill("Arriendo");
  await page.locator("#numeroPersonasACargo").fill("1");

  await irASeccion(page, "Economía");
  await page.locator("#ocupacionId").selectOption("1");
  await page.locator("#empresaId").selectOption("1");
  await page.locator("#salario").fill("1500000");
  await page.locator("#cargo").fill("Auxiliar");
  await page.locator("#direccionEmpresa").fill("Avenida 1 # 2-03");
  await page.locator("#telefonoEmpresa").fill("6071234567");

  await irASeccion(page, "Servicio");
  await page.locator("#comoSeEntero").fill("Universidad");
  await page.locator("#relacionConUniversidad").fill("Egresada");
}

test("presenta errores locales por campo y permite llegar al campo conservando los datos", async ({
  page,
}) => {
  const api = await mockPersonaApi(page);
  await page.goto("/recepcion");
  await completarFormulario(page);

  await irASeccion(page, "Economía");
  await page.locator("#cargo").fill("");
  await irASeccion(page, "Servicio");
  await page.locator("#comoSeEntero").fill("");
  await page.getByRole("button", { name: "Guardar Persona" }).click();

  const resumen = page.locator("#resumen-errores-persona");
  await expect(resumen).toContainText("Cargo");
  await expect(resumen).toContainText("Cómo se enteró de nuestros servicios");
  await expect(
    page.getByRole("button", { name: /Economía, 1 error/ }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /Servicio, 1 error/ }),
  ).toBeVisible();
  expect(api.cuerpos).toHaveLength(0);

  await resumen.getByRole("button", { name: /Cargo/ }).click();
  await expect(page.locator("#cargo")).toBeFocused();
  await expect(page.locator("#salario")).toHaveValue("1500000");
});

test("traduce múltiples errores del backend y no desborda en 360, 768 ni 1280 px", async ({
  page,
}) => {
  await mockPersonaApi(page, [
    {
      status: 400,
      requestId: "persona-validacion-001",
      body: {
        mensaje: "Uno o más campos no son válidos",
        detalles: {
          direccionEmpresa: "El campo es obligatorio",
          relacionConUniversidad: "El campo es obligatorio",
          ocupacionId: "El campo es obligatorio",
          comoSeEntero: "El campo es obligatorio",
          telefonoEmpresa: "El campo es obligatorio",
          empresaId: "El campo es obligatorio",
          cargo: "El campo es obligatorio",
        },
      },
    },
  ]);
  await page.goto("/recepcion");
  await completarFormulario(page);
  await page.getByRole("button", { name: "Guardar Persona" }).click();

  const resumen = page.locator("#resumen-errores-persona");
  await expect(resumen).toContainText("Dirección de la empresa");
  await expect(resumen).toContainText("Relación con la Universidad");
  await expect(resumen).toContainText("Referencia: persona-validacion-001");
  await expect(resumen).not.toContainText("direccionEmpresa");
  await expect(resumen).not.toContainText("ocupacionId");

  for (const width of [360, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    const sinDesbordamiento = await resumen.evaluate(
      (element) => element.scrollWidth <= element.clientWidth,
    );
    expect(sinDesbordamiento).toBe(true);
  }

  await resumen
    .getByRole("button", { name: /Dirección de la empresa/ })
    .click();
  await expect(page.locator("#direccionEmpresa")).toBeFocused();
});

test("permite corregir un único error del servidor y reenviar una sola vez", async ({
  page,
}) => {
  const api = await mockPersonaApi(page, [
    {
      status: 400,
      body: {
        mensaje: "Uno o más campos no son válidos",
        detalles: { cargo: "Revisa el cargo informado" },
      },
    },
    { status: 201, body: { id: 10 } },
  ]);
  await page.goto("/recepcion");
  await completarFormulario(page);
  await page.getByRole("button", { name: "Guardar Persona" }).click();

  const resumen = page.locator("#resumen-errores-persona");
  await expect(resumen).toContainText("Revisa el cargo informado");
  await resumen.getByRole("button", { name: /Cargo/ }).click();
  await page.locator("#cargo").fill("Analista");
  await page.getByRole("button", { name: "Guardar Persona" }).click();

  await expect.poll(() => api.cuerpos.length).toBe(2);
  await expect(page.getByText("Registro exitoso")).toBeVisible();
});

test("conserva los datos ante un error de red y permite reintentar", async ({
  page,
}) => {
  const api = await mockPersonaApi(page, [
    { abort: true },
    { status: 201, body: { id: 10 } },
  ]);
  await page.goto("/recepcion");
  await completarFormulario(page);
  await page.getByRole("button", { name: "Guardar Persona" }).click();

  await expect(page.getByText("Error de conexión")).toBeVisible();
  await expect(page.locator("#relacionConUniversidad")).toHaveValue("Egresada");
  await page.getByRole("button", { name: "Guardar Persona" }).click();

  await expect.poll(() => api.cuerpos.length).toBe(2);
  await expect(page.getByText("Registro exitoso")).toBeVisible();
});
