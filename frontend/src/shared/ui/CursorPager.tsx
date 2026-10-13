import { Button, type ButtonProps } from "react-bootstrap";

interface CursorPageState {
  hasMore: boolean;
  nextCursor?: string | null;
}

interface Props {
  hasPreviousPage: boolean;
  /** 현재 페이지 응답. 아직 없으면 다음 버튼을 막는다. */
  page: CursorPageState | undefined;
  /** 조회 중이거나 선택이 잠긴 동안 두 버튼을 모두 막는다. */
  busy: boolean;
  onPrevious: () => void;
  onNext: (nextCursor: string) => void;
  className?: string;
  previousVariant?: ButtonProps["variant"];
  nextVariant?: ButtonProps["variant"];
}

/** 커서 목록의 이전·다음 이동. 앞뒤로 이동할 페이지가 없으면 그리지 않는다. */
export function CursorPager({
  hasPreviousPage,
  page,
  busy,
  onPrevious,
  onNext,
  className = "d-flex justify-content-center gap-2 mt-3",
  previousVariant = "outline-secondary",
  nextVariant = "outline-primary",
}: Props) {
  if (!hasPreviousPage && !page?.hasMore) return null;
  const nextCursor = page?.hasMore ? page.nextCursor : null;

  return (
    <div className={className}>
      <Button
        type="button"
        size="sm"
        variant={previousVariant}
        disabled={!hasPreviousPage || busy}
        onClick={onPrevious}
      >
        이전
      </Button>
      <Button
        type="button"
        size="sm"
        variant={nextVariant}
        disabled={!nextCursor || busy}
        onClick={() => {
          if (nextCursor) onNext(nextCursor);
        }}
      >
        다음
      </Button>
    </div>
  );
}
