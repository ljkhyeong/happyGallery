import { Button, ButtonGroup } from "react-bootstrap";
import type { OffsetPage } from "@/shared/types";

interface Props {
  page: Pick<OffsetPage<unknown>, "page" | "totalCount" | "totalPages">;
  onMove: (nextPage: number) => void;
  disabled: boolean;
  /** 같은 화면의 다른 페이지 이동과 구분할 이름 */
  ariaLabel?: string;
}

/** 관리자 검색 결과의 쪽 번호 이동과 전체 건수 */
export function AdminSearchPagination({
  page,
  onMove,
  disabled,
  ariaLabel = "검색 결과 페이지 이동",
}: Props) {
  if (page.totalPages === 0) {
    return <small className="d-block mt-3 text-muted-soft">총 0건</small>;
  }

  const currentPage = page.page + 1;
  return (
    <div className="admin-search-pagination">
      <small className="text-muted-soft">
        총 {page.totalCount.toLocaleString("ko-KR")}건 · {currentPage}/{page.totalPages.toLocaleString("ko-KR")} 페이지
      </small>
      <ButtonGroup size="sm" aria-label={ariaLabel}>
        <Button
          variant="outline-secondary"
          disabled={disabled || page.page === 0}
          onClick={() => onMove(page.page - 1)}
        >
          이전
        </Button>
        <Button
          variant="outline-secondary"
          disabled={disabled || page.page + 1 >= page.totalPages}
          onClick={() => onMove(page.page + 1)}
        >
          다음
        </Button>
      </ButtonGroup>
    </div>
  );
}
