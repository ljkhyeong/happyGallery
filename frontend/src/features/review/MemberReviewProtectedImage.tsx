import { useCallback } from "react";
import { Alert, Button } from "react-bootstrap";
import { useBlobObjectUrl } from "@/shared/hooks/useBlobObjectUrl";
import { fetchMyReviewImage } from "./api";

interface Props {
  reviewId: number;
  imageId: number;
  alt: string;
}

export function MemberReviewProtectedImage({ reviewId, imageId, alt }: Props) {
  const loadImage = useCallback(
    (signal: AbortSignal) => fetchMyReviewImage(reviewId, imageId, signal),
    [imageId, reviewId],
  );
  const { url: objectUrl, error, retry } = useBlobObjectUrl(loadImage);

  if (error) {
    return (
      <Alert
        variant="warning"
        className="d-flex flex-column align-items-center justify-content-center gap-1 p-2 small mb-0"
        role="alert"
        aria-label={`${alt} 불러오기 실패`}
      >
        <span>사진을 불러오지 못했습니다.</span>
        <Button
          type="button"
          size="sm"
          variant="outline-dark"
          onClick={retry}
        >
          다시 시도
        </Button>
      </Alert>
    );
  }

  if (!objectUrl) {
    return <span role="status" className="small text-muted-soft">사진을 불러오는 중입니다.</span>;
  }

  return <img src={objectUrl} alt={alt} />;
}
