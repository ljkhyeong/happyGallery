export function loader() {
  return Response.json(
    { status: "UP" },
    { headers: { "Cache-Control": "no-store" } },
  );
}
