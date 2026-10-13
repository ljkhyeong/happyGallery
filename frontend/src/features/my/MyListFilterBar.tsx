import { Search } from "lucide-react";
import { Button, Card, Form } from "react-bootstrap";
import type { MyListFilters } from "./useMyListFilters";

export interface MyFilterOption {
  value: string;
  label: string;
}

interface Props {
  idPrefix: string;
  searchLabel: string;
  searchPlaceholder: string;
  filters: MyListFilters;
  statusOptions: MyFilterOption[];
  quickTabs: MyFilterOption[];
  sortOptions: readonly MyFilterOption[];
  count: number;
  hasMore: boolean;
}

/** 내 정보 예약·주문·이용권 이력의 검색·상태·정렬 도구 막대. */
export function MyListFilterBar({
  idPrefix,
  searchLabel,
  searchPlaceholder,
  filters,
  statusOptions,
  quickTabs,
  sortOptions,
  count,
  hasMore,
}: Props) {
  const { searchQuery, statusFilter, sortValue, defaultSort, updateFilters, resetFilters } = filters;
  const hasActiveFilter = searchQuery.trim() !== ""
    || statusFilter !== "ALL"
    || sortValue !== defaultSort;

  return (
    <Card className="my-filter-card border-0 mb-3">
      <Card.Body className="p-3">
        <div className="my-quick-tabs mb-3" role="group" aria-label="상태별 보기">
          {quickTabs.map((tab) => {
            const isActive = tab.value === statusFilter;
            return (
              <Button
                key={tab.value}
                type="button"
                size="sm"
                variant={isActive ? "dark" : "outline-secondary"}
                className="my-quick-tab"
                onClick={() => updateFilters({ status: tab.value })}
                aria-pressed={isActive}
              >
                <span>{tab.label}</span>
              </Button>
            );
          })}
        </div>
        {/* 작품 목록과 같은 도구 막대: 선택 상자 값이 스스로 설명하므로 라벨은 보조기기용으로만 둔다. */}
        <div className="store-filter-controls">
          <Form.Group controlId={`${idPrefix}-search`} className="store-filter-search">
            <Form.Label className="visually-hidden">{searchLabel}</Form.Label>
            <Search className="store-filter-search-icon" size={16} aria-hidden="true" />
            <Form.Control
              type="search"
              value={searchQuery}
              onChange={(event) => updateFilters({ q: event.target.value })}
              placeholder={searchPlaceholder}
            />
          </Form.Group>
          <Form.Group controlId={`${idPrefix}-filter`} className="store-filter-select">
            <Form.Label className="visually-hidden">상태</Form.Label>
            <Form.Select
              value={statusFilter}
              onChange={(event) => updateFilters({ status: event.target.value })}
            >
              {statusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          <Form.Group controlId={`${idPrefix}-sort`} className="store-filter-select">
            <Form.Label className="visually-hidden">정렬</Form.Label>
            <Form.Select
              value={sortValue}
              onChange={(event) => updateFilters({ sort: event.target.value })}
            >
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </div>
        <div className="store-filter-meta">
          <span className="my-filter-result">
            {`검색 결과 ${count}건 표시 중${hasMore ? " · 더 보기로 계속 조회" : ""}`}
          </span>
          <Button variant="link" size="sm" onClick={resetFilters} disabled={!hasActiveFilter}>
            초기화
          </Button>
        </div>
      </Card.Body>
    </Card>
  );
}
