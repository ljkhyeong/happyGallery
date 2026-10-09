import assert from "node:assert/strict";
import test from "node:test";

import {
  POLICY_VERSION_2026_07_21_V1,
  policyPath,
} from "../../src/features/policy-consent/policyVersions.ts";

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
