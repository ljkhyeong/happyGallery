import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Form, Button, Row, Col, ListGroup, Badge } from "react-bootstrap";
import { fetchProducts } from "@/features/product/api";
import { LoadingSpinner, ErrorAlert } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { OrderItemInput, ProductDetailResponse } from "@/shared/types";

interface Props {
  items: OrderItemInput[];
  onChange: (items: OrderItemInput[]) => void;
}

const MAX_QTY = 99;

export function OrderItemsForm({ items, onChange }: Props) {
  const [selectedId, setSelectedId] = useState("");
  const [qty, setQty] = useState("1");

  const { data: products, isLoading, error } = useQuery({
    queryKey: ["products"],
    queryFn: fetchProducts,
  });

  const productMap = new Map<number, ProductDetailResponse>(
    products?.map((p) => [p.id, p]) ?? [],
  );

  const qtyNum = Number(qty);
  const qtyValid = Number.isInteger(qtyNum) && qtyNum >= 1 && qtyNum <= MAX_QTY;

  const addItem = () => {
    const pid = Number(selectedId);
    if (pid > 0 && qtyValid) {
      const existing = items.find((i) => i.productId === pid);
      if (existing) {
        const newQty = Math.min(existing.qty + qtyNum, MAX_QTY);
        onChange(items.map((i) => (i.productId === pid ? { ...i, qty: newQty } : i)));
      } else {
        onChange([...items, { productId: pid, qty: qtyNum }]);
      }
      setQty("1");
    }
  };

  const removeItem = (productId: number) => {
    onChange(items.filter((i) => i.productId !== productId));
  };

  const totalAmount = items.reduce((sum, item) => {
    const product = productMap.get(item.productId);
    return sum + (product ? product.price * item.qty : 0);
  }, 0);

  if (isLoading) return <LoadingSpinner text="상품 로딩..." />;
  if (error) return <ErrorAlert error={error} />;

  return (
    <div>
      <Row className="g-2 align-items-end mb-3">
        <Col xs={12} sm={6}>
          <Form.Group controlId="order-item-product">
            <Form.Label>상품</Form.Label>
            <Form.Select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
              <option value="">선택하세요</option>
              {products?.filter((p) => p.available).map((p) => (
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
              max={MAX_QTY}
              value={qty}
              onChange={(e) => setQty(e.target.value)}
              isInvalid={qty !== "" && qty !== "1" && !qtyValid}
            />
            <Form.Control.Feedback type="invalid">
              1~{MAX_QTY} 사이의 수량을 입력해 주세요.
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
                <ListGroup.Item key={item.productId}
                  className="d-flex justify-content-between align-items-center">
                  <span>
                    {product?.name ?? `상품 #${item.productId}`}
                    <Badge bg="secondary" className="ms-2">x{item.qty}</Badge>
                    {product && (
                      <small className="text-muted-soft ms-2">
                        {formatKRW(product.price * item.qty)}
                      </small>
                    )}
                  </span>
                  <Button size="sm" variant="outline-danger"
                    onClick={() => removeItem(item.productId)}>삭제</Button>
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
