import { Button, Form } from "react-bootstrap";
import type { ReviewSort } from "./api";

interface Props {
  rating?: number;
  sort: ReviewSort;
  filteredCount: number;
  totalCount: number;
  onRatingChange: (rating?: number) => void;
  onSortChange: (sort: ReviewSort) => void;
}

export function ReviewFilters({
  rating,
  sort,
  filteredCount,
  totalCount,
  onRatingChange,
  onSortChange,
}: Props) {
  return (
    <div className="review-filter-bar">
      <div className="review-rating-filters" role="group" aria-label="별점 필터">
        {[undefined, 5, 4, 3, 2, 1].map((value) => (
          <Button
            key={value ?? "all"}
            type="button"
            size="sm"
            variant={rating === value ? "dark" : "outline-secondary"}
            aria-pressed={rating === value}
            onClick={() => onRatingChange(value)}
          >
            {value ? `${value}점` : "전체"}
          </Button>
        ))}
      </div>
      <div className="review-filter-result">
        <span aria-live="polite">
          {rating ? `${rating}점 후기 ${filteredCount.toLocaleString("ko-KR")}개` : `전체 ${totalCount.toLocaleString("ko-KR")}개`}
        </span>
        <Form.Select
          size="sm"
          aria-label="후기 정렬"
          value={sort}
          onChange={(event) => onSortChange(event.target.value as ReviewSort)}
        >
          <option value="LATEST">최신순</option>
          <option value="RATING_HIGH">별점 높은순</option>
          <option value="RATING_LOW">별점 낮은순</option>
        </Form.Select>
      </div>
    </div>
  );
}
