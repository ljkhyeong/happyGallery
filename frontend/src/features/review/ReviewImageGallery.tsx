import { useState } from "react";
import { ImageOff } from "lucide-react";
import type { ReviewImageResponse } from "@/generated/api/review";

interface Props {
  images: ReviewImageResponse[];
  label?: string;
}

export function ReviewImageGallery({ images, label = "후기 사진" }: Props) {
  if (images.length === 0) return null;

  return (
    <div className="review-image-gallery" aria-label={label}>
      {images.map((image, index) => (
        <ReviewImage
          key={`${image.id}-${image.imageUrl}`}
          image={image}
          label={label}
          index={index}
        />
      ))}
    </div>
  );
}

function ReviewImage({
  image,
  label,
  index,
}: {
  image: ReviewImageResponse;
  label: string;
  index: number;
}) {
  const [failed, setFailed] = useState(false);
  const imageLabel = `${label} ${index + 1}`;

  if (failed) {
    return (
      <div
        className="review-image-link review-image-unavailable"
        role="status"
        aria-label={`${imageLabel} 불러오기 실패`}
      >
        <ImageOff size={20} aria-hidden="true" />
        <span>사진을 표시할 수 없습니다.</span>
      </div>
    );
  }

  return (
    <a
      href={image.imageUrl}
      className="review-image-link"
      target="_blank"
      rel="noreferrer"
      aria-label={`${imageLabel} 크게 보기`}
    >
      <img
        src={image.imageUrl}
        alt={imageLabel}
        loading="lazy"
        onError={() => setFailed(true)}
      />
    </a>
  );
}
