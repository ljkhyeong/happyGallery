import { Link } from "react-router";
import { formatKRW } from "@/shared/lib";
import type { ProductDetailResponse } from "@/shared/types";
import { ProductMediaPlaceholder } from "./ProductMediaPlaceholder";

interface Props {
  product: ProductDetailResponse;
  /** 목록 제목(h1) 바로 아래에서는 h2, 섹션 안에서는 h3를 쓴다. */
  headingLevel?: 2 | 3;
}

/** 작품 목록·홈의 상품 카드. 정사각 사진 아래에 카테고리, 이름, 가격, 주문 제작 표시를 둔다. */
export function ProductCard({ product, headingLevel = 3 }: Props) {
  const Heading = headingLevel === 2 ? "h2" : "h3";

  return (
    <Link
      to={`/products/${product.id}`}
      className={`product-card${product.available ? "" : " is-sold-out"}`}
    >
      <div className="product-card-media">
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} loading="lazy" />
        ) : (
          <ProductMediaPlaceholder />
        )}
        {!product.available && <span className="product-card-status">품절</span>}
      </div>
      {product.category && <span className="product-card-meta">{product.category}</span>}
      <Heading className="product-card-name">{product.name}</Heading>
      <span className="product-card-price">{formatKRW(product.price)}</span>
      {product.type === "MADE_TO_ORDER" && (
        <span className="product-card-badges">
          <span className="product-card-badge">주문 제작</span>
        </span>
      )}
    </Link>
  );
}
