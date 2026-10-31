import type { ReactNode } from "react";
import { Button } from "react-bootstrap";

interface Props {
  /** 현재 쪽과 전체 건수 안내. 쪽 번호 기준(0/1-based)과 문구는 호출부가 정한다. */
  summary: ReactNode;
  previousDisabled: boolean;
  nextDisabled: boolean;
  onPrevious: () => void;
  onNext: () => void;
  /** 한 화면에 쪽 이동이 여럿이면 nav와 ariaLabel로 구분한다. */
  as?: "nav" | "div";
  ariaLabel?: string;
  previousLabel?: string;
  nextLabel?: string;
  className?: string;
  summaryClassName?: string;
}

/**
 * 오프셋 목록의 이전·다음 이동. 표시 조건과 두 버튼의 비활성 조건은 호출부가 계산해 넘긴다.
 * 검색 결과의 총 건수·쪽 번호 표기는 AdminSearchPagination을 사용한다.
 */
export function OffsetPager({
  summary,
  previousDisabled,
  nextDisabled,
  onPrevious,
  onNext,
  as: Container = "div",
  ariaLabel,
  previousLabel = "이전",
  nextLabel = "다음",
  className = "d-flex flex-wrap align-items-center gap-2 mt-3",
  summaryClassName = "small text-muted-soft",
}: Props) {
  return (
    <Container aria-label={ariaLabel} className={className}>
      <Button type="button" size="sm" variant="outline-secondary" disabled={previousDisabled} onClick={onPrevious}>
        {previousLabel}
      </Button>
      <span className={summaryClassName}>{summary}</span>
      <Button type="button" size="sm" variant="outline-secondary" disabled={nextDisabled} onClick={onNext}>
        {nextLabel}
      </Button>
    </Container>
  );
}
