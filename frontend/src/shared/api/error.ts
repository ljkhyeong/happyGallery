import type { ErrorCode } from "@/shared/types/error";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: ErrorCode | string,
    public override readonly message: string,
    public readonly requestId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }

  is(code: ErrorCode): boolean {
    return this.code === code;
  }
}
