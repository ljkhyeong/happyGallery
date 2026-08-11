import { useEffect, useState } from "react";
import { Alert, Button } from "react-bootstrap";
import { fetchMyReviewImage } from "./api";

interface Props {
  reviewId: number;
  imageId: number;
  alt: string;
}

export function MemberReviewProtectedImage({ reviewId, imageId, alt }: Props) {
  const [objectUrl, setObjectUrl] = useState<string>();
  const [error, setError] = useState<unknown>();
  const [retryVersion, setRetryVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let disposed = false;
    let createdObjectUrl: string | undefined;

    setObjectUrl(undefined);
    setError(undefined);
    void fetchMyReviewImage(reviewId, imageId, controller.signal)
      .then((blob) => {
        if (disposed) return;
        createdObjectUrl = URL.createObjectURL(blob);
        setObjectUrl(createdObjectUrl);
      })
      .catch((nextError: unknown) => {
        if (disposed || controller.signal.aborted) return;
        setError(nextError);
      });

    return () => {
      disposed = true;
      controller.abort();
      if (createdObjectUrl) URL.revokeObjectURL(createdObjectUrl);
    };
  }, [imageId, retryVersion, reviewId]);

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
          onClick={() => setRetryVersion((current) => current + 1)}
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
