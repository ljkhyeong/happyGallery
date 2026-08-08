import assert from "node:assert/strict";
import test from "node:test";

import {
  POLICY_VERSION_2026_07_21_V1,
  POLICY_VERSION_2026_08_08_V1,
  PRIVACY_POLICY_VERSION,
  TERMS_POLICY_VERSION,
  policyPath,
} from "../../src/features/policy-consent/policyVersions.ts";

test("새 약관과 개인정보처리방침 버전을 현재 기본값으로 사용한다", () => {
  assert.equal(POLICY_VERSION_2026_08_08_V1, "2026-08-08-v1");
  assert.equal(TERMS_POLICY_VERSION, POLICY_VERSION_2026_08_08_V1);
  assert.equal(PRIVACY_POLICY_VERSION, POLICY_VERSION_2026_08_08_V1);
  assert.equal(policyPath("terms", TERMS_POLICY_VERSION), "/terms/2026-08-08-v1");
  assert.equal(policyPath("privacy", PRIVACY_POLICY_VERSION), "/privacy/2026-08-08-v1");
});

test("이전 불변 정책 버전 경로도 계속 해석할 수 있게 유지한다", () => {
  assert.equal(POLICY_VERSION_2026_07_21_V1, "2026-07-21-v1");
  assert.equal(
    policyPath("terms", POLICY_VERSION_2026_07_21_V1),
    "/terms/2026-07-21-v1",
  );
  assert.equal(
    policyPath("privacy", POLICY_VERSION_2026_07_21_V1),
    "/privacy/2026-07-21-v1",
  );
});
