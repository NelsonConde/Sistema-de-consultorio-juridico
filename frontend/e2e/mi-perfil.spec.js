const { test, expect } = require("@playwright/test")

const PERFILES = [
  "ADMINISTRATIVO",
  "ASESOR",
  "MONITOR",
  "ESTUDIANTE",
  "CONCILIADOR",
]

function perfilFixture(tipoPerfil, email = `${tipoPerfil.toLowerCase()}@prueba.local`) {
  return {
    username: `${tipoPerfil.toLowerCase()}.prueba`,
    rolNombre: `Rol ${tipoPerfil}`,
    tipoPerfil,
    nombre: `Usuario ${tipoPerfil}`,
    email,
    telefono: "+57 300 000 0000",
    sede: "Sede Central",
    codigo: `COD-${tipoPerfil}`,
  }
}

async function mockPerfilApi(page, tipoPerfil) {
  let actual = perfilFixture(tipoPerfil)
  const patchBodies = []
  const profileRequests = []

  await page.route("**/api/**", async (route) => {
    const request = route.request()
    const url = new URL(request.url())

    if (url.pathname.endsWith("/api/auth/me")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ id: 1, tipoPerfil, permisos: [] }),
      })
      return
    }

    if (url.pathname.endsWith("/api/auth/csrf")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "test-token" }),
      })
      return
    }

    if (url.pathname.endsWith("/api/mi-perfil")) {
      profileRequests.push({ method: request.method(), path: url.pathname })
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(actual),
      })
      return
    }

    if (url.pathname.endsWith("/api/mi-perfil/contacto")) {
      const body = request.postDataJSON()
      patchBodies.push(body)
      actual = { ...actual, email: body.email, telefono: body.telefono }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(actual),
      })
      return
    }

    await route.fulfill({ status: 404, contentType: "application/json", body: "{}" })
  })

  return { patchBodies, profileRequests, getActual: () => actual }
}

test.describe("Autogestión de perfil y protección de datos", () => {
  for (const tipoPerfil of PERFILES) {
    test(`Perfil ${tipoPerfil}: lectura y actualización autorizada de contacto`, async ({ page }) => {
      const api = await mockPerfilApi(page, tipoPerfil)

      await page.goto("/mi-perfil")

      // Verifica visualización de atributos de solo lectura
      await expect(page.getByText(`Usuario ${tipoPerfil}`)).toBeVisible()
      await expect(page.getByText(`COD-${tipoPerfil}`)).toBeVisible()
      await expect(page.getByText("Sede Central")).toBeVisible()

      // Edita contacto propio
      const nuevoEmail = `${tipoPerfil.toLowerCase()}.nuevo@prueba.local`
      const nuevoTelefono = "+57 310 987 6543"

      await page.getByLabel("Correo electrónico").fill(nuevoEmail)
      await page.getByLabel("Teléfono").fill(nuevoTelefono)
      await page.getByRole("button", { name: "Guardar cambios" }).click()

      // Verifica que solo email y teléfono fueron enviados
      await expect.poll(() => api.patchBodies.length).toBe(1)
      expect(api.patchBodies[0]).toEqual({
        email: nuevoEmail,
        telefono: nuevoTelefono,
      })

      // Verifica que atributos protegidos de solo lectura no fueron alterados
      expect(api.getActual().codigo).toBe(`COD-${tipoPerfil}`)
      expect(api.getActual().sede).toBe("Sede Central")
      expect(api.getActual().tipoPerfil).toBe(tipoPerfil)
    })
  }

  test("Validación de formulario: bloquea correo y teléfono inválidos antes de enviar", async ({ page }) => {
    const api = await mockPerfilApi(page, "ESTUDIANTE")
    await page.goto("/mi-perfil")

    await page.getByLabel("Correo electrónico").fill("correo-invalido-sin-dominio")
    await page.getByLabel("Teléfono").fill("abc-sin-numeros")
    await page.getByRole("button", { name: "Guardar cambios" }).click()

    await expect(page.getByText("Ingrese un correo electrónico válido")).toBeVisible()
    await expect(page.getByText("Ingresa un teléfono válido")).toBeVisible()
    expect(api.patchBodies).toHaveLength(0)
  })

  test("Protección de atributos: los campos protegidos no tienen inputs editables", async ({ page }) => {
    await mockPerfilApi(page, "ADMINISTRATIVO")
    await page.goto("/mi-perfil")

    // Verificar que campos protegidos no son editables ni existen como inputs
    await expect(page.locator("input[name='id']")).toHaveCount(0)
    await expect(page.locator("input[name='rol']")).toHaveCount(0)
    await expect(page.locator("input[name='perfil']")).toHaveCount(0)
    await expect(page.locator("input[name='documento']")).toHaveCount(0)
    await expect(page.locator("input[name='codigo']")).toHaveCount(0)
    await expect(page.locator("input[name='sede']")).toHaveCount(0)
    await expect(page.locator("input[name='activo']")).toHaveCount(0)

    // Solo email y teléfono son inputs del formulario de contacto
    await expect(page.locator("input[name='email']")).toBeVisible()
    await expect(page.locator("input[name='telefono']")).toBeVisible()
  })
})
