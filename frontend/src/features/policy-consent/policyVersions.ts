export const TERMS_POLICY_VERSION = "2026-07-21-v1";
export const PRIVACY_POLICY_VERSION = "2026-07-21-v1";

export function policyPath(type: "terms" | "privacy", version: string): string {
  return `/${type}/${version}`;
}
