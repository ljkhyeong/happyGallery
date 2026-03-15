import { Button, Card, Col, Form, Row } from "react-bootstrap";

export interface MyFilterOption {
  value: string;
  label: string;
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
  resultText,
  onReset,
}: Props) {
  const hasActiveFilter = searchValue.trim() !== "" || filterValue !== "ALL";

  return (
    <Card className="my-filter-card border-0 mb-3">
      <Card.Body className="p-3">
        <Row className="g-3 align-items-end">
          <Col md={5}>
            <Form.Group controlId={`${idPrefix}-search`}>
              <Form.Label>{searchLabel}</Form.Label>
              <Form.Control
                value={searchValue}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder={searchPlaceholder}
              />
            </Form.Group>
          </Col>
          <Col md={4}>
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
          <Col md={3}>
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
