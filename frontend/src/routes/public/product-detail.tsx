import type { Route } from "./+types/product-detail";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { ProductDetailPage } from "@/pages/ProductDetailPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  seoDescription,
  siteTitle,
} from "@/shared/seo/metadata";
import { buildProductJsonLd } from "@/shared/seo/schemas";
import { loadProduct, requirePublicId } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const FALLBACK_DESCRIPTION = "해피갤러리가 만든 수공예 작품의 상세 정보와 주문 방법입니다.";

export async function loader({ params, request }: Route.LoaderArgs) {
  const productId = requirePublicId(params.id);
  return { product: await loadProduct(productId, request.signal) };
}

/** meta와 화면이 같은 주소·제목·설명을 쓴다. */
function seo({ product }: Route.ComponentProps["loaderData"]) {
  return {
    pathname: `/products/${product.id}`,
    title: siteTitle(product.name),
    description: seoDescription(product.description, FALLBACK_DESCRIPTION),
  };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const { pathname, title, description } = loaderData ? seo(loaderData) : {
    pathname: `/products/${params.id ?? ""}`,
    title: siteTitle("작품을 찾을 수 없습니다"),
    description: FALLBACK_DESCRIPTION,
  };
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
  const { pathname, title, description } = seo(loaderData);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, title, description),
        buildBreadcrumbJsonLd([
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
