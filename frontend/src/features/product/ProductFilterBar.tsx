import { Button, Card, Col, Form, Row } from "react-bootstrap";
import { PRODUCT_TYPE_LABEL, PRODUCT_SORT_LABEL } from "@/shared/lib";
import type { ProductSortOrder } from "@/shared/types";

interface Props {
  keyword: string;
  onKeywordChange: (value: string) => void;
  type: string;
  onTypeChange: (value: string) => void;
  category: string;
  onCategoryChange: (value: string) => void;
  categories: string[];
  sort: ProductSortOrder;
  onSortChange: (value: ProductSortOrder) => void;
  resultText: string;
  onReset: () => void;
}

const TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: "ALL", label: "전체 타입" },
  ...Object.entries(PRODUCT_TYPE_LABEL).map(([value, label]) => ({ value, label })),
];

const SORT_OPTIONS: { value: ProductSortOrder; label: string }[] = (
  Object.entries(PRODUCT_SORT_LABEL) as [ProductSortOrder, string][]
).map(([value, label]) => ({ value, label }));

export function ProductFilterBar({
  keyword,
  onKeywordChange,
  type,
  onTypeChange,
  category,
  onCategoryChange,
  categories,
  sort,
  onSortChange,
  resultText,
  onReset,
}: Props) {
  const categoryOptions = [
    { value: "ALL", label: "전체 카테고리" },
    ...categories.map((c) => ({ value: c, label: c })),
  ];

  const hasActiveFilter =
    keyword.trim() !== "" || type !== "ALL" || category !== "ALL" || sort !== "newest";

  return (
    <Card className="my-filter-card border-0 mb-3">
      <Card.Body className="p-3">
        <Row className="g-3 align-items-end">
          <Col md={3}>
            <Form.Group controlId="product-search">
              <Form.Label>검색</Form.Label>
              <Form.Control
                value={keyword}
                onChange={(e) => onKeywordChange(e.target.value)}
                placeholder="상품명으로 검색"
              />
            </Form.Group>
          </Col>
          <Col md={3}>
            <Form.Group controlId="product-type">
              <Form.Label>상품 타입</Form.Label>
              <Form.Select value={type} onChange={(e) => onTypeChange(e.target.value)}>
                {TYPE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={2}>
            <Form.Group controlId="product-category">
              <Form.Label>카테고리</Form.Label>
              <Form.Select value={category} onChange={(e) => onCategoryChange(e.target.value)}>
                {categoryOptions.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={2}>
            <Form.Group controlId="product-sort">
              <Form.Label>정렬</Form.Label>
              <Form.Select
                value={sort}
                onChange={(e) => onSortChange(e.target.value as ProductSortOrder)}
              >
                {SORT_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={2}>
            <div className="d-grid">
              <Button variant="outline-secondary" onClick={onReset} disabled={!hasActiveFilter}>
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
