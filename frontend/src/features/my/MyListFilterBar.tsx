import { Badge, Button, Card, Col, Form, Row } from "react-bootstrap";

export interface MyFilterOption {
  value: string;
  label: string;
}

export interface MyQuickTab extends MyFilterOption {
  count: number;
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
          <div className="my-quick-tabs mb-3" role="tablist" aria-label={`${idPrefix}-quick-tabs`}>
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
                >
                  <span>{tab.label}</span>
                  <Badge bg={isActive ? "light" : "secondary"} text={isActive ? "dark" : "light"}>
                    {tab.count}
                  </Badge>
                </Button>
              );
            })}
          </div>
        )}
        <Row className="g-3 align-items-end">
          <Col md={hasSort ? 4 : 5}>
            <Form.Group controlId={`${idPrefix}-search`}>
              <Form.Label>{searchLabel}</Form.Label>
              <Form.Control
                value={searchValue}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder={searchPlaceholder}
              />
            </Form.Group>
          </Col>
          <Col md={hasSort ? 3 : 4}>
            <Form.Group controlId={`${idPrefix}-filter`}>
              <Form.Label>{filterLabel}</Form.Label>
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
          </Col>
          {hasSort && (
            <Col md={3}>
              <Form.Group controlId={`${idPrefix}-sort`}>
                <Form.Label>{sortLabel}</Form.Label>
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
            </Col>
          )}
          <Col md={hasSort ? 2 : 3}>
            <div className="d-grid">
              <Button
                variant="outline-secondary"
                onClick={onReset}
                disabled={!hasActiveFilter}
              >
                초기화
              </Button>
            </div>
          </Col>
        </Row>
        <div className="my-filter-result mt-3">{resultText}</div>
      </Card.Body>
    </Card>
  );
}
