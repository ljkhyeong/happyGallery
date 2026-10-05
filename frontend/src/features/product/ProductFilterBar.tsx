import { Search } from "lucide-react";
import { Button, Form } from "react-bootstrap";
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

  // 선택 상자는 값("전체 타입"·"최신순" 등)이 스스로 설명하므로 라벨은 보조기기용으로만 둔다.
  return (
    <div className="store-filter-bar">
      <div className="store-filter-controls">
        <Form.Group controlId="product-search" className="store-filter-search">
          <Form.Label className="visually-hidden">검색</Form.Label>
          <Search className="store-filter-search-icon" size={16} aria-hidden="true" />
          <Form.Control
            type="search"
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            placeholder="작품 이름으로 검색"
          />
        </Form.Group>
        <Form.Group controlId="product-type" className="store-filter-select">
          <Form.Label className="visually-hidden">상품 타입</Form.Label>
          <Form.Select value={type} onChange={(e) => onTypeChange(e.target.value)}>
            {TYPE_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </Form.Select>
        </Form.Group>
        <Form.Group controlId="product-category" className="store-filter-select">
          <Form.Label className="visually-hidden">카테고리</Form.Label>
          <Form.Select value={category} onChange={(e) => onCategoryChange(e.target.value)}>
            {categoryOptions.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </Form.Select>
        </Form.Group>
        <Form.Group controlId="product-sort" className="store-filter-select">
          <Form.Label className="visually-hidden">정렬</Form.Label>
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
      </div>
      <div className="store-filter-meta">
        <span className="my-filter-result">{resultText}</span>
        <Button variant="link" size="sm" onClick={onReset} disabled={!hasActiveFilter}>
          초기화
        </Button>
      </div>
    </div>
  );
}
