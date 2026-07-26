export const POLICY_VERSION_2026_07_21_V1 = "2026-07-21-v1";

export const TERMS_POLICY_VERSION = POLICY_VERSION_2026_07_21_V1;
export const PRIVACY_POLICY_VERSION = POLICY_VERSION_2026_07_21_V1;

export function policyPath(type: "terms" | "privacy", version: string): string {
  return `/${type}/${version}`;
}
