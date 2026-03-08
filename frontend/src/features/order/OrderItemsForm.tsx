import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Form, Button, Row, Col, ListGroup, Badge } from "react-bootstrap";
import { fetchProducts } from "@/features/product/api";
import { LoadingSpinner } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { OrderItemInput, ProductDetailResponse } from "@/shared/types";

interface Props {
  items: OrderItemInput[];
  onChange: (items: OrderItemInput[]) => void;
}

export function OrderItemsForm({ items, onChange }: Props) {
  const [selectedId, setSelectedId] = useState("");
  const [qty, setQty] = useState("1");

  const { data: products, isLoading } = useQuery({
    queryKey: ["products"],
    queryFn: fetchProducts,
  });

  const productMap = new Map<number, ProductDetailResponse>(
    products?.map((p) => [p.id, p]) ?? [],
  );

  const addItem = () => {
    const pid = Number(selectedId);
    if (pid > 0 && Number(qty) > 0) {
      const existing = items.find((i) => i.productId === pid);
      if (existing) {
        onChange(items.map((i) => (i.productId === pid ? { ...i, qty: i.qty + Number(qty) } : i)));
      } else {
        onChange([...items, { productId: pid, qty: Number(qty) }]);
      }
      setQty("1");
    }
  };

  const removeItem = (productId: number) => {
    onChange(items.filter((i) => i.productId !== productId));
  };

  if (isLoading) return <LoadingSpinner text="상품 로딩..." />;

  return (
    <div>
      <Row className="g-2 align-items-end mb-3">
        <Col xs={12} sm={6}>
          <Form.Group>
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
          <Form.Group>
            <Form.Label>수량</Form.Label>
            <Form.Control type="number" min={1} value={qty}
              onChange={(e) => setQty(e.target.value)} />
          </Form.Group>
        </Col>
        <Col xs={6} sm={3}>
          <Button variant="outline-primary" className="w-100"
            disabled={!Number(selectedId)} onClick={addItem}>추가</Button>
        </Col>
      </Row>

      {items.length > 0 && (
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
      )}
    </div>
  );
}
