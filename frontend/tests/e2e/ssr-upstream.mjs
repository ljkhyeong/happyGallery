import { createServer } from "node:http";

const host = "127.0.0.1";
const port = Number(process.env.PLAYWRIGHT_SSR_UPSTREAM_PORT ?? 3001);
const backendOrigin = process.env.PLAYWRIGHT_BACKEND_ORIGIN ?? "http://127.0.0.1:8080";
const fixtures = new Map();

const HOP_BY_HOP_HEADERS = new Set([
  "connection",
  "content-length",
  "host",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
]);

function fixtureKey(method, url) {
  return `${method.toUpperCase()} ${url.pathname}${url.search}`;
}

async function readBody(request) {
  const chunks = [];
  for await (const chunk of request) chunks.push(chunk);
  return Buffer.concat(chunks);
}

function writeJson(response, status, body, headers = {}) {
  response.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    ...headers,
  });
  response.end(JSON.stringify(body));
}

async function replaceFixtures(request, response) {
  const body = JSON.parse((await readBody(request)).toString("utf8"));
  fixtures.clear();
  for (const route of body.routes ?? []) {
    const method = route.method ?? "GET";
    const url = new URL(route.path, "http://fixture.local");
    fixtures.set(fixtureKey(method, url), route);
  }
  writeJson(response, 200, { count: fixtures.size });
}

function writeFixture(response, fixture) {
  writeJson(
    response,
    fixture.status ?? 200,
    fixture.json,
    fixture.headers ?? {},
  );
}

function proxyHeaders(requestHeaders) {
  const headers = new Headers();
  for (const [name, value] of Object.entries(requestHeaders)) {
    if (value === undefined || HOP_BY_HOP_HEADERS.has(name.toLowerCase())) continue;
    for (const item of Array.isArray(value) ? value : [value]) {
      headers.append(name, item);
    }
  }
  headers.set("accept-encoding", "identity");
  return headers;
}

async function proxyToBackend(request, response, requestUrl) {
  const method = request.method ?? "GET";
  const body = method === "GET" || method === "HEAD" ? undefined : await readBody(request);
  const upstream = await fetch(new URL(requestUrl.pathname + requestUrl.search, backendOrigin), {
    method,
    headers: proxyHeaders(request.headers),
    body: body?.length ? body : undefined,
    redirect: "manual",
  });

  response.statusCode = upstream.status;
  for (const [name, value] of upstream.headers) {
    if (name === "set-cookie" || HOP_BY_HOP_HEADERS.has(name.toLowerCase())) continue;
    response.setHeader(name, value);
  }
  const setCookies = upstream.headers.getSetCookie();
  if (setCookies.length > 0) response.setHeader("set-cookie", setCookies);
  response.end(Buffer.from(await upstream.arrayBuffer()));
}

const server = createServer(async (request, response) => {
  const requestUrl = new URL(request.url ?? "/", `http://${host}:${port}`);

  try {
    if (request.method === "GET" && requestUrl.pathname === "/__e2e/health") {
      writeJson(response, 200, { status: "UP" });
      return;
    }
    if (request.method === "PUT" && requestUrl.pathname === "/__e2e/fixtures") {
      await replaceFixtures(request, response);
      return;
    }
    if (request.method === "DELETE" && requestUrl.pathname === "/__e2e/fixtures") {
      fixtures.clear();
      response.writeHead(204).end();
      return;
    }

    const fixture = fixtures.get(fixtureKey(request.method ?? "GET", requestUrl));
    if (fixture) {
      writeFixture(response, fixture);
      return;
    }
    await proxyToBackend(request, response, requestUrl);
  } catch (error) {
    console.error(error);
    if (!response.headersSent) {
      writeJson(response, 502, { code: "TEST_UPSTREAM_FAILURE" });
    } else {
      response.destroy();
    }
  }
});

server.listen(port, host, () => {
  console.log(`Playwright SSR upstream listening on http://${host}:${port}`);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
