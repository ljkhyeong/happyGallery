import { useCallback, useEffect, useState } from "react";
import { Alert, Button } from "react-bootstrap";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { fetchAdminReviewEvidenceImage, fetchAdminReviewImage } from "./api";

type ProtectedImageSource =
  | { kind: "evidence"; evidenceId: number; sortOrder: number }
  | { kind: "review"; reviewId: number; imageId: number };

interface Props {
  adminKey: string;
  source: ProtectedImageSource;
  alt: string;
  onAuthError: () => void;
}

export function AdminReviewProtectedImage({
  adminKey,
  source,
  alt,
  onAuthError,
}: Props) {
  const [objectUrl, setObjectUrl] = useState<string>();
  const [error, setError] = useState<unknown>();
  const [retryVersion, setRetryVersion] = useState(0);
  const evidenceId = source.kind === "evidence" ? source.evidenceId : undefined;
  const sortOrder = source.kind === "evidence" ? source.sortOrder : undefined;
  const reviewId = source.kind === "review" ? source.reviewId : undefined;
  const imageId = source.kind === "review" ? source.imageId : undefined;
  const subject = source.kind === "evidence" ? "사진 증거" : "숨김 후기 사진";
  const objectParticle = source.kind === "evidence" ? "를" : "을";

  const loadImage = useCallback((signal: AbortSignal) => {
    if (evidenceId !== undefined && sortOrder !== undefined) {
      return fetchAdminReviewEvidenceImage(adminKey, evidenceId, sortOrder, signal);
    }
    if (reviewId !== undefined && imageId !== undefined) {
      return fetchAdminReviewImage(adminKey, reviewId, imageId, signal);
    }
    throw new Error("보호 이미지 식별자가 올바르지 않습니다.");
  }, [adminKey, evidenceId, imageId, reviewId, sortOrder]);

  useEffect(() => {
    const controller = new AbortController();
    let disposed = false;
    let createdObjectUrl: string | undefined;

    setObjectUrl(undefined);
    setError(undefined);
    void loadImage(controller.signal)
      .then((blob) => {
        if (disposed) return;
        createdObjectUrl = URL.createObjectURL(blob);
        setObjectUrl(createdObjectUrl);
      })
      .catch((nextError: unknown) => {
        if (disposed || controller.signal.aborted) return;
        if (isAdminSessionUnauthorized(nextError)) onAuthError();
        setError(nextError);
      });

    return () => {
      disposed = true;
      controller.abort();
      if (createdObjectUrl) URL.revokeObjectURL(createdObjectUrl);
    };
  }, [loadImage, onAuthError, retryVersion]);

  if (error) {
    return (
      <Alert
        variant="warning"
        className="review-image-link d-flex flex-column align-items-center justify-content-center gap-2 p-2 small mb-0"
        role="alert"
        aria-label={`${alt} 불러오기 실패`}
      >
        <span>{subject}{objectParticle} 불러오지 못했습니다.</span>
        <Button
          type="button"
          size="sm"
          variant="outline-dark"
          onClick={() => setRetryVersion((current) => current + 1)}
        >
          다시 시도
        </Button>
      </Alert>
    );
  }

  if (!objectUrl) {
    return (
      <div
        className="review-image-link d-flex align-items-center justify-content-center p-2 small text-center"
        role="status"
        aria-label={`${alt} 불러오는 중`}
      >
        {subject}{objectParticle} 불러오는 중입니다.
      </div>
    );
  }

  return (
    <a href={objectUrl} target="_blank" rel="noreferrer" className="review-image-link">
      <img src={objectUrl} alt={alt} />
    </a>
  );
}
