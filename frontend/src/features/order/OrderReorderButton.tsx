import { useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import { getProduct } from "@/generated/api/product";
import type { OrderDetailResponse } from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { useCart } from "@/features/cart/useCart";
import { productSelectionView } from "@/features/product/productSelectionView";
import { productQuantityLimit } from "@/features/product/purchaseStock";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { formatKRW } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import { OrderOptionList } from "./OrderOptionList";

type OrderItem = OrderDetailResponse["items"][number];

export function OrderReorderButton({ item }: { item: OrderItem }) {
  const { sessionVersion } = useCustomerAuth();
  return <ReorderControl key={sessionVersion} item={item} />;
}

function ReorderControl({ item }: { item: OrderItem }) {
  const [open, setOpen] = useState(false);
  return <>
    <Button size="sm" variant="outline-primary" className="mt-2" onClick={() => setOpen(true)}
      aria-label={`${item.productName} 다시 담기`}>다시 담기</Button>
    {open && <ReorderDialog item={item} onClose={() => setOpen(false)} />}
  </>;
}

function selectOptionsKey(options: OrderItem["options"]) {
  return JSON.stringify(options.filter((option) => option.type === "SELECT")
    .map((option) => JSON.stringify([option.groupName, option.value])).sort());
}

function ReorderDialog({ item, onClose }: { item: OrderItem; onClose: () => void }) {
  const [qty, setQty] = useState(String(item.qty));
  const requestRef = useRef<{ fingerprint: string; key: string } | null>(null);
  const cart = useCart();
  const toast = useToast();
  const query = useQuery({
    queryKey: queryKeys.catalog.productDetail(item.productId),
    queryFn: ({ signal }) => getProduct(item.productId, { signal }),
    staleTime: 0,
    retry: false,
  });
  const product = query.data;
  const selection = product ? productSelectionView(product, { productVariantId: item.productVariantId, textInputs: [] }) : null;
  const requiresSelection = product && selection && (
    item.options.some((option) => option.type === "TEXT")
    || (item.productType !== null && item.productType !== product.type)
    || !selection.configurationValid
    || selectOptionsKey(item.options) !== selectOptionsKey(selection.options)
  );
  const existingQty = cart.items.filter((row) => row.productId === item.productId
    && row.productVariantId === selection?.productVariantId).reduce((sum, row) => sum + row.qty, 0);
  const limit = product && selection ? Math.max(0, productQuantityLimit(product, selection.productVariantId) - existingQty) : 0;
  const quantity = Number(qty);
  const validQty = Number.isSafeInteger(quantity) && quantity >= 1 && quantity <= limit;
  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      async () => {
        if (!product || !selection || requiresSelection || !validQty) throw new Error("상품 옵션과 수량을 다시 확인해 주세요.");
        const additions = [{ productId: product.id, productVariantId: selection.productVariantId, textInputs: [], qty: quantity }];
        const fingerprint = JSON.stringify(additions);
        if (requestRef.current?.fingerprint !== fingerprint) requestRef.current = { fingerprint, key: crypto.randomUUID() };
        await cart.addItems(additions, requestRef.current.key);
      },
      () => {
        requestRef.current = null;
        toast.show("장바구니에 담았습니다. 장바구니에서 현재 금액을 확인해 주세요.", "success");
        onClose();
      },
    ),
  });
  return (
    <Modal show onHide={() => { if (!mutation.isPending) onClose(); }} centered>
      <Modal.Header closeButton={!mutation.isPending}><Modal.Title>구매 상품 다시 담기</Modal.Title></Modal.Header>
      <Modal.Body>
        <p className="fw-semibold">{item.productName}</p>
        <p className="small text-muted">이전 주문: {item.qty}개 · 당시 개당 {formatKRW(item.unitPrice)}</p>
        {query.isLoading && <LoadingSpinner text="현재 상품과 재고를 확인하고 있습니다" />}
        <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} />
        <ErrorAlert error={cart.error} onRetry={cart.refetch} />
        {product && selection && <>
          <p>현재 개당 가격: <strong>{formatKRW(selection.unitPrice)}</strong></p>
          <ProductPurchaseTerms productName={product.name} type={product.type} specification={product.specification}
            careInstructions={product.careInstructions} productionLeadDays={product.productionLeadDays} compact showCustomizationInquiry={false} />
          {requiresSelection ? <Alert variant="info" className="mt-3">
            상품 옵션이 바뀌었거나 직접 입력한 문구가 있습니다. 상품 상세에서 옵션과 문구를 다시 선택해 주세요.
            <div className="mt-2"><LinkButton to={`/products/${product.id}`} size="sm" variant="outline-primary">옵션 다시 선택</LinkButton></div>
          </Alert> : <>
            <OrderOptionList options={selection.options} />
            <Form.Group controlId={`reorder-qty-${item.orderItemId}`} className="mt-3">
              <Form.Label>다시 담을 수량</Form.Label>
              <Form.Control type="number" min={1} max={Math.max(1, limit)} step={1} value={qty} disabled={mutation.isPending || limit === 0}
                onChange={(event) => setQty(event.target.value)} />
            </Form.Group>
            {limit === 0 ? <Alert variant="warning" className="mt-3">품절되었거나 현재 장바구니 수량으로 추가 가능한 수량을 모두 담았습니다.</Alert>
              : !validQty ? <p className="text-danger small mt-2">수량을 1~{limit}개로 조정해 주세요. 현재 장바구니에 담긴 수량도 반영합니다.</p>
              : <p className="mt-3">추가할 상품 금액: <strong>{formatKRW(selection.unitPrice * quantity)}</strong></p>}
          </>}
        </>}
        <ErrorAlert error={mutation.error} />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" disabled={mutation.isPending} onClick={onClose}>닫기</Button>
        {product && !requiresSelection && <Button disabled={!validQty || query.isFetching || !!query.error || cart.isLoading || !!cart.error || mutation.isPending}
          onClick={() => mutation.mutate()}>{mutation.isPending ? "담는 중..." : "장바구니에 담기"}</Button>}
      </Modal.Footer>
    </Modal>
  );
}
