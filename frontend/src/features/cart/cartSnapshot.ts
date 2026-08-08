export function isCartSnapshotConflict(error: unknown): boolean {
  if (typeof error !== "object" || error === null) return false;
  const failure = error as {
    status?: unknown;
    code?: unknown;
  };
  return failure.status === 409
    && failure.code === "CART_SNAPSHOT_CHANGED";
}
