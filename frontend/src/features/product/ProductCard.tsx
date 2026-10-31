import { Link } from "react-router";
import { formatKRW } from "@/shared/lib";
import type { ProductDetailResponse } from "@/generated/api/product";
import { ProductMediaPlaceholder } from "./ProductMediaPlaceholder";

interface Props {
  product: ProductDetailResponse;
  /** 목록 제목(h1) 바로 아래에서는 h2, 섹션 안에서는 h3를 쓴다. */
  headingLevel?: 2 | 3;
  /** 카테고리로 거른 목록에서는 카드마다 같은 카테고리를 되풀이하지 않는다. */
  showCategory?: boolean;
}

/** 작품 목록·홈의 상품 카드. 여백을 둔 사진 아래에 카테고리, 이름, 가격, 주문 제작·품절 표시를 둔다. */
export function ProductCard({ product, headingLevel = 3, showCategory = true }: Props) {
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
      </div>
      {showCategory && product.category && <span className="product-card-meta">{product.category}</span>}
      <Heading className="product-card-name">{product.name}</Heading>
      <span className="product-card-price">{formatKRW(product.price)}</span>
      {!product.available ? (
        <span className="product-card-status">품절</span>
      ) : product.type === "MADE_TO_ORDER" ? (
        <span className="product-card-type">주문 제작</span>
      ) : null}
    </Link>
  );
}
