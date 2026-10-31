import type { Route } from "./+types/products";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { productListHeading, readProductFilters } from "@/features/product/productFilters";
import { ProductListPage } from "@/pages/ProductListPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  siteTitle,
} from "@/shared/seo/metadata";
import { loadProductCategories, loadProducts } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = siteTitle("수공예 작품");
const DESCRIPTION = "해피갤러리가 직접 만든 수공예 작품과 주문 제작 작품입니다.";

export async function loader({ request }: Route.LoaderArgs) {
  const filters = readProductFilters(new URL(request.url).searchParams);
  const [products, categories] = await Promise.all([
    loadProducts(request.signal, filters),
    loadProductCategories(request.signal),
  ]);
  return { products, categories, filters };
}

export function meta({ loaderData }: Route.MetaArgs) {
  // 카테고리·주문 제작·검색으로 들어오면 제목만 조건에 맞춘다. 대표 주소는 /products 하나다.
  const filters = loaderData?.filters ?? {};
  const { documentName } = productListHeading(filters);
  return buildSeoMeta({
    title: documentName ? siteTitle(documentName) : TITLE,
    description: filters.category
      ? `해피갤러리가 직접 만든 ${filters.category} 작품입니다.`
      : DESCRIPTION,
    pathname: "/products",
    image: heroWorkshop,
    indexable: Boolean(loaderData),
  });
}

export default function ProductsRoute({ loaderData }: Route.ComponentProps) {
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd("/products", TITLE, DESCRIPTION, "CollectionPage"),
        buildBreadcrumbJsonLd([{ name: "공방 작품", pathname: "/products" }]),
      ]} />
      <ProductListPage
        initialProducts={loaderData.products}
        initialCategories={loaderData.categories}
        initialFilters={loaderData.filters}
      />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
