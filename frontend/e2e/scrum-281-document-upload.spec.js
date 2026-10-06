const { test, expect } = require("@playwright/test")

const CONSULTA_ID = 281
const UPLOAD_ID = "upload-scrum-281"

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
    {
        failFirstCompletion = false,
        holdUploadInitiation = false,
    } = {},
) {
    const state = {
        consultaRequests: 0,
        uploadInitiations: 0,
        storagePuts: 0,
        completionRequests: 0,
        abortRequests: 0,
        consultaBodies: [],
        uploadBodies: [],
    }

    let releaseUploadInitiation = null

    const uploadInitiationGate = holdUploadInitiation
        ? new Promise((resolve) => {
            releaseUploadInitiation = resolve
        })
        : null

    state.releaseUploadInitiation = () => {
        releaseUploadInitiation?.()
    }

    await page.route("https://storage.test/**", async (route) => {
        const request = route.request()

        if (request.method() === "OPTIONS") {
            await route.fulfill({
                status: 204,
                headers: {
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Allow-Methods": "PUT, OPTIONS",
                    "Access-Control-Allow-Headers": "Content-Type",
                },
            })
            return
        }

        if (request.method() === "PUT") {
            state.storagePuts += 1

            await route.fulfill({
                status: 200,
                headers: {
                    "Access-Control-Allow-Origin": "*",
                },
                body: "",
            })
            return
        }

        await route.fulfill({ status: 405, body: "" })
    })

    await page.route("**/api/**", async (route) => {
        const request = route.request()
        const url = new URL(request.url())
        const path = url.pathname
        const method = request.method()

        if (path.endsWith("/api/auth/me")) {
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
            await json(route, {
                headerName: "X-CSRF-TOKEN",
                token: "test-token",
            })
            return
        }

        if (path.endsWith("/api/sedes")) {
            await json(route, [
                {
                    id: 1,
                    nombre: "Sede Central",
                },
            ])
            return
        }

        if (path.endsWith("/api/areas")) {
            await json(route, [
                {
                    id: 1,
                    nombre: "Derecho Civil",
                },
            ])
            return
        }

        if (path.endsWith("/api/temas/area/1")) {
            await json(route, [
                {
                    id: 10,
                    nombre: "Tema de prueba",
                },
            ])
            return
        }

        if (path.endsWith("/api/tipos/tema/10")) {
            await json(route, [
                {
                    id: 20,
                    nombre: "Tipo de prueba",
                },
            ])
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

        if (
            path.endsWith("/api/consultas") &&
            method === "POST"
        ) {
            state.consultaRequests += 1
            state.consultaBodies.push(request.postDataJSON())

            /*
             * Mantiene la primera creación ocupada durante un instante para que
             * dos clicks consecutivos realmente ejerciten el bloqueo síncrono
             * implementado con submitLockRef.
             */
            await new Promise((resolve) => setTimeout(resolve, 100))

            await json(route, {
                id: CONSULTA_ID,
            })
            return
        }

        if (
            path.endsWith(
                `/api/consultas/${CONSULTA_ID}/archivos/uploads`,
            ) &&
            method === "POST"
        ) {
            state.uploadInitiations += 1
            state.uploadBodies.push(request.postDataJSON())

            if (uploadInitiationGate) {
                await uploadInitiationGate
            }

            await json(route, {
                uploadId: UPLOAD_ID,
                uploadUrl: `https://storage.test/${UPLOAD_ID}`,
                expiresAt: "2099-01-01T00:00:00Z",
            })
            return
        }

        if (
            path.endsWith(
                `/api/file-uploads/${UPLOAD_ID}/complete`,
            ) &&
            method === "POST"
        ) {
            state.completionRequests += 1

            if (
                failFirstCompletion &&
                state.completionRequests === 1
            ) {
                await json(
                    route,
                    {
                        message: "Fallo controlado de confirmación",
                    },
                    500,
                )
                return
            }

            await json(route, {
                id: 9001,
                fileName: "evidencia.pdf",
                size: 20,
                contentType: "application/pdf",
                status: "CONFIRMED",
            })
            return
        }

        if (
            path.endsWith(`/api/file-uploads/${UPLOAD_ID}`) &&
            method === "DELETE"
        ) {
            state.abortRequests += 1
            await route.fulfill({
                status: 204,
                body: "",
            })
            return
        }

        await json(route, {}, 404)
    })

    return state
}

async function completarConsultaMinima(page) {
    await page.goto("/nuevaconsulta")

    await expect(
        page.locator('input[name="fecha"]'),
    ).toBeVisible()

    await page.locator('input[name="fecha"]').fill("2026-10-04")
    await page.locator('input[name="tramite"]').fill("Consulta jurídica")

    await page
        .locator('select[name="sedeId"]')
        .selectOption("1")

    await page
        .locator('select[name="areaId"]')
        .selectOption("1")

    await expect(
        page.locator(
            'select[name="temaId"] option[value="10"]',
        ),
    ).toHaveCount(1)

    await page
        .locator('select[name="temaId"]')
        .selectOption("10")

    await expect(
        page.locator(
            'select[name="tipoId"] option[value="20"]',
        ),
    ).toHaveCount(1)

    await page
        .locator('select[name="tipoId"]')
        .selectOption("20")

    await page
        .getByRole("button", {
            name: /Buscar parte principal/i,
        })
        .click()

    await expect(
        page.getByRole("heading", {
            name: "Seleccionar Parte Principal",
        }),
    ).toBeVisible()

    await page
        .getByRole("button", {
            name: /Persona Prueba/i,
        })
        .click()

    await page
        .locator('textarea[name="descripcion"]')
        .fill("Descripción de prueba")

    await page
        .locator('textarea[name="hechos"]')
        .fill("Hechos de prueba")

    await page
        .locator('textarea[name="pretensiones"]')
        .fill("Pretensiones de prueba")

    await page
        .locator('textarea[name="conceptoJuridico"]')
        .fill("Concepto jurídico de prueba")
}

async function adjuntarArchivo(page) {
    const content = Buffer.from(
        "contenido documental SCRUM-281",
        "utf8",
    )

    await page
        .locator("#archivos")
        .setInputFiles({
            name: "evidencia.pdf",
            mimeType: "application/pdf",
            buffer: content,
        })

    return content
}

test.describe("SCRUM-281 - carga documental", () => {
    test(
        "doble envío no duplica consulta ni sesión de carga",
        async ({ page }) => {
            const api = await mockNuevaConsultaApi(page)

            await completarConsultaMinima(page)

            const fileContent = await adjuntarArchivo(page)

            const crear = page.getByRole("button", {
                name: "Crear consulta",
            })

            /*
             * Dos clicks en el mismo turno del navegador evitan depender de que
             * React alcance a deshabilitar visualmente el botón entre ambos.
             * El segundo submit debe quedar bloqueado por submitLockRef.
             */
            await crear.evaluate((button) => {
                button.click()
                button.click()
            })

            await expect
                .poll(() => api.consultaRequests)
                .toBe(1)

            await expect
                .poll(() => api.uploadInitiations)
                .toBe(1)

            await expect
                .poll(() => api.storagePuts)
                .toBe(1)

            await expect
                .poll(() => api.completionRequests)
                .toBe(1)

            expect(api.consultaBodies).toHaveLength(1)
            expect(api.uploadBodies).toHaveLength(1)

            expect(api.uploadBodies[0]).toEqual({
                fileName: "evidencia.pdf",
                size: fileContent.length,
                contentType: "application/pdf",
            })

            /*
             * Metadatos controlados por backend: el frontend no debe enviarlos.
             */
            expect(api.uploadBodies[0]).not.toHaveProperty("autor")
            expect(api.uploadBodies[0]).not.toHaveProperty("origen")
            expect(api.uploadBodies[0]).not.toHaveProperty("version")

            expect(api.abortRequests).toBe(0)
        },
    )

    test(
        "fallo de confirmación reintenta solo complete sin retransmitir bytes",
        async ({ page }) => {
            const api = await mockNuevaConsultaApi(page, {
                failFirstCompletion: true,
            })

            await completarConsultaMinima(page)
            await adjuntarArchivo(page)

            await page
                .getByRole("button", {
                    name: "Crear consulta",
                })
                .click()

            await expect
                .poll(() => api.consultaRequests)
                .toBe(1)

            await expect
                .poll(() => api.uploadInitiations)
                .toBe(1)

            await expect
                .poll(() => api.storagePuts)
                .toBe(1)

            await expect
                .poll(() => api.completionRequests)
                .toBe(1)

            const retryButton = page.getByRole("button", {
                name: /Reintentar confirmación/i,
            })

            await expect(retryButton).toBeVisible()

            /*
             * El fallo de /complete no debe eliminar la sesión porque el usuario
             * todavía puede confirmar el mismo uploadId.
             */
            expect(api.abortRequests).toBe(0)

            await retryButton.click()

            await expect
                .poll(() => api.completionRequests)
                .toBe(2)

            /*
             * La confirmación se repite, pero NO la creación de la consulta,
             * NO la sesión de carga y NO el PUT al almacenamiento.
             */
            expect(api.consultaRequests).toBe(1)
            expect(api.uploadInitiations).toBe(1)
            expect(api.storagePuts).toBe(1)
            expect(api.abortRequests).toBe(0)
        },
    )

    test(
        "cancelar carga elimina la sesión y evita transferencia y confirmación",
        async ({ page }) => {
            const api = await mockNuevaConsultaApi(page, {
                holdUploadInitiation: true,
            })

            await completarConsultaMinima(page)
            await adjuntarArchivo(page)

            await page
                .getByRole("button", {
                    name: "Crear consulta",
                })
                .click()

            await expect
                .poll(() => api.uploadInitiations)
                .toBe(1)

            const estadoCarga = page.locator(
                'section[aria-label="Estado de carga de documentos"]',
            )

            const cancelarCarga = estadoCarga.getByRole("button", {
                name: "Cancelar",
            })

            await expect(cancelarCarga).toBeVisible()
            await cancelarCarga.click()

            api.releaseUploadInitiation()

            /*
             * La sesión backend termina de crearse, pero el AbortController ya
             * está cancelado. Por eso no debe comenzar el PUT y la sesión debe
             * compensarse mediante DELETE.
             */
            await expect
                .poll(() => api.abortRequests)
                .toBe(1)

            expect(api.consultaRequests).toBe(1)
            expect(api.uploadInitiations).toBe(1)
            expect(api.storagePuts).toBe(0)
            expect(api.completionRequests).toBe(0)

            await expect(
                estadoCarga.getByText("Carga cancelada"),
            ).toBeVisible()
        },
    )
})