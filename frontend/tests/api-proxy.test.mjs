import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import nextConfig from "../next.config.mjs";

async function withEnv(key, value, action) {
  const original = process.env[key];
  if (value === null) delete process.env[key];
  else process.env[key] = value;

  try {
    return await action();
  } finally {
    if (original === undefined) delete process.env[key];
    else process.env[key] = original;
  }
}

test("sin API_PROXY_ORIGIN conserva el despliegue existente", async () => {
  await withEnv("API_PROXY_ORIGIN", null, async () => {
    assert.deepEqual(await nextConfig.rewrites(), []);
  });
});

test("reenvía /api sin duplicar rutas ni crear un proxy abierto", async () => {
  await withEnv("API_PROXY_ORIGIN", "https://backend.example.test", async () => {
    assert.deepEqual(await nextConfig.rewrites(), [{
      source: "/api/:path*",
      destination: "https://backend.example.test/api/:path*",
    }]);
  });
});

test("rechaza destinos de proxy inseguros o mal formados", async () => {
  for (const value of [
    "not-a-url",
    "http://remote.example.test",
    "https://backend.example.test/api",
    "https://backend.example.test/?token=abc",
    "https://user:password@backend.example.test",
    "javascript:alert(1)",
  ]) {
    await withEnv("API_PROXY_ORIGIN", value, async () => {
      await assert.rejects(nextConfig.rewrites(), /API_PROXY_ORIGIN/);
    });
  }
});

test("HTTP local solo durante desarrollo", async () => {
  await withEnv("API_PROXY_ORIGIN", "http://localhost:8080", async () => {
    await withEnv("NODE_ENV", "development", async () => {
      assert.deepEqual(await nextConfig.rewrites(), [{
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      }]);
    });
    await withEnv("NODE_ENV", "production", async () => {
      await assert.rejects(nextConfig.rewrites(), /API_PROXY_ORIGIN/);
    });
  });
});

async function importCurrentApi() {
  const configSource = await readFile(
    new URL("../src/lib/config.js", import.meta.url), "utf8",
  );
  const clientSource = await readFile(
    new URL("../src/lib/apiClient.js", import.meta.url), "utf8",
  );
  const configUrl = `data:text/javascript;base64,${Buffer.from(configSource).toString("base64")}`;
  const replaced = clientSource.replace(
    /from (["'])@\/lib\/config\1/,
    `from "${configUrl}"`,
  );
  assert.notEqual(replaced, clientSource, "Se debe importar el config.js real");
  const clientUrl = `data:text/javascript;base64,${Buffer.from(replaced).toString("base64")}`;
  const [config, client] = await Promise.all([import(configUrl), import(clientUrl)]);
  return { ...config, ...client };
}

test("preserva rutas completas y relativas, CSRF y envío de cookies", async () => {
  await withEnv("NEXT_PUBLIC_API_URL_BASE", "https://frontend.example.test/api", async () => {
    const originalFetch = globalThis.fetch;
    const requests = [];
    try {
      globalThis.fetch = async (url, options = {}) => {
        requests.push({ url, options });
        const payload = url.endsWith("/auth/csrf")
          ? { headerName: "X-CSRF-TOKEN", token: "csrf-sintetico" }
          : { ok: true };

        return new Response(JSON.stringify(payload), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      };

      const { API_URL_BASE, apiClient } = await importCurrentApi();
      assert.equal(API_URL_BASE, "https://frontend.example.test/api");
      assert.equal(new URL(`${API_URL_BASE}/audit`).pathname, "/api/audit");

      await apiClient.get("/consultas");
      await apiClient.get(`${API_URL_BASE}/estadisticas/semestres`);
      await apiClient.post("/auth/login", {
        username: "qa",
        password: "sintetica",
      });

      assert.deepEqual(requests.map(({ url }) => url), [
        "https://frontend.example.test/api/consultas",
        "https://frontend.example.test/api/estadisticas/semestres",
        "https://frontend.example.test/api/auth/csrf",
        "https://frontend.example.test/api/auth/login",
      ]);
      assert.ok(requests.every(({ options }) => options.credentials === "include"));
      assert.equal(
        requests[3].options.headers.get("X-CSRF-TOKEN"),
        "csrf-sintetico",
      );
    } finally {
      globalThis.fetch = originalFetch;
    }
  });
});
