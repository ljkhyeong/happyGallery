import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, mkdir, writeFile, readFile, symlink, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { once } from "node:events";
import { publishAssets } from "../../publish-assets.mjs";
import { createAssetApp } from "../../assets-server.mjs";

test("버전 교체와 동시에 이전·새 JS를 HTTP로 제공하고 재게시해도 유지한다", async (t) => {
  const root = await mkdtemp(path.join(tmpdir(), "hg-assets-"));
  t.after(() => rm(root, { recursive: true, force: true }));
  const previous = path.join(root, "previous");
  const next = path.join(root, "next");
  const storage = path.join(root, "storage");
  await mkdir(previous); await mkdir(next);
  await writeFile(path.join(previous, "old-a123.js"), "old page");
  await writeFile(path.join(next, "new-b456.js"), "new page");
  await publishAssets(previous, storage);
  const server = createAssetApp(storage).listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => { server.closeAllConnections(); server.close(); });
  const origin = `http://127.0.0.1:${server.address().port}`;
  await publishAssets(next, storage);
  await publishAssets(previous, storage);
  for (const [file, content] of [["old-a123.js", "old page"], ["new-b456.js", "new page"]]) {
    const response = await fetch(`${origin}/assets/${file}`);
    assert.equal(response.status, 200);
    assert.match(response.headers.get("cache-control"), /immutable/);
    assert.match(response.headers.get("content-type"), /javascript/);
    assert.equal(await response.text(), content);
  }
  assert.equal((await fetch(`${origin}/assets/missing.js`)).status, 404);
  assert.equal(await (await fetch(`${origin}/assets/happygallery-asset-store-v1.txt`)).text(), "shared-assets-v1");
});

test("동일 URL 내용 충돌과 심볼릭 링크를 거부하고 기존 파일을 보존한다", async (t) => {
  const root = await mkdtemp(path.join(tmpdir(), "hg-assets-"));
  t.after(() => rm(root, { recursive: true, force: true }));
  const source = path.join(root, "source");
  const target = path.join(root, "target");
  await mkdir(source);
  await writeFile(path.join(source, "same.js"), "original");
  await publishAssets(source, target);
  await writeFile(path.join(source, "same.js"), "conflict");
  await assert.rejects(publishAssets(source, target), /충돌/);
  assert.equal(await readFile(path.join(target, "same.js"), "utf8"), "original");
  await rm(path.join(source, "same.js"));
  await symlink(path.join(target, "same.js"), path.join(source, "linked.js"));
  await assert.rejects(publishAssets(source, target), /게시할 수 없는/);
});
