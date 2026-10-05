import { Search } from "lucide-react";
import { Badge, Button, Card, Form } from "react-bootstrap";

export interface MyFilterOption {
  value: string;
  label: string;
}

export interface MyQuickTab extends MyFilterOption {
  count?: number;
}

interface Props {
  idPrefix: string;
  searchLabel: string;
  searchPlaceholder: string;
  searchValue: string;
  onSearchChange: (value: string) => void;
  filterLabel: string;
  filterValue: string;
  filterOptions: MyFilterOption[];
  onFilterChange: (value: string) => void;
  quickTabs?: MyQuickTab[];
  activeTabValue?: string;
  onTabChange?: (value: string) => void;
  sortLabel?: string;
  sortValue?: string;
  sortOptions?: MyFilterOption[];
  onSortChange?: (value: string) => void;
  defaultSortValue?: string;
  resultText: string;
  onReset: () => void;
}

export function MyListFilterBar({
  idPrefix,
  searchLabel,
  searchPlaceholder,
  searchValue,
  onSearchChange,
  filterLabel,
  filterValue,
  filterOptions,
  onFilterChange,
  quickTabs,
  activeTabValue,
  onTabChange,
  sortLabel,
  sortValue,
  sortOptions,
  onSortChange,
  defaultSortValue,
  resultText,
  onReset,
}: Props) {
  const hasSort = !!sortOptions?.length && !!sortValue && !!onSortChange;
  const hasActiveFilter =
    searchValue.trim() !== "" ||
    filterValue !== "ALL" ||
    (!!sortValue && !!defaultSortValue && sortValue !== defaultSortValue);

  return (
    <Card className="my-filter-card border-0 mb-3">
      <Card.Body className="p-3">
        {!!quickTabs?.length && !!activeTabValue && !!onTabChange && (
          <div className="my-quick-tabs mb-3" role="group" aria-label="상태별 보기">
            {quickTabs.map((tab) => {
              const isActive = tab.value === activeTabValue;
              return (
                <Button
                  key={tab.value}
                  type="button"
                  size="sm"
                  variant={isActive ? "dark" : "outline-secondary"}
                  className="my-quick-tab"
                  onClick={() => onTabChange(tab.value)}
                  aria-pressed={isActive}
                >
                  <span>{tab.label}</span>
                  {tab.count !== undefined && (
                    <Badge bg={isActive ? "light" : "secondary"} text={isActive ? "dark" : "light"}>
                      {tab.count}
                    </Badge>
                  )}
                </Button>
              );
            })}
          </div>
        )}
        {/* 작품 목록과 같은 도구 막대: 선택 상자 값이 스스로 설명하므로 라벨은 보조기기용으로만 둔다. */}
        <div className="store-filter-controls">
          <Form.Group controlId={`${idPrefix}-search`} className="store-filter-search">
            <Form.Label className="visually-hidden">{searchLabel}</Form.Label>
            <Search className="store-filter-search-icon" size={16} aria-hidden="true" />
            <Form.Control
              type="search"
              value={searchValue}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder={searchPlaceholder}
            />
          </Form.Group>
          <Form.Group controlId={`${idPrefix}-filter`} className="store-filter-select">
            <Form.Label className="visually-hidden">{filterLabel}</Form.Label>
            <Form.Select
              value={filterValue}
              onChange={(event) => onFilterChange(event.target.value)}
            >
              {filterOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          {hasSort && (
            <Form.Group controlId={`${idPrefix}-sort`} className="store-filter-select">
              <Form.Label className="visually-hidden">{sortLabel}</Form.Label>
              <Form.Select
                value={sortValue}
                onChange={(event) => onSortChange(event.target.value)}
              >
                {sortOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          )}
        </div>
        <div className="store-filter-meta">
          <span className="my-filter-result">{resultText}</span>
          <Button variant="link" size="sm" onClick={onReset} disabled={!hasActiveFilter}>
            초기화
          </Button>
        </div>
      </Card.Body>
    </Card>
  );
}
