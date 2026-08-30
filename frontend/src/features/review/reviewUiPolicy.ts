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

export function chunkReviewIdsByPage(
  reviewIdPages: number[][],
  batchSize = REVIEW_REACTION_BATCH_SIZE,
): number[][] {
  return reviewIdPages.flatMap((reviewIds) => chunkReviewIds(reviewIds, batchSize));
}

export function reviewImageSelectionError(
  files: ReviewImageCandidate[],
  remainingCount: number,
): string | null {
  if (files.length > remainingCount) {
    return `사진은 ${remainingCount}장 더 등록할 수 있습니다.`;
  }
  if (files.some((file) => file.size === 0)) {
    return "비어 있는 사진 파일은 등록할 수 없습니다.";
  }
  if (files.some((file) => {
    const mimeType = file.type.trim().toLowerCase();
    return mimeType !== ""
      && mimeType !== "application/octet-stream"
      && !ACCEPTED_REVIEW_IMAGE_TYPES.has(mimeType);
  })) {
    return "JPEG 또는 PNG 사진만 등록할 수 있습니다.";
  }
  if (files.some((file) => file.size > MAX_REVIEW_IMAGE_BYTES)) {
    return "각 사진은 5MB 이하여야 합니다.";
  }
  return null;
}
