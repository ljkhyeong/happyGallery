import { useMemo } from "react";
import { Container } from "react-bootstrap";
import { fetchProducts, fetchCategories } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { ProductFilterBar } from "@/features/product/ProductFilterBar";
import { PUBLIC_DATA_STALE_TIME, REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { useProductListFilters } from "@/features/product/useProductListFilters";
import type { ListProductsParams, ProductDetailResponse } from "@/generated/api/product";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";

interface ProductListPageProps {
  initialProducts: ProductDetailResponse[];
  initialCategories: string[];
  initialFilters: ListProductsParams;
}

export function ProductListPage({
  initialProducts,
  initialCategories,
  initialFilters,
}: ProductListPageProps) {
  const { filters: filterParams, keyword, setKeyword, updateFilter, resetFilters } = useProductListFilters();
  const type = filterParams.type ?? "ALL";
  const category = filterParams.category ?? "ALL";
  const sort = filterParams.sort ?? "newest";

  const hasActiveFilter = Object.keys(filterParams).length > 0;
  const productsQueryKey = useMemo(
    () => hasActiveFilter
      ? [queryKeys.catalog.products[0], filterParams] as const
      : queryKeys.catalog.products,
    [filterParams, hasActiveFilter],
  );

  const {
    data: products,
    error,
    isLoading,
  } = useLoaderBackedQuery({
    queryKey: productsQueryKey,
    queryFn: () => fetchProducts(hasActiveFilter ? filterParams : undefined),
    staleTime: PUBLIC_DATA_STALE_TIME,
  }, JSON.stringify(filterParams) === JSON.stringify(initialFilters) ? initialProducts : undefined);

  const {
    data: categories,
    error: categoriesError,
    query: categoriesQuery,
  } = useLoaderBackedQuery({
    queryKey: queryKeys.catalog.productCategories,
    queryFn: fetchCategories,
    staleTime: REFERENCE_DATA_STALE_TIME,
  }, initialCategories);

  return (
    <Container className="page-container">
      <section className="store-list-header mb-4 anim-fade-up">
        <p className="store-section-kicker mb-2">해피갤러리 작품</p>
        <div className="d-flex flex-column flex-md-row justify-content-between gap-3 align-items-md-end">
          <div>
            <h1 className="store-list-title mb-1">공방 작품</h1>
            <p className="text-muted-soft store-section-desc mb-0">
              바로 구매할 수 있는 작품과 주문 후 제작하는 작품을 함께 소개합니다.
            </p>
          </div>
        </div>
      </section>

      <div className="anim-fade-up anim-delay-1">
        <ProductFilterBar
          keyword={keyword}
          onKeywordChange={setKeyword}
          type={type}
          onTypeChange={(value) => updateFilter("type", value)}
          category={category}
          onCategoryChange={(value) => updateFilter("category", value)}
          categories={categories ?? []}
          sort={sort}
          onSortChange={(value) => updateFilter("sort", value)}
          resultText={products ? `작품 ${products.length}개` : "작품을 불러오는 중"}
          onReset={resetFilters}
        />
      </div>

      <ErrorAlert
        error={categoriesError}
        onRetry={() => { void categoriesQuery.refetch(); }}
        retrying={categoriesQuery.isFetching}
      />
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {products && products.length === 0 && <EmptyState message="조건에 맞는 상품이 없습니다." />}
      {products && products.length > 0 && (
        <div className="product-grid anim-fade-up anim-delay-2">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} headingLevel={2} />
          ))}
        </div>
      )}
    </Container>
  );
}
