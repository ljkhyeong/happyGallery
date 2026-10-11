import { useMemo } from "react";
import { Container } from "react-bootstrap";
import { Link } from "react-router";
import { fetchProducts, fetchCategories } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { ProductFilterBar } from "@/features/product/ProductFilterBar";
import { PUBLIC_DATA_STALE_TIME, REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { useProductListFilters } from "@/features/product/useProductListFilters";
import { productListHeading } from "@/features/product/productFilters";
import { SHOP_CATEGORIES } from "@/features/product/shopCategories";
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
  const {
    filters: filterParams,
    keyword,
    setKeyword,
    updateFilter,
    categoryHref,
    resetFilters,
  } = useProductListFilters();
  const type = filterParams.type ?? "ALL";
  const category = filterParams.category ?? null;
  const sort = filterParams.sort ?? "newest";
  const heading = productListHeading(filterParams);

  const hasActiveFilter = Object.keys(filterParams).length > 0;
  const productsQueryKey = useMemo(
    () => queryKeys.catalog.productList(filterParams),
    [filterParams],
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

  // 머리글 카테고리 줄과 같은 순서로 두고, 그 밖의 카테고리와 주소로 들어온 카테고리는 뒤에 붙인다.
  const shopCategories: readonly string[] = SHOP_CATEGORIES;
  const availableCategories = categories ?? [];
  const categoryTabs = [
    ...shopCategories.filter((name) => availableCategories.includes(name)),
    ...availableCategories.filter((name) => !shopCategories.includes(name)),
  ];
  if (category && !categoryTabs.includes(category)) categoryTabs.push(category);
  const filtered = Object.keys(filterParams).some((name) => name !== "sort");

  return (
    <Container className="page-container">
      {filtered && (
        <nav className="store-detail-breadcrumb" aria-label="경로">
          <Link to="/products" className="store-detail-breadcrumb-link">전체 작품</Link>
          <span aria-hidden="true">/</span>
          <span className="store-detail-breadcrumb-current" aria-current="page">{heading.title}</span>
        </nav>
      )}
      <header className="store-list-header">
        <h1 className="store-list-title">{heading.title}</h1>
        {heading.lead && <p className="text-muted-soft store-section-desc mb-0">{heading.lead}</p>}
      </header>

      <nav className="store-category-tabs" aria-label="카테고리">
        <Link
          to={categoryHref(null)}
          preventScrollReset
          aria-current={category === null ? "page" : undefined}
        >
          전체
        </Link>
        {categoryTabs.map((name) => (
          <Link
            key={name}
            to={categoryHref(name)}
            preventScrollReset
            aria-current={category === name ? "page" : undefined}
          >
            {name}
          </Link>
        ))}
      </nav>

      <ProductFilterBar
        keyword={keyword}
        onKeywordChange={setKeyword}
        type={type}
        onTypeChange={(value) => updateFilter("type", value)}
        category={category}
        sort={sort}
        onSortChange={(value) => updateFilter("sort", value)}
        resultText={products ? `작품 ${products.length}개` : "작품을 불러오는 중"}
        onReset={resetFilters}
      />

      <ErrorAlert
        error={categoriesError}
        onRetry={() => { void categoriesQuery.refetch(); }}
        retrying={categoriesQuery.isFetching}
      />
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {products && products.length === 0 && (
        <EmptyState message={category && !filterParams.keyword && !filterParams.type
          ? `아직 등록된 ${category} 작품이 없습니다.`
          : "조건에 맞는 상품이 없습니다."} />
      )}
      {products && products.length > 0 && (
        <div className="product-grid">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} headingLevel={2} showCategory={category === null} />
          ))}
        </div>
      )}
    </Container>
  );
}
