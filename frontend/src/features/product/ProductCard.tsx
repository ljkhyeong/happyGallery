import { Card, Badge } from "react-bootstrap";
import { Link } from "react-router-dom";
import { formatKRW } from "@/shared/lib";
import type { ProductDetailResponse } from "@/shared/types";

const TYPE_LABEL: Record<string, string> = {
  READY_STOCK: "기존 재고",
  MADE_TO_ORDER: "예약 제작",
};

interface Props {
  product: ProductDetailResponse;
}

export function ProductCard({ product }: Props) {
  return (
    <Card as={Link} to={`/products/${product.id}`} className="product-card text-decoration-none h-100">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-start mb-3">
          <div>
            <div className="product-card-kicker">{TYPE_LABEL[product.type] ?? product.type}</div>
            <Card.Title className="h6 mb-0">{product.name}</Card.Title>
          </div>
          <Badge bg={product.available ? "success" : "secondary"} className="badge-status">
            {product.available ? "구매 가능" : "품절"}
          </Badge>
        </div>
        <p className="product-card-copy text-muted-soft mb-3">
          {product.type === "MADE_TO_ORDER"
            ? "승인 후 제작이 시작되는 예약 제작 상품"
            : "재고 수량 기준으로 바로 주문 가능한 상품"}
        </p>
        <div className="d-flex justify-content-between align-items-end">
          <span className="fw-semibold">{formatKRW(product.price)}</span>
          <span className="product-card-cta">상세 보기 &rarr;</span>
        </div>
      </Card.Body>
    </Card>
  );
}
