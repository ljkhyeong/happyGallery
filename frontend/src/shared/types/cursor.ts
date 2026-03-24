export interface CursorPage<T> {
  content: T[];
  nextCursor: string | null;
  hasMore: boolean;
}
