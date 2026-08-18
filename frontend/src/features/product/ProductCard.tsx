import { Card, Badge } from "react-bootstrap";
import { Link } from "react-router";
import { formatKRW, PRODUCT_TYPE_LABEL } from "@/shared/lib";
import type { ProductDetailResponse } from "@/shared/types";

interface Props {
  product: ProductDetailResponse;
}

export function ProductCard({ product }: Props) {
  return (
    <Card as={Link} to={`/products/${product.id}`} className="product-card text-decoration-none h-100">
      {product.imageUrl && (
        <div className="product-card-media">
          <img src={product.imageUrl} alt={product.name} loading="lazy" />
        </div>
      )}
      <Card.Body className="d-flex flex-column p-4">
        <div className="d-flex justify-content-between align-items-start mb-3">
          <div className="product-card-kicker">
            {PRODUCT_TYPE_LABEL[product.type] ?? "상품 종류 확인 필요"}
            {product.category && (
              <span className="ms-2 text-muted-soft" style={{ fontWeight: 400 }}>
                {product.category}
              </span>
            )}
          </div>
          <Badge bg={product.available ? "dark" : "secondary"} className="badge-status">
            {product.available ? "구매 가능" : "품절"}
          </Badge>
        </div>
        <Card.Title className="product-card-name mb-2">
          {product.name}
        </Card.Title>
        <p className="product-card-copy text-muted-soft mb-3 flex-grow-1">
          {product.description || (product.type === "MADE_TO_ORDER"
            ? "승인 후 제작이 시작되는 예약 제작 상품"
            : "재고 수량 기준으로 바로 주문 가능한 상품")}
        </p>
        <div className="d-flex justify-content-between align-items-end pt-2 product-card-divider">
          <span className="product-card-price">
            {formatKRW(product.price)}
          </span>
          <span className="product-card-cta">자세히 보기 →</span>
        </div>
      </Card.Body>
    </Card>
  );
}
