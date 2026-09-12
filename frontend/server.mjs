import { pathToFileURL } from "node:url";
import path from "node:path";
import compression from "compression";
import express from "express";
import { createRequestHandler } from "@react-router/express";

export function createFrontendApp({
  requestHandler, clientDirectory, publicPath = "/", indexNowKey = process.env.INDEXNOW_KEY ?? "",
}) {
  const app = express();
  app.disable("x-powered-by");
  app.use(compression());

  if (indexNowKey) {
    if (indexNowKey.length < 8 || indexNowKey.length > 128 || /[^a-zA-Z0-9-]/.test(indexNowKey)) {
      throw new Error("INDEXNOW_KEY는 영문·숫자·하이픈 8~128자여야 합니다.");
    }
    app.get(`/${indexNowKey}.txt`, (_request, response) => {
      response.set("Cache-Control", "no-store").type("text/plain").send(indexNowKey);
    });
  }

  if (clientDirectory) {
    app.use(
      path.posix.join(publicPath, "assets"),
      express.static(path.join(clientDirectory, "assets"), {
        immutable: true,
        maxAge: "1y",
      }),
    );
    app.use(publicPath, express.static(clientDirectory, { maxAge: "1h" }));
  }

  // 결제 callback에는 paymentKey가 있으므로 요청 URL access log를 남기지 않는다.
  app.all("/{*splat}", requestHandler);
  return app;
}

export async function startServer() {
  const buildPath = path.resolve("build/server/index.js");
  const build = await import(pathToFileURL(buildPath).href);
  const app = createFrontendApp({
    requestHandler: createRequestHandler({
      build,
      mode: process.env.NODE_ENV ?? "production",
    }),
    clientDirectory: path.resolve(build.assetsBuildDirectory),
    publicPath: build.publicPath,
  });
  const host = process.env.HOST ?? "0.0.0.0";
  const port = Number(process.env.PORT ?? 3000);
  const server = app.listen(port, host, () => {
    console.info(`[happygallery-frontend] listening on ${host}:${port}`);
  });

  const shutdown = () => {
    server.close((error) => {
      if (error) {
        console.error("[happygallery-frontend] graceful shutdown failed", error);
        process.exitCode = 1;
      }
    });
  };
  process.once("SIGTERM", shutdown);
  process.once("SIGINT", shutdown);

  return server;
}

const invokedPath = process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href;
if (invokedPath === import.meta.url) {
  await startServer();
}
