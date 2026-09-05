import type { Route } from "./+types/products";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { readProductFilters } from "@/features/product/productFilters";
import { ProductListPage } from "@/pages/ProductListPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
} from "@/shared/seo/metadata";
import { loadProductCategories, loadProducts } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = "수공예 작품 | 해피갤러리";
const DESCRIPTION = "해피갤러리가 직접 만든 수공예 작품과 주문 제작 상품을 둘러보세요.";

export async function loader({ request }: Route.LoaderArgs) {
  const filters = readProductFilters(new URL(request.url).searchParams);
  const [products, categories] = await Promise.all([
    loadProducts(request.signal, filters),
    loadProductCategories(request.signal),
  ]);
  return { products, categories, filters };
}

export function meta({ loaderData }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
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
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "공방 작품", pathname: "/products" },
        ]),
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
