export const POLICY_VERSION_2026_07_21_V1 = "2026-07-21-v1";
export const POLICY_VERSION_2026_08_08_V1 = "2026-08-08-v1";
export const POLICY_VERSION_2026_08_11_V1 = "2026-08-11-v1";
export const POLICY_VERSION_2026_08_11_V2 = "2026-08-11-v2";

export const TERMS_POLICY_VERSION = POLICY_VERSION_2026_08_08_V1;
export const PRIVACY_POLICY_VERSION = POLICY_VERSION_2026_08_11_V2;

export function policyPath(type: "terms" | "privacy", version: string): string {
  return `/${type}/${version}`;
}
