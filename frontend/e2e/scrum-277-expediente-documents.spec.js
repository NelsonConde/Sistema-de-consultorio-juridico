const { test, expect } = require("@playwright/test")

const CONSULTA_ID = 101

const consulta = {
    id: CONSULTA_ID,
    descripcion: "Consulta de prueba para expediente documental",
    fecha: "2026-10-04",
    estado: "ACTIVO",
    tramite: "Asesoría",
    hechos: "Hechos de prueba",
    pretensiones: "Pretensiones de prueba",
    conceptoJuridico: "Concepto de prueba",
    observaciones: "",
    tipoViolencia: "",
    resultado: "",
    personaId: null,
    sedeId: null,
    areaId: null,
    temaId: null,
    tipoId: null,
    asesorId: null,
    monitorId: null,
    estudianteId: null,
    partesIds: [],
    contrapartesIds: [],
    version: 1,
}

const documentos = [
    {
        id: 5001,
        documentoLogico: "11111111-1111-1111-1111-111111111111",
        version: 2,
        tipoDocumental: "DEMANDA",
        origen: "CARGA_USUARIO",
        referenciaAnteriorId: 4001,
        resourceType: "PROCESO",
        resourceId: 3001,
        fileName: "demanda.pdf",
        size: 2048,
        contentType: "application/pdf",
        status: "VIGENTE",
        autorId: 20,
        autorUsername: "asesor.prueba",
        createdAt: "2026-10-04T15:30:00",
    },
]

let downloadDescriptorRequests = 0

async function mockApi(page, options = {}) {
    const expedienteStatus = options.expedienteStatus ?? 200

    await page.route("**/api/**", async (route) => {
        const request = route.request()
        const url = new URL(request.url())

        if (url.pathname.endsWith("/api/auth/me")) {
            await route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify({
                    id: 1,
                    username: "admin.prueba",
                    tipoPerfil: "ADMINISTRATIVO",
                    permisos: [
                        "Acceder consultas jurídicas",
                        "Ver consultas",
                        "Editar consultas",
                    ],
                }),
            })
            return
        }

        if (url.pathname.endsWith("/api/auth/csrf")) {
            await route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify({
                    headerName: "X-CSRF-TOKEN",
                    token: "test-token",
                }),
            })
            return
        }

        if (
            url.pathname.endsWith("/api/sedes") ||
            url.pathname.endsWith("/api/areas") ||
            url.pathname.endsWith("/api/asesores/activos") ||
            url.pathname.endsWith("/api/monitores/activos") ||
            url.pathname.endsWith("/api/estudiantes/activos")
        ) {
            await route.fulfill({
                status: 200,
                contentType: "application/json",
                body: "[]",
            })
            return
        }

        if (
            url.pathname === "/api/consultas" &&
            request.method() === "GET"
        ) {
            await route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify([consulta]),
            })
            return
        }

        if (
            url.pathname === `/api/consultas/${CONSULTA_ID}` &&
            request.method() === "GET"
        ) {
            await route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(consulta),
            })
            return
        }

        if (
            url.pathname ===
            `/api/consultas/${CONSULTA_ID}/expediente/archivos`
        ) {
            await route.fulfill({
                status: expedienteStatus,
                contentType: "application/json",
                body:
                    expedienteStatus === 200
                        ? JSON.stringify(options.documentos ?? documentos)
                        : JSON.stringify({
                            message: "Acceso denegado",
                        }),
            })
            return
        }

        if (
            url.pathname === "/api/archivos/5001/download"
        ) {
            downloadDescriptorRequests += 1

            const downloadUrl =
                options.expiredFirstDownload &&
                downloadDescriptorRequests === 1
                    ? "https://storage.test/documento-expirado"
                    : "https://storage.test/documento"

            await route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify({
                    fileName: "demanda.pdf",
                    downloadUrl,
                }),
            })
            return
        }

        await route.fulfill({
            status: 404,
            contentType: "application/json",
            body: "{}",
        })
    })

    await page.route(
        "https://storage.test/documento",
        async (route) => {
            await route.fulfill({
                status: 200,
                contentType: "application/pdf",
                body: "contenido-prueba",
            })
        },
    )

    await page.route(
        "https://storage.test/documento-expirado",
        async (route) => {
            await route.fulfill({
                status: 403,
                contentType: "text/plain",
                body: "Expired",
            })
        },
    )
}

async function abrirDocumentos(page) {
    await page.goto("/consultasjuridicas")

    await expect(
        page.getByText(
            "Consulta de prueba para expediente documental",
        ),
    ).toBeVisible()

    await page.getByRole("button", {
        name: "Editar",
    }).click()

    await expect(
        page.getByRole("tab", {
            name: "Documentos",
        }),
    ).toBeVisible()

    await page.getByRole("tab", {
        name: "Documentos",
    }).click()
}

test.describe("SCRUM-277 - Documentos del expediente", () => {
    test("muestra documentos agregados del expediente", async ({
                                                                   page,
                                                               }) => {
        await mockApi(page)

        await abrirDocumentos(page)

        const panelDocumentos = page.getByRole("tabpanel", {
            name: "Documentos",
        })

        const tabla = panelDocumentos.getByRole("table")

        await expect(tabla).toBeVisible()

        await expect(
            tabla.getByTitle("demanda.pdf"),
        ).toBeVisible()

        await expect(
            tabla.getByRole("cell", {
                name: "Demanda",
                exact: true,
            }),
        ).toBeVisible()

        await expect(
            tabla.getByRole("cell", {
                name: "Proceso",
                exact: true,
            }),
        ).toBeVisible()

        await expect(
            tabla.getByRole("cell", {
                name: "asesor.prueba",
                exact: true,
            }),
        ).toBeVisible()

        await expect(
            tabla.getByRole("cell", {
                name: "2.0 KB",
                exact: true,
            }),
        ).toBeVisible()

        await expect(
            tabla.getByRole("cell", {
                name: "Vigente",
                exact: true,
            }),
        ).toBeVisible()
    })

    test("muestra estado vacío", async ({ page }) => {
        await mockApi(page, {
            documentos: [],
        })

        await abrirDocumentos(page)

        await expect(
            page.getByText(
                "No hay documentos para mostrar.",
            ),
        ).toBeVisible()
    })

    test("un 403 limpia los documentos y muestra mensaje", async ({
                                                                      page,
                                                                  }) => {
        await mockApi(page, {
            expedienteStatus: 403,
        })

        await abrirDocumentos(page)

        await expect(
            page.getByText(
                "No tienes autorización para consultar los documentos de este expediente.",
            ),
        ).toBeVisible()

        await expect(
            page.getByText("demanda.pdf"),
        ).toHaveCount(0)
    })

    test("envía filtros al endpoint agregado", async ({
                                                          page,
                                                      }) => {
        await mockApi(page)

        await abrirDocumentos(page)

        const requestPromise = page.waitForRequest(
            (request) => {
                const url = new URL(request.url())

                return (
                    url.pathname ===
                    `/api/consultas/${CONSULTA_ID}/expediente/archivos` &&
                    url.searchParams.get("resourceType") ===
                    "PROCESO" &&
                    url.searchParams.get("tipoDocumental") ===
                    "DEMANDA"
                )
            },
        )

        await page
            .locator('select[name="resourceType"]')
            .selectOption("PROCESO")

        await page
            .locator('input[name="tipoDocumental"]')
            .fill("DEMANDA")

        await page.getByRole("button", {
            name: "Aplicar filtros",
        }).click()

        await requestPromise
    })

    test("solicita autorización de descarga por ID", async ({
                                                                page,
                                                            }) => {
        await mockApi(page)

        await abrirDocumentos(page)

        const requestPromise = page.waitForRequest(
            (request) =>
                new URL(request.url()).pathname ===
                "/api/archivos/5001/download",
        )

        await page.getByRole("button", {
            name: "Descargar",
        }).click()

        const request = await requestPromise

        expect(request.method()).toBe("GET")
    })

    test("renueva una URL firmada expirada y reintenta la descarga", async ({
                                                                                page,
                                                                            }) => {
        await mockApi(page, {
            expiredFirstDownload: true,
        })

        await abrirDocumentos(page)

        let descriptorRequests = 0

        page.on("request", (request) => {
            const url = new URL(request.url())

            if (
                url.pathname === "/api/archivos/5001/download"
            ) {
                descriptorRequests += 1
            }
        })

        await page.getByRole("tabpanel", {
            name: "Documentos",
        }).getByRole("button", {
            name: "Descargar",
        }).click()

        await expect.poll(
            () => descriptorRequests,
        ).toBe(2)
    })
})