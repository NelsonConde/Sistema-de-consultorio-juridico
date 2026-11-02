const { test, expect } = require("@playwright/test")

const PERSONA = {
    id: 501,
    nombres: "Persona",
    apellidos: "Prueba",
    tipoDocumento: "CC",
    numeroDocumentoEnmascarado: "******1234",
    activo: true,
}

function json(route, body, status = 200) {
    return route.fulfill({
        status,
        contentType: "application/json",
        body: JSON.stringify(body),
    })
}

async function mockNuevaConsultaApi(
    page,
    { consultationStatuses = [201] } = {},
) {
    const state = {
        authStatus: 200,
        authRequests: 0,
        csrfRequests: 0,
        consultationRequests: 0,
        consultationBodies: [],
    }

    await page.route("**/api/**", async (route) => {
        const request = route.request()
        const url = new URL(request.url())
        const path = url.pathname
        const method = request.method()

        if (path.endsWith("/api/auth/me")) {
            state.authRequests += 1

            if (state.authStatus !== 200) {
                await json(
                    route,
                    { message: "Sesión no disponible" },
                    state.authStatus,
                )
                return
            }

            await json(route, {
                id: 1,
                permisos: [
                    "Acceder nueva consulta",
                    "Crear consultas",
                ],
            })
            return
        }

        if (path.endsWith("/api/auth/csrf")) {
            state.csrfRequests += 1
            await json(route, {
                headerName: "X-CSRF-TOKEN",
                token: `csrf-${state.csrfRequests}`,
            })
            return
        }

        if (path.endsWith("/api/sedes")) {
            await json(route, [{ id: 1, nombre: "Sede Central" }])
            return
        }

        if (path.endsWith("/api/areas")) {
            await json(route, [{ id: 1, nombre: "Derecho Civil" }])
            return
        }

        if (path.endsWith("/api/temas/area/1")) {
            await json(route, [{ id: 10, nombre: "Tema de prueba" }])
            return
        }

        if (path.endsWith("/api/tipos/tema/10")) {
            await json(route, [{ id: 20, nombre: "Tipo de prueba" }])
            return
        }

        if (path.endsWith("/api/personas/activos")) {
            await json(route, {
                content: [PERSONA],
                page: 1,
                size: 10,
                totalElements: 1,
                totalPages: 1,
            })
            return
        }

        if (path.endsWith("/api/consultas") && method === "POST") {
            state.consultationRequests += 1
            state.consultationBodies.push(request.postDataJSON())

            const responseIndex = Math.min(
                state.consultationRequests - 1,
                consultationStatuses.length - 1,
            )
            const status = consultationStatuses[responseIndex]

            if (status === 201) {
                await json(route, { id: 826 }, status)
                return
            }

            await json(route, { message: "Solicitud rechazada" }, status)
            return
        }

        await json(route, {}, 404)
    })

    return state
}

async function completarConsultaMinima(page) {
    await page.goto("/nuevaconsulta")

    await expect(page.locator('input[name="fecha"]')).toBeVisible()
    await page.locator('input[name="fecha"]').fill("2026-10-23")
    await page.locator('input[name="tramite"]').fill("Consulta jurídica CP-26")
    await page.locator('select[name="sedeId"]').selectOption("1")
    await page.locator('select[name="areaId"]').selectOption("1")

    await expect(
        page.locator('select[name="temaId"] option[value="10"]'),
    ).toHaveCount(1)
    await page.locator('select[name="temaId"]').selectOption("10")

    await expect(
        page.locator('select[name="tipoId"] option[value="20"]'),
    ).toHaveCount(1)
    await page.locator('select[name="tipoId"]').selectOption("20")

    await page
        .getByRole("button", { name: /Buscar parte principal/i })
        .click()
    await page.getByRole("button", { name: /Persona Prueba/i }).click()

    await page
        .locator('textarea[name="descripcion"]')
        .fill("Descripción única del borrador CP-26")
    await page.locator('textarea[name="hechos"]').fill("Hechos de prueba")
    await page
        .locator('textarea[name="pretensiones"]')
        .fill("Pretensiones de prueba")
    await page
        .locator('textarea[name="conceptoJuridico"]')
        .fill("Concepto jurídico de prueba")
}

test.describe("CP-26: sesión vencida al crear consulta", () => {
    test("conserva el borrador y exige revalidación antes de un único reenvío explícito", async ({ page }) => {
        const api = await mockNuevaConsultaApi(page, {
            consultationStatuses: [401, 201],
        })

        await completarConsultaMinima(page)
        await page.getByRole("button", { name: "Crear consulta" }).click()

        await expect.poll(() => api.consultationRequests).toBe(1)
        await expect(page).toHaveURL(/\/nuevaconsulta$/)
        await expect(
            page.getByText(
                "Tu sesión venció. Inicia sesión nuevamente para guardar esta consulta",
            ),
        ).toBeVisible()
        await expect(
            page.locator('textarea[name="descripcion"]'),
        ).toHaveValue("Descripción única del borrador CP-26")
        await expect(
            page.getByRole("button", { name: "Crear consulta" }),
        ).toBeDisabled()

        const browserStorage = await page.evaluate(() =>
            JSON.stringify({
                local: { ...window.localStorage },
                session: { ...window.sessionStorage },
            }),
        )
        expect(browserStorage).not.toContain("Descripción única del borrador CP-26")
        expect(browserStorage).not.toContain("Hechos de prueba")

        api.authStatus = 401
        const [loginPage] = await Promise.all([
            page.waitForEvent("popup"),
            page.getByRole("button", { name: "Abrir inicio de sesión" }).click(),
        ])
        await loginPage.waitForLoadState("domcontentloaded")
        expect(new URL(loginPage.url()).pathname).toBe("/")
        await loginPage.close()

        await page
            .getByRole("button", { name: "Volver a comprobar sesión" })
            .click()
        await expect(
            page.getByText(/La sesión aún no está activa/),
        ).toBeVisible()
        expect(api.consultationRequests).toBe(1)

        api.authStatus = 200
        const csrfAntesDeRevalidar = api.csrfRequests

        await page
            .getByRole("button", { name: "Volver a comprobar sesión" })
            .click()

        await expect(page.getByText("Sesión revalidada")).toBeVisible()
        await expect(
            page.getByRole("button", { name: "Reintentar creación" }),
        ).toBeEnabled()
        expect(api.csrfRequests).toBeGreaterThan(csrfAntesDeRevalidar)
        expect(api.consultationRequests).toBe(1)

        await page
            .getByRole("button", { name: "Reintentar creación" })
            .click()

        await expect.poll(() => api.consultationRequests).toBe(2)
        expect(api.consultationBodies).toHaveLength(2)
        expect(api.consultationBodies[1]).toEqual(api.consultationBodies[0])
        await expect(page).toHaveURL(/\/consultasjuridicas\?refresh=/)
    })

    test("mantiene el control de autorización 403", async ({ page }) => {
        const api = await mockNuevaConsultaApi(page, {
            consultationStatuses: [403],
        })

        await completarConsultaMinima(page)
        await page.getByRole("button", { name: "Crear consulta" }).click()

        await expect.poll(() => api.consultationRequests).toBe(1)
        await expect(page).toHaveURL(/\/inicio$/)
    })

    test("mantiene las validaciones y no envía un formulario incompleto", async ({ page }) => {
        const api = await mockNuevaConsultaApi(page)

        await page.goto("/nuevaconsulta")
        await expect(page.locator('input[name="fecha"]')).toBeVisible()
        await page.getByRole("button", { name: "Crear consulta" }).click()

        expect(api.consultationRequests).toBe(0)
        await expect(page).toHaveURL(/\/nuevaconsulta$/)
    })

    test("solicita confirmación antes de descartar el borrador en memoria", async ({ page }) => {
        await mockNuevaConsultaApi(page)

        await page.goto("/nuevaconsulta")
        const tramite = page.locator('input[name="tramite"]')
        await expect(tramite).toBeVisible()
        await tramite.fill("Borrador que no debe perderse silenciosamente")

        await page.getByRole("button", { name: "Cancelar" }).click()
        await expect(
            page.getByText("Descartar consulta sin guardar"),
        ).toBeVisible()
        await expect(
            page.getByText(/el borrador se perderá/),
        ).toBeVisible()

        await page
            .getByRole("button", { name: "Conservar borrador" })
            .click()
        await expect(tramite).toHaveValue(
            "Borrador que no debe perderse silenciosamente",
        )
        await expect(page).toHaveURL(/\/nuevaconsulta$/)

        await page.getByRole("button", { name: "Cancelar" }).click()
        await page
            .getByRole("button", { name: "Descartar y salir" })
            .click()
        await expect(page).toHaveURL(/\/consultasjuridicas$/)
    })
})
