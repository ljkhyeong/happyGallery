import type { ComponentType } from "react";
import {
  PRIVACY_POLICY_DOCUMENTS,
  resolvePolicyDocument,
  TERMS_POLICY_DOCUMENTS,
} from "./policyDocuments";
import {
  PRIVACY_POLICY_VERSION,
  TERMS_POLICY_VERSION,
  policyPath,
} from "./policyVersions";

type PolicyKind = "terms" | "privacy";

const POLICIES = {
  terms: { documents: TERMS_POLICY_DOCUMENTS, currentVersion: TERMS_POLICY_VERSION },
  privacy: { documents: PRIVACY_POLICY_DOCUMENTS, currentVersion: PRIVACY_POLICY_VERSION },
} as const;

function notFound(): Response {
  return new Response(null, { status: 404, statusText: "Not Found" });
}

/**
 * 정책 문서 route loader 값. 모르는 버전은 문서 404로 응답하고,
 * 현재 버전은 버전 없는 대표 경로(`/terms`, `/privacy`)를 canonical로 쓴다.
 */
export function loadPolicyRoute(kind: PolicyKind, requestedVersion: string | undefined) {
  const { documents, currentVersion } = POLICIES[kind];
  const version = requestedVersion ?? currentVersion;
  if (!resolvePolicyDocument(documents, version)) throw notFound();
  const current = version === currentVersion;
  return { version, current, pathname: current ? `/${kind}` : policyPath(kind, version) };
}

/** loader가 확인한 버전의 문서 화면. 함수는 loader 응답으로 보낼 수 없어 화면에서 다시 고른다. */
export function policyDocument(kind: PolicyKind, version: string): ComponentType {
  const PolicyDocument = resolvePolicyDocument(POLICIES[kind].documents, version);
  if (!PolicyDocument) throw notFound();
  return PolicyDocument;
}
