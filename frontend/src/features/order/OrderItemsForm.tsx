import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Form, Button, Row, Col, ListGroup, Badge } from "react-bootstrap";
import { fetchProducts } from "@/features/product/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { OrderItemInput, ProductDetailResponse } from "@/shared/types";
import type { ProductType } from "@/shared/types/product";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";
import { OrderOptionList, type OrderOptionDisplay } from "./OrderOptionList";
import { productOptionLineKey } from "@/features/product/optionLineKey";

interface Props {
  items: OrderItemInput[];
  onChange: (items: OrderItemInput[]) => void;
  onItemAmountChange: (amount: number) => void;
  onProductTypesChange: (types: ProductType[]) => void;
}

function getProductTypes(
  items: OrderItemInput[],
  productsById: Map<number, ProductDetailResponse>,
): ProductType[] {
  return Array.from(new Set(
    items
      .map((item) => productsById.get(item.productId)?.type)
      .filter((type): type is ProductType => type !== undefined),
  ));
}

function itemKey(item: OrderItemInput) {
  return productOptionLineKey(item.productId, item.productVariantId, item.textInputs);
}

function unitPrice(item: OrderItemInput, product: ProductDetailResponse) {
  const variantAdjustment = product.variants.find(
    (variant) => variant.id === item.productVariantId,
  )?.priceAdjustment ?? 0;
  const textAdjustment = (item.textInputs ?? []).reduce((sum, input) => {
    if (!input.value?.trim()) return sum;
    return sum + (product.optionGroups.find(
      (group) => group.key === input.groupKey,
    )?.inputPriceAdjustment ?? 0);
  }, 0);
  return product.price + variantAdjustment + textAdjustment;
}

function selectedOptions(item: OrderItemInput, product: ProductDetailResponse): OrderOptionDisplay[] {
  const variant = product.variants.find((candidate) => candidate.id === item.productVariantId);
  return product.optionGroups.flatMap((group) => {
    const selection = variant?.selections.find((candidate) => candidate.groupKey === group.key);
    const value = group.type === "SELECT"
      ? group.values.find((candidate) => candidate.key === selection?.valueKey)?.name
      : item.textInputs?.find((input) => input.groupKey === group.key)?.value?.trim();
    return value ? [{
      groupName: group.name,
      value,
      priceAdjustment: group.type === "TEXT" ? group.inputPriceAdjustment ?? 0 : 0,
    }] : [];
  });
}

export function OrderItemsForm({
  items,
  onChange,
  onItemAmountChange,
  onProductTypesChange,
}: Props) {
  const [selectedId, setSelectedId] = useState("");
  const [qty, setQty] = useState("1");

  const { data: products, isLoading, error } = useQuery({
    queryKey: ["products"],
    queryFn: () => fetchProducts(),
    staleTime: PUBLIC_DATA_STALE_TIME,
  });

  const productMap = useMemo(() => new Map<number, ProductDetailResponse>(
    products?.map((product) => [product.id, product]) ?? [],
  ), [products]);

  const qtyNum = Number(qty);
  const qtyValid = Number.isInteger(qtyNum)
    && qtyNum >= 1
    && qtyNum <= MAX_PRODUCT_QUANTITY;

  const updateItems = (nextItems: OrderItemInput[]) => {
    onChange(nextItems);
    onProductTypesChange(getProductTypes(nextItems, productMap));
  };

  const addItem = () => {
    const pid = Number(selectedId);
    if (pid > 0 && qtyValid) {
      const product = productMap.get(pid);
      if (!product || product.optionGroups.length > 0) return;
      const nextItem: OrderItemInput = {
        productId: pid,
        productVariantId: product.type === "MADE_TO_ORDER"
          ? (product.variants[0]?.id ?? null)
          : null,
        textInputs: [],
        qty: qtyNum,
      };
      const key = itemKey(nextItem);
      const existing = items.find((item) => itemKey(item) === key);
      if (existing) {
        const newQty = Math.min(existing.qty + qtyNum, MAX_PRODUCT_QUANTITY);
        updateItems(items.map((item) => itemKey(item) === key ? { ...item, qty: newQty } : item));
      } else {
        updateItems([...items, nextItem]);
      }
      setQty("1");
    }
  };

  const removeItem = (key: string) => {
    updateItems(items.filter((item) => itemKey(item) !== key));
  };

  const totalAmount = items.reduce((sum, item) => {
    const product = productMap.get(item.productId);
    return sum + (product ? unitPrice(item, product) * item.qty : 0);
  }, 0);
  const selectedProductTypes = useMemo(
    () => getProductTypes(items, productMap),
    [items, productMap],
  );

  useEffect(() => {
    onItemAmountChange(totalAmount);
  }, [onItemAmountChange, totalAmount]);

  useEffect(() => {
    if (!products) return;
    onProductTypesChange(selectedProductTypes);
  }, [onProductTypesChange, products, selectedProductTypes]);

  if (isLoading) return <LoadingSpinner text="상품을 불러오는 중입니다..." />;
  if (error) return <ErrorAlert error={error} />;

  return (
    <div>
      <Row className="g-2 align-items-end mb-3">
        <Col xs={12} sm={6}>
          <Form.Group controlId="order-item-product">
            <Form.Label>상품</Form.Label>
            <Form.Select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
              <option value="">선택하세요</option>
              {products?.filter((p) => p.available && p.optionGroups.length === 0).map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({formatKRW(p.price)})
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col xs={6} sm={3}>
          <Form.Group controlId="order-item-qty">
            <Form.Label>수량</Form.Label>
            <Form.Control
              type="number"
              min={1}
              max={MAX_PRODUCT_QUANTITY}
              value={qty}
              onChange={(e) => setQty(e.target.value)}
              isInvalid={qty !== "" && qty !== "1" && !qtyValid}
              aria-invalid={qty !== "" && qty !== "1" && !qtyValid}
              aria-describedby={
                qty !== "" && qty !== "1" && !qtyValid ? "order-item-qty-error" : undefined
              }
            />
            <Form.Control.Feedback id="order-item-qty-error" type="invalid">
              1~{MAX_PRODUCT_QUANTITY} 사이의 수량을 입력해 주세요.
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
        <Col xs={6} sm={3}>
          <Button variant="outline-primary" className="w-100"
            disabled={!Number(selectedId) || !qtyValid} onClick={addItem}>추가</Button>
        </Col>
      </Row>

      {items.length > 0 && (
        <>
          <ListGroup className="mb-2">
            {items.map((item) => {
              const product = productMap.get(item.productId);
              return (
                <ListGroup.Item key={itemKey(item)}>
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span>
                      {product?.name ?? `상품 #${item.productId}`}
                      <Badge bg="secondary" className="ms-2">x{item.qty}</Badge>
                      {product && (
                        <small className="text-muted-soft ms-2">
                          {formatKRW(unitPrice(item, product) * item.qty)}
                        </small>
                      )}
                    </span>
                    <Button size="sm" variant="outline-danger"
                      onClick={() => removeItem(itemKey(item))}>삭제</Button>
                  </div>
                  {product && <OrderOptionList options={selectedOptions(item, product)} />}
                  {product && (
                    <ProductPurchaseTerms
                      productName={product.name}
                      type={product.type}
                      specification={product.specification}
                      careInstructions={product.careInstructions}
                      productionLeadDays={product.productionLeadDays}
                      compact
                    />
                  )}
                </ListGroup.Item>
              );
            })}
          </ListGroup>
          <div className="text-end fw-bold">
            합계: {formatKRW(totalAmount)}
          </div>
        </>
      )}
    </div>
  );
}
