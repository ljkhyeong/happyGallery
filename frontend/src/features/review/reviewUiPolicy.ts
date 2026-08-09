export const REVIEW_REACTION_BATCH_SIZE = 100;
export const MAX_REVIEW_IMAGES = 5;
export const MAX_REVIEW_IMAGE_BYTES = 5 * 1024 * 1024;

const ACCEPTED_REVIEW_IMAGE_TYPES = new Set([
  "image/jpeg",
  "image/png",
]);

interface ReviewImageCandidate {
  type: string;
  size: number;
}

export function chunkReviewIds(
  reviewIds: number[],
  batchSize = REVIEW_REACTION_BATCH_SIZE,
): number[][] {
  if (!Number.isSafeInteger(batchSize) || batchSize < 1) {
    throw new Error("후기 반응 조회 묶음 크기는 1 이상이어야 합니다.");
  }

  const chunks: number[][] = [];
  for (let index = 0; index < reviewIds.length; index += batchSize) {
    chunks.push(reviewIds.slice(index, index + batchSize));
  }
  return chunks;
}

export function reviewImageSelectionError(
  files: ReviewImageCandidate[],
  remainingCount: number,
): string | null {
  if (files.length > remainingCount) {
    return `사진은 ${remainingCount}장 더 등록할 수 있습니다.`;
  }
  if (files.some((file) => !ACCEPTED_REVIEW_IMAGE_TYPES.has(file.type))) {
    return "JPEG 또는 PNG 사진만 등록할 수 있습니다.";
  }
  if (files.some((file) => file.size > MAX_REVIEW_IMAGE_BYTES)) {
    return "각 사진은 5MB 이하여야 합니다.";
  }
  return null;
}
