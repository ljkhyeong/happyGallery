import assert from "node:assert/strict";
import test from "node:test";

import {
  MAX_REVIEW_IMAGE_BYTES,
  chunkReviewIds,
  chunkReviewIdsByPage,
  reviewImageSelectionError,
} from "../../src/features/review/reviewUiPolicy.ts";

test("회원별 후기 반응 ID를 API 최대 100개 단위로 나눈다", () => {
  const ids = Array.from({ length: 205 }, (_, index) => index + 1);

  const chunks = chunkReviewIds(ids);

  assert.deepEqual(chunks.map((chunk) => chunk.length), [100, 100, 5]);
  assert.deepEqual(chunks.flat(), ids);
});

test("공개 후기 반응 조회 묶음은 누적 목록을 다시 자르지 않고 각 공개 페이지 경계를 유지한다", () => {
  const firstPage = Array.from({ length: 60 }, (_, index) => index + 1);
  const secondPage = Array.from({ length: 60 }, (_, index) => index + 61);

  const chunks = chunkReviewIdsByPage([firstPage, secondPage]);

  assert.deepEqual(chunks, [firstPage, secondPage]);
  assert.notDeepEqual(chunks, chunkReviewIds([...firstPage, ...secondPage]));
});

test("후기 사진은 남은 개수와 지원 형식, 5MB 한도를 검사한다", () => {
  assert.equal(
    reviewImageSelectionError([
      { type: "image/jpeg", size: 10 },
      { type: "image/png", size: 10 },
    ], 1),
    "사진은 1장 더 등록할 수 있습니다.",
  );
  assert.equal(
    reviewImageSelectionError([{ type: "image/gif", size: 10 }], 2),
    "JPEG 또는 PNG 사진만 등록할 수 있습니다.",
  );
  assert.equal(
    reviewImageSelectionError([{ type: "image/webp", size: 10 }], 2),
    "JPEG 또는 PNG 사진만 등록할 수 있습니다.",
  );
  assert.equal(
    reviewImageSelectionError([{ type: "", size: 10 }], 2),
    null,
  );
  assert.equal(
    reviewImageSelectionError([{ type: "application/octet-stream", size: 10 }], 2),
    null,
  );
  assert.equal(
    reviewImageSelectionError([{ type: "text/plain", size: 10 }], 2),
    "JPEG 또는 PNG 사진만 등록할 수 있습니다.",
  );
  assert.equal(
    reviewImageSelectionError([{ type: "", size: 0 }], 2),
    "비어 있는 사진 파일은 등록할 수 없습니다.",
  );
  assert.equal(
    reviewImageSelectionError([{ type: "image/png", size: MAX_REVIEW_IMAGE_BYTES + 1 }], 2),
    "각 사진은 5MB 이하여야 합니다.",
  );
  assert.equal(
    reviewImageSelectionError([{ type: "image/png", size: MAX_REVIEW_IMAGE_BYTES }], 2),
    null,
  );
});
