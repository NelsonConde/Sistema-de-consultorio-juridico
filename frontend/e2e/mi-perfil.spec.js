const { test, expect } = require("@playwright/test");

const PERFILES = [
  "ADMINISTRATIVO",
  "ASESOR",
  "MONITOR",
  "ESTUDIANTE",
  "CONCILIADOR",
];

const VIEWPORTS = [360, 768, 1280];
const THEMES = ["light", "dark"];

const KNOWN_API_PATHS = new Set([
  "/api/auth/me",
  "/api/auth/csrf",
  "/api/mi-perfil",
  "/api/mi-perfil/contacto",
  "/api/auth/cambiar-password",
]);

function perfilFixture(
    tipoPerfil,
    email = `${tipoPerfil.toLowerCase()}@prueba.local`,
) {
  return {
    username: `${tipoPerfil.toLowerCase()}.prueba`,
    rolNombre: `Rol ${tipoPerfil}`,
    tipoPerfil,
    nombre: `Usuario ${tipoPerfil}`,
    email,
    telefono: "+57 300 000 0000",
    sede: "Sede Central",
    codigo: `COD-${tipoPerfil}`,
  };
}

async function mockPerfilApi(page, tipoPerfil, options = {}) {
  const {
    contactFailure = null,
    passwordFailure = null,
    profileDelayMs = 0,
    contactDelayMs = 0,
    passwordDelayMs = 0,
    profileFailures = 0,
    profileFailureUntilReleased = false,
    profileUnauthorized = false,
  } = options;

  let actual = perfilFixture(tipoPerfil);
  let remainingProfileFailures = profileFailures;
  let profileFailureLocked = profileFailureUntilReleased;
  let sessionExpired = false;

  const patchBodies = [];
  const passwordBodies = [];
  const profileRequests = [];
  const apiRequests = [];

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());

    apiRequests.push({
      method: request.method(),
      path: url.pathname,
    });

    if (url.pathname.endsWith("/api/auth/me")) {
      await route.fulfill({
        status: sessionExpired ? 401 : 200,
        contentType: "application/json",
        body: JSON.stringify(
            sessionExpired
                ? { mensaje: "Sesión expirada" }
                : {
                  id: 1,
                  tipoPerfil,
                  permisos: [],
                },
        ),
      });
      return;
    }

    if (url.pathname.endsWith("/api/auth/csrf")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          headerName: "X-CSRF-TOKEN",
          token: "test-token",
        }),
      });
      return;
    }

    if (url.pathname.endsWith("/api/mi-perfil")) {
      profileRequests.push({
        method: request.method(),
        path: url.pathname,
      });

      if (profileDelayMs > 0) {
        await new Promise((resolve) => setTimeout(resolve, profileDelayMs));
      }

      if (profileUnauthorized) {
        sessionExpired = true;

        await route.fulfill({
          status: 401,
          contentType: "application/json",
          body: JSON.stringify({
            mensaje: "Sesión expirada",
          }),
        });
        return;
      }

      if (profileFailureLocked || remainingProfileFailures > 0) {
        if (remainingProfileFailures > 0) {
          remainingProfileFailures -= 1;
        }

        await route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({
            mensaje: "Perfil no disponible",
          }),
        });
        return;
      }

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(actual),
      });
      return;
    }

    if (url.pathname.endsWith("/api/mi-perfil/contacto")) {
      const body = request.postDataJSON();
      patchBodies.push(body);

      if (contactDelayMs > 0) {
        await new Promise((resolve) => setTimeout(resolve, contactDelayMs));
      }

      if (contactFailure) {
        await route.fulfill({
          status: contactFailure.status,
          contentType: "application/json",
          body: JSON.stringify(contactFailure.body),
        });
        return;
      }

      actual = {
        ...actual,
        email: body.email,
        telefono: body.telefono,
      };

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(actual),
      });
      return;
    }

    if (url.pathname.endsWith("/api/auth/cambiar-password")) {
      const body = request.postDataJSON();
      passwordBodies.push(body);

      if (passwordDelayMs > 0) {
        await new Promise((resolve) => setTimeout(resolve, passwordDelayMs));
      }

      if (passwordFailure) {
        await route.fulfill({
          status: passwordFailure.status,
          contentType: "application/json",
          body: JSON.stringify(passwordFailure.body),
        });
        return;
      }

      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          mensaje: "Contraseña actualizada",
        }),
      });
      return;
    }

    await route.fulfill({
      status: 404,
      contentType: "application/json",
      body: "{}",
    });
  });

  return {
    patchBodies,
    passwordBodies,
    profileRequests,
    apiRequests,
    getActual: () => actual,
    releaseProfileFailure: () => {
      profileFailureLocked = false;
    },
  };
}

async function assertNoHorizontalScroll(page) {
  await expect
      .poll(() =>
          page.evaluate(
              () =>
                  document.documentElement.scrollWidth <=
                  document.documentElement.clientWidth,
          ),
      )
      .toBe(true);
}

async function setTheme(page, theme) {
  await page.evaluate((selectedTheme) => {
    document.documentElement.classList.toggle(
        "dark",
        selectedTheme === "dark",
    );
  }, theme);

  await expect
      .poll(() =>
          page.evaluate(() =>
              document.documentElement.classList.contains("dark"),
          ),
      )
      .toBe(theme === "dark");
}

function assertBoxesDoNotOverlap(width, summary, panel) {
  expect(summary).not.toBeNull();
  expect(panel).not.toBeNull();

  if (width === 1280) {
    expect(panel.x).toBeGreaterThanOrEqual(
        summary.x + summary.width - 2,
    );
    return;
  }

  expect(panel.y).toBeGreaterThanOrEqual(
      summary.y + summary.height - 2,
  );
}

test.describe("SCRUM-352 — QA Mi Perfil", () => {
  for (const tipoPerfil of PERFILES) {
    test(`Perfil ${tipoPerfil}: lectura y actualización autorizada de contacto`, async ({
                                                                                          page,
                                                                                        }) => {
      const api = await mockPerfilApi(page, tipoPerfil);

      await page.goto("/mi-perfil");

      await expect(
          page.getByText(`Usuario ${tipoPerfil}`),
      ).toBeVisible();

      await expect(
          page.getByText(`COD-${tipoPerfil}`),
      ).toBeVisible();

      await expect(
          page.getByText("Sede Central"),
      ).toBeVisible();

      const nuevoEmail =
          `${tipoPerfil.toLowerCase()}.nuevo@prueba.local`;

      const nuevoTelefono =
          "+57 310 987 6543";

      await page
          .getByLabel("Correo electrónico")
          .fill(nuevoEmail);

      await page
          .getByLabel("Teléfono")
          .fill(nuevoTelefono);

      await page
          .getByRole("button", {
            name: "Guardar contacto",
          })
          .click();

      await expect
          .poll(() => api.patchBodies.length)
          .toBe(1);

      expect(api.patchBodies[0]).toEqual({
        email: nuevoEmail,
        telefono: nuevoTelefono,
      });

      expect(api.getActual().codigo).toBe(
          `COD-${tipoPerfil}`,
      );

      expect(api.getActual().sede).toBe(
          "Sede Central",
      );

      expect(api.getActual().tipoPerfil).toBe(
          tipoPerfil,
      );

      await expect(
          page.getByText(
              "Los datos de contacto se guardaron correctamente.",
          ),
      ).toBeVisible();
    });
  }

  test("bloquea correo y teléfono inválidos antes de enviar", async ({
                                                                       page,
                                                                     }) => {
    const api =
        await mockPerfilApi(page, "ESTUDIANTE");

    await page.goto("/mi-perfil");

    await page
        .getByLabel("Correo electrónico")
        .fill("correo-invalido-sin-dominio");

    await page
        .getByLabel("Teléfono")
        .fill("abc-sin-numeros");

    await page
        .getByRole("button", {
          name: "Guardar contacto",
        })
        .click();

    await expect(
        page.getByText(
            "Ingrese un correo electrónico válido",
        ),
    ).toBeVisible();

    await expect(
        page.getByText(
            "Ingresa un teléfono válido",
        ),
    ).toBeVisible();

    expect(api.patchBodies).toHaveLength(0);
  });

  test("campos vacíos exponen errores accesibles y no envían solicitudes", async ({
                                                                                    page,
                                                                                  }) => {
    const api =
        await mockPerfilApi(page, "ADMINISTRATIVO");

    await page.goto("/mi-perfil");

    const email =
        page.getByLabel("Correo electrónico");

    const telefono =
        page.getByLabel("Teléfono");

    await email.fill("");
    await telefono.fill("");

    await page
        .getByRole("button", {
          name: "Guardar contacto",
        })
        .click();

    await expect(email)
        .toHaveAttribute("aria-invalid", "true");

    await expect(telefono)
        .toHaveAttribute("aria-invalid", "true");

    await expect(email)
        .toHaveAttribute(
            "aria-describedby",
            /email-error/,
        );

    await expect(telefono)
        .toHaveAttribute(
            "aria-describedby",
            /telefono-error/,
        );

    await expect(
        page.locator("#email-error"),
    ).toHaveText("El campo es obligatorio");

    await expect(
        page.locator("#telefono-error"),
    ).toHaveText("El teléfono es obligatorio");

    expect(api.patchBodies).toHaveLength(0);

    await page
        .getByRole("tab", {
          name: "Seguridad",
        })
        .click();

    await page
        .getByRole("button", {
          name: "Cambiar contraseña",
        })
        .click();

    const actual =
        page.locator(
            'input[name="passwordActual"]',
        );

    const nueva =
        page.locator(
            'input[name="passwordNueva"]',
        );

    const confirmar =
        page.locator(
            'input[name="confirmarPassword"]',
        );

    await expect(actual)
        .toHaveAttribute("aria-invalid", "true");

    await expect(nueva)
        .toHaveAttribute("aria-invalid", "true");

    await expect(confirmar)
        .toHaveAttribute("aria-invalid", "true");

    await expect(actual)
        .toHaveAttribute(
            "aria-describedby",
            /passwordActual-error/,
        );

    await expect(nueva)
        .toHaveAttribute(
            "aria-describedby",
            /passwordNueva-error/,
        );

    await expect(confirmar)
        .toHaveAttribute(
            "aria-describedby",
            /confirmarPassword-error/,
        );

    await expect(
        page.locator("#passwordActual-error"),
    ).toHaveText(
        "La contraseña actual es obligatoria",
    );

    await expect(
        page.locator("#passwordNueva-error"),
    ).toHaveText(
        "La nueva contraseña es obligatoria",
    );

    await expect(
        page.locator("#confirmarPassword-error"),
    ).toHaveText(
        "Debes confirmar la nueva contraseña",
    );

    expect(api.passwordBodies).toHaveLength(0);
  });

  test("los campos protegidos no tienen inputs editables", async ({
                                                                    page,
                                                                  }) => {
    await mockPerfilApi(
        page,
        "ADMINISTRATIVO",
    );

    await page.goto("/mi-perfil");

    for (const field of [
      "id",
      "rol",
      "perfil",
      "documento",
      "codigo",
      "sede",
      "activo",
    ]) {
      await expect(
          page.locator(
              `input[name='${field}']`,
          ),
      ).toHaveCount(0);
    }

    await expect(
        page.locator(
            "input[name='email']",
        ),
    ).toBeVisible();

    await expect(
        page.locator(
            "input[name='telefono']",
        ),
    ).toBeVisible();
  });

  test("conserva borrador de contacto y limpia contraseñas al cambiar de pestaña", async ({
                                                                                            page,
                                                                                          }) => {
    await mockPerfilApi(page, "ASESOR");

    await page.goto("/mi-perfil");

    const draftEmail =
        "borrador.asesor@prueba.local";

    await page
        .getByLabel("Correo electrónico")
        .fill(draftEmail);

    await page
        .getByRole("tab", {
          name: "Seguridad",
        })
        .click();

    await page
        .locator(
            'input[name="passwordActual"]',
        )
        .fill("Actual123");

    await page
        .locator(
            'input[name="passwordNueva"]',
        )
        .fill("NuevaClave123");

    await page
        .locator(
            'input[name="confirmarPassword"]',
        )
        .fill("NuevaClave123");

    await page
        .getByRole("tab", {
          name: "Contacto",
        })
        .click();

    await expect(
        page.getByLabel(
            "Correo electrónico",
        ),
    ).toHaveValue(draftEmail);

    await page
        .getByRole("tab", {
          name: "Seguridad",
        })
        .click();

    await expect(
        page.locator(
            'input[name="passwordActual"]',
        ),
    ).toHaveValue("");

    await expect(
        page.locator(
            'input[name="passwordNueva"]',
        ),
    ).toHaveValue("");

    await expect(
        page.locator(
            'input[name="confirmarPassword"]',
        ),
    ).toHaveValue("");
  });

  test("cambia contraseña usando únicamente el contrato vigente", async ({
                                                                           page,
                                                                         }) => {
    const api =
        await mockPerfilApi(page, "MONITOR");

    await page.goto("/mi-perfil");

    await page
        .getByRole("tab", {
          name: "Seguridad",
        })
        .click();

    await page
        .locator(
            'input[name="passwordActual"]',
        )
        .fill("Actual123");

    await page
        .locator(
            'input[name="passwordNueva"]',
        )
        .fill("clave-segura");

    await page
        .locator(
            'input[name="confirmarPassword"]',
        )
        .fill("clave-segura");

    await page
        .getByRole("button", {
          name: "Cambiar contraseña",
        })
        .click();

    await expect
        .poll(() => api.passwordBodies.length)
        .toBe(1);

    expect(api.passwordBodies[0]).toEqual({
      passwordActual: "Actual123",
      passwordNueva: "clave-segura",
    });

    await expect(
        page.getByText(
            "La contraseña se cambió correctamente.",
        ),
    ).toBeVisible();

    await expect(
        page.locator(
            'input[name="passwordActual"]',
        ),
    ).toHaveValue("");

    await expect(
        page.locator(
            'input[name="passwordNueva"]',
        ),
    ).toHaveValue("");

    await expect(
        page.locator(
            'input[name="confirmarPassword"]',
        ),
    ).toHaveValue("");
  });

  test("muestra estado Guardando y evita doble envío de contacto", async ({
                                                                            page,
                                                                          }) => {
    const api = await mockPerfilApi(
        page,
        "ESTUDIANTE",
        {
          contactDelayMs: 350,
        },
    );

    await page.goto("/mi-perfil");

    await page
        .getByLabel("Correo electrónico")
        .fill(
            "estado.guardando@prueba.local",
        );

    await page
        .getByLabel("Teléfono")
        .fill("+57 311 000 0000");

    const saveButton =
        page.getByRole("button", {
          name: "Guardar contacto",
        });

    await saveButton.click();

    const savingButton =
        page.getByRole("button", {
          name: "Guardando...",
        });

    await expect(savingButton)
        .toBeVisible();

    await expect(savingButton)
        .toBeDisabled();

    await expect(
        page.locator("form[aria-busy='true']"),
    ).toBeVisible();

    await expect(
        page.getByText(
            "Los datos de contacto se guardaron correctamente.",
        ),
    ).toBeVisible();

    expect(api.patchBodies).toHaveLength(1);
  });

  test("representa fallo controlado de contacto sin perder las acciones", async ({
                                                                                   page,
                                                                                 }) => {
    const api = await mockPerfilApi(
        page,
        "CONCILIADOR",
        {
          contactFailure: {
            status: 400,
            body: {
              mensaje:
                  "No fue posible actualizar el contacto",
            },
          },
        },
    );

    await page.goto("/mi-perfil");

    const email =
        "error.controlado@prueba.local";

    const telefono =
        "+57 312 123 4567";

    await page
        .getByLabel("Correo electrónico")
        .fill(email);

    await page
        .getByLabel("Teléfono")
        .fill(telefono);

    await page
        .getByRole("button", {
          name: "Guardar contacto",
        })
        .click();

    await expect(
        page
            .getByTestId("profile-panel")
            .getByRole("alert"),
    ).toContainText(
        "No fue posible actualizar el contacto",
    );

    await expect(
        page.getByLabel(
            "Correo electrónico",
        ),
    ).toHaveValue(email);

    await expect(
        page.getByLabel("Teléfono"),
    ).toHaveValue(telefono);

    await expect(
        page.getByRole("button", {
          name: "Guardar contacto",
        }),
    ).toBeEnabled();

    expect(api.patchBodies).toHaveLength(1);
  });

  test("muestra errores largos sin generar desplazamiento horizontal", async ({
                                                                                page,
                                                                              }) => {
    const longMessage =
        "No fue posible actualizar el contacto porque la información recibida no cumple las reglas vigentes de validación institucional. Revise el correo y el teléfono antes de intentarlo nuevamente.";

    await mockPerfilApi(
        page,
        "CONCILIADOR",
        {
          contactFailure: {
            status: 400,
            body: {
              mensaje: longMessage,
            },
          },
        },
    );

    await page.setViewportSize({
      width: 360,
      height: 800,
    });

    await page.goto("/mi-perfil");

    await page
        .getByRole("button", {
          name: "Guardar contacto",
        })
        .click();

    await expect(
        page
            .getByTestId("profile-panel")
            .getByRole("alert"),
    ).toContainText(longMessage);

    await assertNoHorizontalScroll(page);
  });

  test("presenta loading, error y recuperación mediante retry", async ({
                                                                         page,
                                                                       }) => {
    const api = await mockPerfilApi(
        page,
        "ADMINISTRATIVO",
        {
          profileDelayMs: 250,
          profileFailureUntilReleased: true,
        },
    );

    await page.goto("/mi-perfil");

    await expect(
        page.getByTestId(
            "profile-loading",
        ),
    ).toBeVisible();

    await expect(
        page.getByText(
            "No fue posible cargar tu perfil",
        ),
    ).toBeVisible();

    await expect(
        page.getByRole("button", {
          name: "Reintentar",
        }),
    ).toBeVisible();

    api.releaseProfileFailure();

    await page
        .getByRole("button", {
          name: "Reintentar",
        })
        .click();

    await expect(
        page.getByText(
            "Usuario ADMINISTRATIVO",
        ),
    ).toBeVisible();

    expect(
        api.profileRequests.length,
    ).toBeGreaterThanOrEqual(2);
  });

  test("una sesión vencida redirige fuera de Mi Perfil", async ({
                                                                  page,
                                                                }) => {
    await mockPerfilApi(
        page,
        "ADMINISTRATIVO",
        {
          profileUnauthorized: true,
        },
    );

    await page.goto("/mi-perfil");

    await expect
        .poll(() => {
          const currentUrl =
              new URL(page.url());

          return currentUrl.pathname;
        })
        .toBe("/");
  });

  test("mantiene responsive en 360, 768 y 1280 px para claro y oscuro", async ({
                                                                                 page,
                                                                               }, testInfo) => {
    await mockPerfilApi(
        page,
        "ESTUDIANTE",
    );

    for (const width of VIEWPORTS) {
      for (const theme of THEMES) {
        await page.setViewportSize({
          width,
          height: 900,
        });

        await page.goto("/mi-perfil");

        await expect(
            page.getByTestId(
                "profile-summary",
            ),
        ).toBeVisible();

        await expect(
            page.getByTestId(
                "profile-panel",
            ),
        ).toBeVisible();

        await setTheme(
            page,
            theme,
        );

        const summary =
            await page
                .getByTestId(
                    "profile-summary",
                )
                .boundingBox();

        const panel =
            await page
                .getByTestId(
                    "profile-panel",
                )
                .boundingBox();

        assertBoxesDoNotOverlap(
            width,
            summary,
            panel,
        );

        await expect(
            page.getByRole("tab", {
              name: "Contacto",
            }),
        ).toBeVisible();

        await expect(
            page.getByRole("tab", {
              name: "Seguridad",
            }),
        ).toBeVisible();

        await expect(
            page.getByLabel(
                "Correo electrónico",
            ),
        ).toBeVisible();

        await expect(
            page.getByLabel("Teléfono"),
        ).toBeVisible();

        await expect(
            page.getByRole("button", {
              name: "Guardar contacto",
            }),
        ).toBeVisible();

        await assertNoHorizontalScroll(
            page,
        );

        await page
            .getByRole("tab", {
              name: "Seguridad",
            })
            .click();

        await expect(
            page.locator('input[name="passwordActual"]'),
        ).toBeVisible();

        await expect(
            page.locator('input[name="passwordNueva"]'),
        ).toBeVisible();

        await expect(
            page.locator('input[name="confirmarPassword"]'),
        ).toBeVisible();

        await expect(
            page.getByRole("button", {
              name: "Cambiar contraseña",
            }),
        ).toBeVisible();

        await assertNoHorizontalScroll(
            page,
        );

        const screenshot =
            await page.screenshot({
              fullPage: true,
            });

        await testInfo.attach(
            `SCRUM-352-${width}-${theme}`,
            {
              body: screenshot,
              contentType: "image/png",
            },
        );
      }
    }
  });

  test("permite Tab, Shift+Tab y navegación de pestañas con foco visible", async ({
                                                                                    page,
                                                                                  }) => {
    await mockPerfilApi(
        page,
        "ADMINISTRATIVO",
    );

    await page.goto("/mi-perfil");

    const email =
        page.getByLabel(
            "Correo electrónico",
        );

    const telefono =
        page.getByLabel("Teléfono");

    await email.focus();

    await page.keyboard.press("Tab");

    await expect(telefono)
        .toBeFocused();

    expect(
        await telefono.evaluate(
            (element) =>
                element.matches(":focus-visible"),
        ),
    ).toBe(true);

    await page.keyboard.press(
        "Shift+Tab",
    );

    await expect(email)
        .toBeFocused();

    expect(
        await email.evaluate(
            (element) =>
                element.matches(":focus-visible"),
        ),
    ).toBe(true);

    const contactoTab =
        page.getByRole("tab", {
          name: "Contacto",
        });

    const seguridadTab =
        page.getByRole("tab", {
          name: "Seguridad",
        });

    await contactoTab.focus();

    await page.keyboard.press(
        "ArrowRight",
    );

    await expect(seguridadTab)
        .toHaveAttribute(
            "data-state",
            "active",
        );

    await expect(seguridadTab)
        .toBeFocused();

    expect(
        await seguridadTab.evaluate(
            (element) =>
                element.matches(":focus-visible"),
        ),
    ).toBe(true);

    await page.keyboard.press(
        "ArrowLeft",
    );

    await expect(contactoTab)
        .toHaveAttribute(
            "data-state",
            "active",
        );

    await expect(contactoTab)
        .toBeFocused();
  });

  test("mantiene exclusivamente los endpoints previstos para Mi Perfil", async ({
                                                                                  page,
                                                                                }) => {
    const api =
        await mockPerfilApi(
            page,
            "ADMINISTRATIVO",
        );

    await page.goto("/mi-perfil");

    await page
        .getByLabel("Correo electrónico")
        .fill(
            "endpoints@prueba.local",
        );

    await page
        .getByLabel("Teléfono")
        .fill("+57 313 000 0000");

    await page
        .getByRole("button", {
          name: "Guardar contacto",
        })
        .click();

    await expect
        .poll(() => api.patchBodies.length)
        .toBe(1);

    await page
        .getByRole("tab", {
          name: "Seguridad",
        })
        .click();

    await page
        .locator(
            'input[name="passwordActual"]',
        )
        .fill("Actual123");

    await page
        .locator(
            'input[name="passwordNueva"]',
        )
        .fill("clave-segura");

    await page
        .locator(
            'input[name="confirmarPassword"]',
        )
        .fill("clave-segura");

    await page
        .getByRole("button", {
          name: "Cambiar contraseña",
        })
        .click();

    await expect
        .poll(
            () =>
                api.passwordBodies.length,
        )
        .toBe(1);

    const unexpected =
        api.apiRequests.filter(
            ({ path }) =>
                !KNOWN_API_PATHS.has(path),
        );

    expect(unexpected).toEqual([]);
  });
});