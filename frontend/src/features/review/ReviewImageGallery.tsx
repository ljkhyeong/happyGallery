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
        <a
          key={image.id}
          href={image.imageUrl}
          className="review-image-link"
          target="_blank"
          rel="noreferrer"
          aria-label={`${label} ${index + 1} 크게 보기`}
        >
          <img
            src={image.imageUrl}
            alt={`${label} ${index + 1}`}
            loading="lazy"
          />
        </a>
      ))}
    </div>
  );
}
