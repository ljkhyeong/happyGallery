import { useQuery } from "@tanstack/react-query";
import { Button, Card, Col, Container, Row } from "react-bootstrap";
import { Link, useSearchParams } from "react-router-dom";
import { fetchMyPasses } from "@/features/my/api";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { buildPassTabs, getPassFilterKey } from "@/features/my/listUtils";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

const DEFAULT_SORT = "EXPIRY_ASC";
const PASS_SORT_OPTIONS = [
  { value: "EXPIRY_ASC", label: "만료 임박순" },
  { value: "PURCHASE_DESC", label: "최근 구매순" },
  { value: "CREDITS_DESC", label: "잔여 횟수 많은순" },
];

export function MyPassesPage() {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: passes, isLoading, error } = useQuery({
    queryKey: ["my", "passes"],
    queryFn: fetchMyPasses,
    enabled: isAuthenticated,
  });
  const searchQuery = searchParams.get("q") ?? "";
  const passFilter = searchParams.get("status") ?? searchParams.get("filter") ?? "ALL";
  const sortValue = searchParams.get("sort") ?? DEFAULT_SORT;
  const filteredPasses = (passes ?? []).filter((pass) => {
    const matchesFilter = passFilter === "ALL" || getPassFilterKey(pass) === passFilter;
    const normalizedQuery = searchQuery.trim();
    const matchesQuery = normalizedQuery === "" || String(pass.passId).includes(normalizedQuery);
    return matchesFilter && matchesQuery;
  });
  const sortedPasses = [...filteredPasses].sort((left, right) => {
    switch (sortValue) {
      case "PURCHASE_DESC":
        return new Date(right.purchasedAt).getTime() - new Date(left.purchasedAt).getTime();
      case "CREDITS_DESC":
        return right.remainingCredits - left.remainingCredits;
      case "EXPIRY_ASC":
      default:
        return new Date(left.expiresAt).getTime() - new Date(right.expiresAt).getTime();
    }
  });
  const quickTabs = buildPassTabs(passes ?? []);
  const activePassCount = (passes ?? []).filter((pass) => getPassFilterKey(pass) === "ACTIVE").length;
  const expiringSoonCount = (passes ?? []).filter((pass) => {
    const expiresIn = new Date(pass.expiresAt).getTime() - Date.now();
    return getPassFilterKey(pass) === "ACTIVE" && expiresIn <= 7 * 24 * 60 * 60 * 1000;
  }).length;
  const remainingCredits = (passes ?? []).reduce((sum, pass) => sum + pass.remainingCredits, 0);

  function updateFilters(next: { q?: string; status?: string; sort?: string }) {
    const nextSearchParams = new URLSearchParams(searchParams);
    const nextQuery = next.q ?? searchQuery;
    const nextStatus = next.status ?? passFilter;
    const nextSort = next.sort ?? sortValue;

    if (nextQuery.trim()) nextSearchParams.set("q", nextQuery.trim());
    else nextSearchParams.delete("q");

    if (nextStatus !== "ALL") nextSearchParams.set("status", nextStatus);
    else nextSearchParams.delete("status");
    nextSearchParams.delete("filter");

    if (nextSort !== DEFAULT_SORT) nextSearchParams.set("sort", nextSort);
    else nextSearchParams.delete("sort");

    setSearchParams(nextSearchParams, { replace: true });
  }

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 720 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="회원 8회권 목록은 로그인 후 내 정보에서 바로 확인할 수 있습니다."
        />
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <div className="my-detail-header">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start mb-3">
          <Link to="/my" className="text-decoration-none small">
            &larr; 내 정보
          </Link>
          <Button as={Link as any} to="/passes/purchase" variant="outline-secondary" size="sm">
            8회권 구매
          </Button>
        </div>
        <div className="my-section-kicker mb-2">My Passes</div>
        <h4 className="mb-2">전체 8회권</h4>
        <p className="text-muted-soft small mb-0">
          남은 횟수와 만료일을 기준으로 현재 사용할 수 있는 8회권을 정렬하고, 빠른 상태 탭으로 바로 좁힐 수 있습니다.
        </p>
      </div>

      <ErrorAlert error={error} />
      {passes && passes.length > 0 && (
        <div className="my-list-summary mb-3">
          <span className="my-summary-chip">사용 가능 {activePassCount}건</span>
          <span className="my-summary-chip">잔여 총 {remainingCredits}회</span>
          <span className="my-summary-chip">7일 내 만료 {expiringSoonCount}건</span>
        </div>
      )}
      {passes && passes.length > 0 && (
        <MyListFilterBar
          idPrefix="my-passes"
          searchLabel="8회권 번호 검색"
          searchPlaceholder="예: 12"
          searchValue={searchQuery}
          onSearchChange={(value) => updateFilters({ q: value })}
          filterLabel="상태"
          filterValue={passFilter}
          filterOptions={[
            { value: "ALL", label: "전체 상태" },
            { value: "ACTIVE", label: "사용 가능" },
            { value: "USED_UP", label: "사용 완료" },
            { value: "EXPIRED", label: "만료" },
          ]}
          onFilterChange={(value) => updateFilters({ status: value })}
          quickTabs={quickTabs}
          activeTabValue={passFilter}
          onTabChange={(value) => updateFilters({ status: value })}
          sortLabel="정렬"
          sortValue={sortValue}
          sortOptions={PASS_SORT_OPTIONS}
          onSortChange={(value) => updateFilters({ sort: value })}
          defaultSortValue={DEFAULT_SORT}
          resultText={`${sortedPasses.length} / ${passes.length}건 표시 중`}
          onReset={() => setSearchParams({}, { replace: true })}
        />
      )}
      {passes && passes.length === 0 && <EmptyState message="8회권이 없습니다." />}
      {passes && passes.length > 0 && sortedPasses.length === 0 && (
        <EmptyState message="필터 조건에 맞는 8회권이 없습니다." />
      )}
      {sortedPasses.length > 0 && sortedPasses.map((pass) => (
        <Card key={pass.passId} className="mb-2 my-list-card border-0">
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={4}>
                <div className="fw-semibold small">8회권 #{pass.passId}</div>
                <small className="text-muted-soft">구매 {formatDateTime(pass.purchasedAt)}</small>
              </Col>
              <Col xs={6} md={3}>
                <small>잔여 <strong>{pass.remainingCredits}</strong>/{pass.totalCredits}회</small>
              </Col>
              <Col xs={6} md={3}>
                <small>{formatKRW(pass.totalPrice)}</small>
              </Col>
              <Col xs={12} md={2} className="text-md-end">
                <small className="text-muted-soft">~{formatDateTime(pass.expiresAt)}</small>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      ))}
    </Container>
  );
}
