import type { ReviewSummaryResponse } from "@/generated/api/review";

interface Props {
  summary: ReviewSummaryResponse;
  selectedRating?: number;
  onSelectRating: (rating?: number) => void;
}

export function ReviewHistogram({ summary, selectedRating, onSelectRating }: Props) {
  const counts = [5, 4, 3, 2, 1].map((rating) => ({
    rating,
    count: summary.histogram[`rating${rating}` as keyof typeof summary.histogram],
  }));
  const maxCount = Math.max(1, ...counts.map(({ count }) => count));

  return (
    <div className="review-histogram" aria-label="별점별 후기 분포">
      {counts.map(({ rating, count }) => (
        <button
          key={rating}
          type="button"
          className={`review-histogram-row${selectedRating === rating ? " active" : ""}`}
          aria-pressed={selectedRating === rating}
          onClick={() => onSelectRating(selectedRating === rating ? undefined : rating)}
        >
          <span>{rating}점</span>
          <span className="review-histogram-track" aria-hidden="true">
            <span style={{ width: `${(count / maxCount) * 100}%` }} />
          </span>
          <strong>{count.toLocaleString("ko-KR")}</strong>
        </button>
      ))}
    </div>
  );
}
