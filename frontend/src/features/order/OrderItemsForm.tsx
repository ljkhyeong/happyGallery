import { useState } from "react";
import { Alert, Form, Button, Row, Col, ListGroup } from "react-bootstrap";
import { LoadingSpinner, ErrorAlert } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import { formatKRW } from "@/shared/lib";
import type { OrderItemInput } from "@/shared/types";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";
import { OrderOptionList } from "./OrderOptionList";
import { productOptionLineKey } from "@/features/product/optionLineKey";
import { productSelectionView } from "@/features/product/productSelectionView";
import { productQuantityLimit, productSkuKey } from "@/features/product/purchaseStock";
import type { useOrderItems } from "./useOrderItems";

interface Props {
  state: ReturnType<typeof useOrderItems>;
  onChange: (items: OrderItemInput[]) => void;
}

export function OrderItemsForm({ state, onChange }: Props) {
  const [selectedId, setSelectedId] = useState("");
  const [qty, setQty] = useState("1");
  const { query, productMap, items, lines, quantities, itemAmount } = state;
  const selectedProduct = productMap.get(Number(selectedId));
  const selected = selectedProduct ? productSelectionView(selectedProduct, {}) : null;
  const nextItem = {
    productId: Number(selectedId), productVariantId: selected?.productVariantId ?? null,
    textInputs: [], qty: Number(qty),
  };
  const remaining = selectedProduct
    ? Math.max(0, productQuantityLimit(selectedProduct, nextItem.productVariantId)
      - (quantities.get(productSkuKey(nextItem)) ?? 0))
    : 0;
  const canAdd = Boolean(selectedProduct?.available && selected?.configurationValid)
    && selectedProduct?.optionGroups.length === 0
    && Number.isInteger(nextItem.qty) && nextItem.qty >= 1 && nextItem.qty <= remaining;

  const addItem = () => {
    if (!canAdd) return;
    const key = productOptionLineKey(nextItem.productId, nextItem.productVariantId, nextItem.textInputs);
    const existing = lines.find((line) => line.key === key);
    onChange(existing
      ? lines.map((line) => line.key === key ? { ...line.item, qty: line.item.qty + nextItem.qty } : line.item)
      : [...items, nextItem]);
    setQty("1");
  };

  const updateQty = (key: string, quantity: number) => {
    const line = lines.find((candidate) => candidate.key === key);
    if (!line || !Number.isInteger(quantity) || quantity < 1 || quantity > MAX_PRODUCT_QUANTITY) return;
    if (quantity > line.item.qty && quantity > line.maxQuantity) return;
    onChange(lines.map((candidate) => candidate.key === key ? { ...candidate.item, qty: quantity } : candidate.item));
  };

  if (query.isPending) return <LoadingSpinner text="상품을 불러오는 중입니다..." />;

  return (
    <div>
      <ErrorAlert error={query.error} onRetry={() => void query.refetch()} retrying={query.isFetching} />
      <Row className="g-2 align-items-end mb-3">
        <Col xs={12} sm={6}>
          <Form.Group controlId="order-item-product">
            <Form.Label>상품</Form.Label>
            <Form.Select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
              <option value="">선택하세요</option>
              {query.data?.filter((product) => product.available && product.optionGroups.length === 0).map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} ({formatKRW(productSelectionView(product, {}).unitPrice)})
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col xs={6} sm={3}>
          <Form.Group controlId="order-item-qty">
            <Form.Label>수량</Form.Label>
            <Form.Control
              type="number" min={1} max={Math.max(1, remaining)} value={qty}
              onChange={(e) => setQty(e.target.value)}
              isInvalid={Boolean(selectedProduct) && !canAdd}
              aria-invalid={Boolean(selectedProduct) && !canAdd}
              aria-describedby={selectedProduct ? "order-item-qty-help" : undefined}
            />
          </Form.Group>
        </Col>
        <Col xs={6} sm={3}>
          <Button variant="outline-primary" className="w-100"
            disabled={!canAdd || query.isError} onClick={addItem}>추가</Button>
        </Col>
        {selectedProduct && <Form.Text id="order-item-qty-help">추가 가능 수량: {remaining}개</Form.Text>}
      </Row>

      {lines.length > 0 && (
        <>
          <ListGroup className="mb-2">
            {lines.map(({ key, item, product, selection, problem, maxQuantity }) => (
              <ListGroup.Item key={key}>
                <div className="d-flex justify-content-between align-items-center mb-2 gap-2">
                  <span>
                    {product?.name ?? `상품 #${item.productId}`}
                    {selection && !problem && (
                      <small className="text-muted-soft ms-2">{formatKRW(selection.unitPrice * item.qty)}</small>
                    )}
                  </span>
                  <div className="d-flex align-items-center gap-2">
                    <Form.Control type="number" size="sm" style={{ width: 80 }}
                      min={1} max={Math.max(1, maxQuantity)} value={item.qty}
                      aria-label={`${product?.name ?? "상품"} ${selection?.label ?? ""} 주문 수량`}
                      onChange={(event) => updateQty(key, Number(event.target.value))} />
                    <Button size="sm" variant="outline-danger"
                      onClick={() => onChange(lines.filter((line) => line.key !== key).map((line) => line.item))}>삭제</Button>
                  </div>
                </div>
                {problem && <Alert variant="warning" className="py-2">
                  {problem}
                  {product?.available && !selection?.configurationValid && (
                    <LinkButton to={`/products/${item.productId}`} variant="link" size="sm">옵션 다시 선택</LinkButton>
                  )}
                </Alert>}
                {selection && <OrderOptionList options={selection.options} />}
                {product && (
                  <ProductPurchaseTerms productName={product.name} type={product.type}
                    specification={product.specification} careInstructions={product.careInstructions}
                    productionLeadDays={product.productionLeadDays} compact />
                )}
              </ListGroup.Item>
            ))}
          </ListGroup>
          <div className="text-end fw-bold">합계: {formatKRW(itemAmount)}</div>
        </>
      )}
    </div>
  );
}
