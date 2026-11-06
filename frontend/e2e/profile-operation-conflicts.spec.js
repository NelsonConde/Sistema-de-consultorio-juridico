const { test, expect } = require("@playwright/test")

const PROFILE_CONFLICT_CODE = "CONFLICTO_OPERACION_PERFIL"

const PROFILE_SECTIONS = [
  { id: "administrativos", label: "Administrativos" },
  { id: "asesores", label: "Asesores" },
  { id: "estudiantes", label: "Estudiantes" },
  { id: "monitores", label: "Monitores" },
  { id: "conciliadores", label: "Conciliadores" },
]

const ELIMINATION_PERMISSIONS = [
  "Cambiar estado personas",
  "Cambiar estado usuarios",
  "Cambiar estado estudiantes",
  "Cambiar estado consultas",
  "Archivar consultas",
]

function pageOf(content) {
  return {
    content,
    page: 1,
    size: 10,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
  }
}

function profileConflict(message) {
  return {
    estado: 409,
    mensaje: message,
    detalles: { codigo: PROFILE_CONFLICT_CODE },
  }
}

async function fulfillJson(route, status, body) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  })
}

async function mockProfileApi(page, handler, permissions = ELIMINATION_PERMISSIONS) {
  await page.route("**/api/**", async (route) => {
    const request = route.request()
    const url = new URL(request.url())

    if (url.pathname.endsWith("/api/auth/me")) {
      await fulfillJson(route, 200, {
        id: 1,
        username: "admin@prueba.local",
        tipoPerfil: "ADMINISTRATIVO",
        permisos: permissions,
      })
      return
    }

    if (url.pathname.endsWith("/api/auth/csrf")) {
      await fulfillJson(route, 200, {
        headerName: "X-CSRF-TOKEN",
        token: "csrf-prueba",
      })
      return
    }

    if (await handler(route, request, url)) return

    await fulfillJson(route, 200, [])
  })
}

for (const section of PROFILE_SECTIONS) {
  test(`${section.label}: la reactivación histórica muestra el conflicto funcional`, async ({ page }) => {
    const backendMessage =
      "No se puede cambiar directamente el estado de un perfil histórico. Utilice el cambio explícito de perfil"

    await mockProfileApi(page, async (route, request, url) => {
      if (
        request.method() === "GET" &&
        url.pathname.endsWith(`/api/${section.id}`)
      ) {
        await fulfillJson(route, 200, pageOf([{
          id: 27,
          nombre: `Perfil ${section.label}`,
          documento: "DOC-27",
          codigo: "COD-27",
          activo: false,
        }]))
        return true
      }

      if (
        request.method() === "PATCH" &&
        url.pathname.endsWith(`/api/${section.id}/27/activo`)
      ) {
        await fulfillJson(route, 409, profileConflict(backendMessage))
        return true
      }

      return false
    })

    await page.goto("/eliminacion")
    await page.getByRole("button", { name: new RegExp(section.label) }).click()
    await expect(page.getByText(`Perfil ${section.label}`)).toBeVisible()

    await page.getByRole("button", { name: "Reactivar" }).first().click()
    await page.getByRole("button", { name: "Reactivar" }).last().click()

    await expect(page.getByText("No se puede reactivar el perfil")).toBeVisible()
    await expect(page.getByText(backendMessage)).toBeVisible()
    await expect(page.getByText("El registro cambió antes de reactivarlo")).toHaveCount(0)
    await expect(page.getByText("Registro reactivado correctamente")).toHaveCount(0)
  })
}

test("un 409 sin código funcional conserva el tratamiento de concurrencia", async ({ page }) => {
  let listRequests = 0

  await mockProfileApi(page, async (route, request, url) => {
    if (
      request.method() === "GET" &&
      url.pathname.endsWith("/api/estudiantes")
    ) {
      listRequests += 1
      await fulfillJson(route, 200, pageOf([{
        id: 27,
        nombre: "Estudiante concurrente",
        documento: "DOC-27",
        codigo: "EST-27",
        activo: false,
      }]))
      return true
    }

    if (
      request.method() === "PATCH" &&
      url.pathname.endsWith("/api/estudiantes/27/activo")
    ) {
      await fulfillJson(route, 409, {
        estado: 409,
        mensaje: "El registro fue modificado por otra transacción",
      })
      return true
    }

    return false
  })

  await page.goto("/eliminacion")
  await page.getByRole("button", { name: /Estudiantes/ }).click()
  await expect(page.getByText("Estudiante concurrente")).toBeVisible()

  await page.getByRole("button", { name: "Reactivar" }).first().click()
  await page.getByRole("button", { name: "Reactivar" }).last().click()

  await expect(page.getByText("El registro cambió antes de reactivarlo")).toBeVisible()
  await expect(page.getByText("No se puede reactivar el perfil")).toHaveCount(0)
  await expect.poll(() => listRequests).toBeGreaterThan(1)
})

const DIRECT_DEACTIVATION_CASES = [
  {
    name: "Estudiantes",
    pagePath: "/estudiantes",
    endpoint: "estudiantes",
    permissions: [
      "Acceder estudiantes",
      "Ver estudiantes",
      "Cambiar estado estudiantes",
    ],
  },
  {
    name: "Asesores",
    pagePath: "/asesoresymonitores",
    endpoint: "asesores",
    permissions: [
      "Acceder asesores y monitores",
      "Ver asesores y monitores",
      "Gestionar asesores y monitores",
    ],
  },
  {
    name: "Monitores",
    pagePath: "/asesoresymonitores",
    endpoint: "monitores",
    permissions: [
      "Acceder asesores y monitores",
      "Ver asesores y monitores",
      "Gestionar asesores y monitores",
    ],
  },
]

for (const profile of DIRECT_DEACTIVATION_CASES) {
  test(`${profile.name}: la desactivación actual explica que debe suspenderse la cuenta`, async ({ page }) => {
    const backendMessage =
      "No se puede desactivar directamente el perfil actual. Suspenda la cuenta o utilice el cambio explícito de perfil"

    await mockProfileApi(page, async (route, request, url) => {
      if (
        request.method() === "GET" &&
        url.pathname.endsWith(`/api/${profile.endpoint}`)
      ) {
        await fulfillJson(route, 200, pageOf([{
          id: 31,
          nombre: `Perfil ${profile.name}`,
          documento: "DOC-31",
          email: "perfil@prueba.local",
          codigo: "COD-31",
          activo: true,
        }]))
        return true
      }

      if (
        request.method() === "PATCH" &&
        url.pathname.endsWith(`/api/${profile.endpoint}/31/activo`)
      ) {
        await fulfillJson(route, 409, profileConflict(backendMessage))
        return true
      }

      return false
    }, profile.permissions)

    await page.goto(profile.pagePath)
    if (profile.endpoint === "monitores") {
      await page.getByRole("button", { name: "Monitores", exact: true }).click()
    }

    await expect(page.getByText(`Perfil ${profile.name}`)).toBeVisible()
    await page.getByRole("button", { name: "Desactivar" }).first().click()
    await page.getByRole("button", { name: "Desactivar" }).last().click()

    await expect(page.getByText("No se puede desactivar el perfil")).toBeVisible()
    await expect(page.getByText(backendMessage)).toBeVisible()
    await expect(page.getByText(`${profile.name.slice(0, -1)} desactivado`)).toHaveCount(0)
  })
}

test("el cambio explícito informa que una cuenta inconsistente requiere revisión", async ({ page }) => {
  const backendMessage =
    "No se puede cambiar de perfil porque la cuenta ya tiene otro perfil operativo activo"

  await mockProfileApi(page, async (route, request, url) => {
    if (request.method() === "GET" && url.pathname.endsWith("/api/roles/activos")) {
      await fulfillJson(route, 200, [{
        id: 44,
        nombre: "Rol Monitor",
        tipoPerfil: "MONITOR",
        activo: true,
      }])
      return true
    }

    if (request.method() === "GET" && url.pathname.endsWith("/api/tipos-documento/activos")) {
      await fulfillJson(route, 200, [{ id: 2, nombre: "Cédula" }])
      return true
    }

    if (request.method() === "GET" && url.pathname.endsWith("/api/sedes")) {
      await fulfillJson(route, 200, [{ id: 3, nombre: "Sede principal" }])
      return true
    }

    if (request.method() === "GET" && url.pathname.endsWith("/api/areas")) {
      await fulfillJson(route, 200, [{ id: 4, nombre: "Área jurídica" }])
      return true
    }

    if (
      request.method() === "GET" &&
      url.pathname.endsWith("/api/usuarios-sistema/10")
    ) {
      await fulfillJson(route, 200, {
        id: 10,
        username: "cuenta@prueba.local",
        rolNombre: "Rol Asesor",
        tipoPerfil: "ASESOR",
        perfilId: 21,
        activo: true,
      })
      return true
    }

    if (
      request.method() === "GET" &&
      url.pathname.endsWith("/api/usuarios-sistema")
    ) {
      await fulfillJson(route, 200, pageOf([{
        id: 10,
        username: "cuenta@prueba.local",
        rolNombre: "Rol Asesor",
        tipoPerfil: "ASESOR",
        perfilId: 21,
        activo: true,
      }]))
      return true
    }

    if (request.method() === "GET" && url.pathname.endsWith("/api/asesores/21")) {
      await fulfillJson(route, 200, {
        id: 21,
        nombre: "Cuenta inconsistente",
        tipoDocumentoId: 2,
        documento: "10990001",
        telefono: "3000000000",
        usuario: "cuenta.inconsistente",
        codigo: "ASE-21",
        sedeId: 3,
        areaId: 4,
      })
      return true
    }

    if (
      request.method() === "PATCH" &&
      url.pathname.endsWith("/api/usuarios-sistema/10/perfil/monitor")
    ) {
      await fulfillJson(route, 409, profileConflict(backendMessage))
      return true
    }

    return false
  }, ["Asignar rol usuarios", "Gestionar administradores"])

  await page.goto("/admin")
  await page.getByRole("tab", { name: "Cambiar Rol" }).click()
  await expect(page.getByText("Cambiar rol y perfil de usuario")).toBeVisible()

  await page.getByRole("button", { name: "Buscar usuario" }).click()
  await expect(page.getByText("cuenta@prueba.local")).toBeVisible()
  await page.getByRole("button", { name: "Seleccionar" }).click()

  await page.locator('select[name="destino"]').selectOption("MONITOR")
  await page.getByLabel("Motivo del cambio").fill("Regularización solicitada")
  await page.getByRole("button", { name: "Cambiar rol y perfil" }).click()

  await expect(page.getByText("No se puede cambiar el perfil")).toBeVisible()
  await expect(page.getByText(new RegExp(backendMessage))).toBeVisible()
  await expect(page.getByText(/Revisa administrativamente los perfiles activos/)).toBeVisible()
  await expect(page.getByText("Perfil y rol actualizados correctamente")).toHaveCount(0)
})
