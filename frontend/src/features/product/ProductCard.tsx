import { Link } from "react-router";
import { formatKRW, PRODUCT_TYPE_LABEL } from "@/shared/lib";
import type { ProductDetailResponse } from "@/shared/types";
import { ProductMediaPlaceholder } from "./ProductMediaPlaceholder";

interface Props {
  product: ProductDetailResponse;
  /** 목록 제목(h1) 바로 아래에서는 h2, 섹션 안에서는 h3를 쓴다. */
  headingLevel?: 2 | 3;
  /** 홈 전시 목록처럼 순서가 의미 있을 때 붙이는 작품 번호(1부터). */
  exhibitNumber?: number;
}

/** 작품 목록·홈의 상품 카드. 액자처럼 여백을 둔 사진 아래에 번호·카테고리, 이름, 판매 방식, 가격을 둔다. */
export function ProductCard({ product, headingLevel = 3, exhibitNumber }: Props) {
  const Heading = headingLevel === 2 ? "h2" : "h3";
  const label = [
    exhibitNumber === undefined ? null : `No. ${String(exhibitNumber).padStart(2, "0")}`,
    product.category,
  ].filter(Boolean).join(" — ");

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
      {label && <span className="product-card-meta">{label}</span>}
      <Heading className="product-card-name">{product.name}</Heading>
      {product.available ? (
        <span className="product-card-type">{PRODUCT_TYPE_LABEL[product.type] ?? ""}</span>
      ) : (
        <span className="product-card-status">품절</span>
      )}
      <span className="product-card-price">{formatKRW(product.price)}</span>
    </Link>
  );
}
