import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const formPath = new URL("../src/components/forms/conciliacion/ConciliacionesForm.jsx", import.meta.url);
const fileApiPath = new URL("../src/lib/fileApi.js", import.meta.url);

test("conciliaciones llama a un método disponible de fileApi con el descriptor de archivo", async () => {
  const [form, fileApi] = await Promise.all([
    readFile(formPath, "utf8"),
    readFile(fileApiPath, "utf8"),
  ]);

  assert.match(
    form,
    /await\s+fileApi\.download\(file,\s*\{\s*type:\s*["']conciliacion["'],\s*id:\s*conciliacionId\s*\}\)/,
    "La descarga debe pasar el descriptor completo al método soportado",
  );
  assert.doesNotMatch(
    form,
    /fileApi\.downloadById\s*\(/,
    "downloadById no forma parte del contrato público de fileApi",
  );
  assert.match(
    fileApi,
    /export\s+async\s+function\s+download\s*\(file,\s*resource\s*=\s*null\s*\)/,
  );
  assert.match(fileApi, /export\s+const\s+fileApi\s*=\s*\{[\s\S]*?\bdownload\s*,[\s\S]*?\}/);
  assert.match(
    form,
    /const\s+file\s*=\s*files\.find\s*\(\s*\(item\)\s*=>\s*Number\(item\.id\)\s*===\s*Number\(fileId\)\s*\)/,
    "Solo debe descargarse el archivo seleccionado del recurso",
  );
});
