import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Container, Row, Col } from "react-bootstrap";
import { fetchProducts, fetchCategories } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { ProductFilterBar } from "@/features/product/ProductFilterBar";
import { PUBLIC_DATA_STALE_TIME, REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { useDebouncedValue } from "@/shared/hooks/useDebouncedValue";
import type { ProductFilterParams, ProductSortOrder, ProductType } from "@/shared/types";

export function ProductListPage() {
  const [keyword, setKeyword] = useState("");
  const [type, setType] = useState("ALL");
  const [category, setCategory] = useState("ALL");
  const [sort, setSort] = useState<ProductSortOrder>("newest");

  const debouncedKeyword = useDebouncedValue(keyword, 300);

  const filterParams: ProductFilterParams = {
    ...(type !== "ALL" && { type: type as ProductType }),
    ...(category !== "ALL" && { category }),
    ...(debouncedKeyword.trim() && { keyword: debouncedKeyword.trim() }),
    ...(sort !== "newest" && { sort }),
  };

  const hasActiveFilter = Object.keys(filterParams).length > 0;

  const {
    data: products,
    isLoading,
    error,
  } = useQuery({
    queryKey: hasActiveFilter ? ["products", filterParams] : ["products"],
    queryFn: () => fetchProducts(hasActiveFilter ? filterParams : undefined),
    staleTime: PUBLIC_DATA_STALE_TIME,
  });

  const { data: categories = [] } = useQuery({
    queryKey: ["product-categories"],
    queryFn: fetchCategories,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

  function handleReset() {
    setKeyword("");
    setType("ALL");
    setCategory("ALL");
    setSort("newest");
  }

  return (
    <Container className="page-container">
      <section className="store-list-header mb-4 anim-fade-up">
        <p className="store-section-kicker mb-2">Store</p>
        <div className="d-flex flex-column flex-md-row justify-content-between gap-3 align-items-md-end">
          <div>
            <h3 className="store-list-title mb-1">ALL PRODUCTS</h3>
            <p className="text-muted-soft store-section-desc mb-0">
              바로 판매 가능한 상품과 예약 제작 상품을 한 곳에서 확인하세요.
            </p>
          </div>
        </div>
      </section>

      <div className="anim-fade-up anim-delay-1">
        <ProductFilterBar
          keyword={keyword}
          onKeywordChange={setKeyword}
          type={type}
          onTypeChange={setType}
          category={category}
          onCategoryChange={setCategory}
          categories={categories}
          sort={sort}
          onSortChange={setSort}
          resultText={products ? `${products.length}개의 상품` : "상품을 불러오는 중"}
          onReset={handleReset}
        />
      </div>

      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {products && products.length === 0 && <EmptyState message="조건에 맞는 상품이 없습니다." />}
      {products && products.length > 0 && (
        <Row xs={1} sm={2} md={3} className="g-4 anim-fade-up anim-delay-2">
          {products.map((p) => (
            <Col key={p.id}>
              <ProductCard product={p} />
            </Col>
          ))}
        </Row>
      )}
    </Container>
  );
}
