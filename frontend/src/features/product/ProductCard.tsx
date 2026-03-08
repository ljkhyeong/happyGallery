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
    <Card as={Link} to={`/products/${product.id}`} className="text-decoration-none h-100">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-start mb-2">
          <Card.Title className="h6 mb-0">{product.name}</Card.Title>
          <Badge bg={product.available ? "success" : "secondary"} className="badge-status">
            {product.available ? "구매 가능" : "품절"}
          </Badge>
        </div>
        <small className="text-muted-soft d-block mb-2">
          {TYPE_LABEL[product.type] ?? product.type}
        </small>
        <span className="fw-semibold">{formatKRW(product.price)}</span>
      </Card.Body>
    </Card>
  );
}
