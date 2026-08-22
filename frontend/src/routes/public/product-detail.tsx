import type { Route } from "./+types/product-detail";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { ProductDetailPage } from "@/pages/ProductDetailPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  seoDescription,
} from "@/shared/seo/metadata";
import { buildProductJsonLd } from "@/shared/seo/schemas";
import { loadProduct, requirePublicId } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const FALLBACK_DESCRIPTION = "해피갤러리 수공예 작품의 상세 정보와 주문 방법을 확인하세요.";

export async function loader({ params, request }: Route.LoaderArgs) {
  const productId = requirePublicId(params.id);
  return { product: await loadProduct(productId, request.signal) };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const pathname = `/products/${params.id ?? ""}`;
  const title = loaderData ? `${loaderData.product.name} | 해피갤러리` : "작품을 찾을 수 없습니다 | 해피갤러리";
  const description = loaderData
    ? seoDescription(loaderData.product.description, FALLBACK_DESCRIPTION)
    : FALLBACK_DESCRIPTION;
  return buildSeoMeta({
    title,
    description,
    pathname,
    image: loaderData?.product.imageUrl ?? heroWorkshop,
    type: "product",
    indexable: Boolean(loaderData),
  });
}

export default function ProductDetailRoute({ loaderData }: Route.ComponentProps) {
  const pathname = `/products/${loaderData.product.id}`;
  const title = `${loaderData.product.name} | 해피갤러리`;
  const description = seoDescription(loaderData.product.description, FALLBACK_DESCRIPTION);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, title, description),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "공방 작품", pathname: "/products" },
          { name: loaderData.product.name, pathname },
        ]),
        buildProductJsonLd(loaderData.product, pathname, FALLBACK_DESCRIPTION),
      ]} />
      <ProductDetailPage initialProduct={loaderData.product} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
