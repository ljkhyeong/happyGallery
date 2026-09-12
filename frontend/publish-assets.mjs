import { constants } from "node:fs";
import { copyFile, mkdir, readdir, readFile, link, unlink } from "node:fs/promises";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { pathToFileURL } from "node:url";

// 동일 URL의 내용을 덮어쓰지 않는다. 이전 HTML이 참조하는 파일도 계속 보존한다.
export async function publishAssets(source, destination) {
  await mkdir(destination, { recursive: true });
  for (const entry of await readdir(source, { withFileTypes: true })) {
    if (entry.name.startsWith(".") || entry.isSymbolicLink()) {
      throw new Error(`게시할 수 없는 asset: ${entry.name}`);
    }
    const from = path.join(source, entry.name);
    const to = path.join(destination, entry.name);
    if (entry.isDirectory()) {
      await publishAssets(from, to);
    } else if (entry.isFile()) {
      const temporary = path.join(destination, `.publish-${randomUUID()}`);
      await copyFile(from, temporary, constants.COPYFILE_EXCL);
      try {
        // hard link 생성은 원자적이고 기존 파일을 교체하지 않는다.
        await link(temporary, to);
      } catch (error) {
        if (error.code !== "EEXIST") throw error;
        if (!(await readFile(from)).equals(await readFile(to))) {
          throw new Error(`기존 asset과 내용이 충돌합니다: ${entry.name}`);
        }
      } finally {
        await unlink(temporary);
      }
    } else {
      throw new Error(`일반 파일이 아닌 asset: ${entry.name}`);
    }
  }
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  if (process.argv.length !== 4) throw new Error("사용법: publish-assets.mjs <원본> <저장소>");
  await publishAssets(process.argv[2], process.argv[3]);
}
