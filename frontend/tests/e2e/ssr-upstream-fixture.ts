const frontendPort = Number(process.env.PLAYWRIGHT_FRONTEND_PORT ?? 3000);
const ssrUpstreamPort = Number(
  process.env.PLAYWRIGHT_SSR_UPSTREAM_PORT ?? frontendPort + 1,
);
const SSR_UPSTREAM_ORIGIN = process.env.PLAYWRIGHT_SSR_UPSTREAM_ORIGIN
  ?? `http://127.0.0.1:${ssrUpstreamPort}`;
let fixturesRegistered = false;

export interface SsrUpstreamRoute {
  method?: string;
  path: string;
  status?: number;
  headers?: Record<string, string>;
  json: unknown;
}

export function ssrApiFixture(path: string, json: unknown): SsrUpstreamRoute {
  return { path: `/api/v1${path}`, json };
}

interface HomeSsrData {
  workshop: unknown;
  products?: unknown[];
  classes?: unknown[];
  events?: unknown[];
  notices?: unknown[];
}

export function homeSsrFixtures({
  workshop,
  products = [],
  classes = [],
  events = [],
  notices = [],
}: HomeSsrData): SsrUpstreamRoute[] {
  return [
    ssrApiFixture("/products", products),
    ssrApiFixture("/classes", classes),
    ssrApiFixture("/events", events),
    ssrApiFixture("/notices", notices),
    ssrApiFixture("/workshop", workshop),
  ];
}

export async function replaceSsrUpstreamFixtures(
  ...routes: SsrUpstreamRoute[]
): Promise<void> {
  const response = await fetch(`${SSR_UPSTREAM_ORIGIN}/__e2e/fixtures`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ routes }),
  });
  if (!response.ok) {
    throw new Error(`SSR upstream fixture registration failed: ${response.status}`);
  }
  fixturesRegistered = true;
}

export async function clearSsrUpstreamFixtures(): Promise<void> {
  if (!fixturesRegistered) return;
  const response = await fetch(`${SSR_UPSTREAM_ORIGIN}/__e2e/fixtures`, {
    method: "DELETE",
  });
  if (!response.ok) {
    throw new Error(`SSR upstream fixture cleanup failed: ${response.status}`);
  }
  fixturesRegistered = false;
}
