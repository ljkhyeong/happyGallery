import { Link } from "react-router";
import { formatKRW, PRODUCT_TYPE_LABEL } from "@/shared/lib";
import type { ProductDetailResponse } from "@/shared/types";
import { ProductMediaPlaceholder } from "./ProductMediaPlaceholder";

interface Props {
  product: ProductDetailResponse;
  /** 목록 제목(h1) 바로 아래에서는 h2, 섹션 안에서는 h3를 쓴다. */
  headingLevel?: 2 | 3;
}

export function ProductCard({ product, headingLevel = 3 }: Props) {
  const Heading = headingLevel === 2 ? "h2" : "h3";
  const meta = [product.category, PRODUCT_TYPE_LABEL[product.type] ?? "상품 종류 확인 필요"]
    .filter(Boolean)
    .join(" · ");

  return (
    <Link
      to={`/products/${product.id}`}
      className={`product-card${product.available ? "" : " is-sold-out"}`}
    >
      {/* 품절 표시는 둥근 사진 밖 아래 가장자리에 걸치도록 사진 틀 바깥에 둔다. */}
      <div className="product-card-visual">
        <div className="product-card-media">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} loading="lazy" />
          ) : (
            <ProductMediaPlaceholder />
          )}
        </div>
        {!product.available && <span className="product-card-status">품절</span>}
      </div>
      <div className="product-card-body">
        <span className="product-card-meta">{meta}</span>
        <Heading className="product-card-name">{product.name}</Heading>
        <span className="product-card-price">{formatKRW(product.price)}</span>
      </div>
    </Link>
  );
}
