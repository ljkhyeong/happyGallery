import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { createProduct } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import type { ProductType } from "@/shared/types";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function CreateProductForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState("");
  const [type, setType] = useState<ProductType>("READY_STOCK");
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("1");

  const mutation = useMutation({
    mutationFn: () =>
      createProduct(adminKey, {
        name,
        type,
        price: Number(price),
        quantity: Number(quantity),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      toast.show("상품이 등록되었습니다.");
      setName("");
      setPrice("");
      setQuantity("1");
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401) {
        onAuthError();
      }
    },
  });

  const valid = name.trim() && Number(price) > 0 && Number(quantity) >= 1;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-2 align-items-end">
        <Col xs={12} md={3}>
          <Form.Group>
            <Form.Label>상품명</Form.Label>
            <Form.Control
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="상품명"
            />
          </Form.Group>
        </Col>
        <Col xs={6} md={2}>
          <Form.Group>
            <Form.Label>유형</Form.Label>
            <Form.Select value={type} onChange={(e) => setType(e.target.value as ProductType)}>
              <option value="READY_STOCK">기존 재고</option>
              <option value="MADE_TO_ORDER">예약 제작</option>
            </Form.Select>
          </Form.Group>
        </Col>
        <Col xs={6} md={2}>
          <Form.Group>
            <Form.Label>가격 (원)</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              placeholder="0"
            />
          </Form.Group>
        </Col>
        <Col xs={6} md={2}>
          <Form.Group>
            <Form.Label>수량</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={6} md={3}>
          <Button type="submit" variant="primary" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "등록 중..." : "상품 등록"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
