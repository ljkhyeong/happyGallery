import type { ComponentType } from "react";
import { PrivacyPolicy20260721V1 } from "./documents/PrivacyPolicy20260721V1";
import { PrivacyPolicy20260808V1 } from "./documents/PrivacyPolicy20260808V1";
import { PrivacyPolicy20260811V1 } from "./documents/PrivacyPolicy20260811V1";
import { PrivacyPolicy20260811V2 } from "./documents/PrivacyPolicy20260811V2";
import { TermsPolicy20260721V1 } from "./documents/TermsPolicy20260721V1";
import { TermsPolicy20260808V1 } from "./documents/TermsPolicy20260808V1";
import {
  POLICY_VERSION_2026_07_21_V1,
  POLICY_VERSION_2026_08_08_V1,
  POLICY_VERSION_2026_08_11_V1,
  POLICY_VERSION_2026_08_11_V2,
} from "./policyVersions";

type PolicyDocumentRegistry = Readonly<Record<string, ComponentType>>;

export const TERMS_POLICY_DOCUMENTS: PolicyDocumentRegistry = Object.freeze({
  [POLICY_VERSION_2026_07_21_V1]: TermsPolicy20260721V1,
  [POLICY_VERSION_2026_08_08_V1]: TermsPolicy20260808V1,
});

export const PRIVACY_POLICY_DOCUMENTS: PolicyDocumentRegistry = Object.freeze({
  [POLICY_VERSION_2026_07_21_V1]: PrivacyPolicy20260721V1,
  [POLICY_VERSION_2026_08_08_V1]: PrivacyPolicy20260808V1,
  [POLICY_VERSION_2026_08_11_V1]: PrivacyPolicy20260811V1,
  [POLICY_VERSION_2026_08_11_V2]: PrivacyPolicy20260811V2,
});

export function resolvePolicyDocument(
  documents: PolicyDocumentRegistry,
  version: string,
): ComponentType | undefined {
  return Object.prototype.hasOwnProperty.call(documents, version)
    ? documents[version]
    : undefined;
}
