import express from "express";
import path from "node:path";
import { pathToFileURL } from "node:url";

export function createAssetApp(directory) {
  const app = express();
  app.disable("x-powered-by");
  app.get("/healthz", (_request, response) => response.send("ok"));
  app.get("/assets/happygallery-asset-store-v1.txt", (_request, response) => {
    response.set("Cache-Control", "no-store").type("text/plain").send("shared-assets-v1");
  });
  app.use("/assets", express.static(directory, { immutable: true, maxAge: "1y", dotfiles: "deny" }));
  app.use((_request, response) => response.sendStatus(404));
  return app;
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  const server = createAssetApp(process.env.ASSET_DIRECTORY ?? "/assets")
    .listen(Number(process.env.PORT ?? 8080), "0.0.0.0");
  const shutdown = () => server.close();
  process.once("SIGTERM", shutdown);
  process.once("SIGINT", shutdown);
}
