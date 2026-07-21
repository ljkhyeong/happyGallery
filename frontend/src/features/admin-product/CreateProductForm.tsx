import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { createProduct } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { ProductType } from "@/shared/types";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { AdminImageField } from "@/features/admin-media/AdminImageField";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function CreateProductForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState("");
  const [type, setType] = useState<ProductType>("READY_STOCK");
  const [category, setCategory] = useState("");
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [description, setDescription] = useState("");
  const [imageUrl, setImageUrl] = useState("");

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () =>
      createProduct(adminKey, {
        name,
        type,
        category: category.trim() || undefined,
        price: Number(price),
        quantity: Number(quantity),
        description: description.trim() || undefined,
        imageUrl: imageUrl.trim() || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["product-categories"] });
      toast.show("상품이 등록되었습니다.");
      setName("");
      setCategory("");
      setPrice("");
      setQuantity("1");
      setDescription("");
      setImageUrl("");
    },
  });

  const valid = name.trim().length > 0 && Number(price) > 0 && Number(quantity) >= 1;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-3 align-items-end">
        <Col xs={12} md={4}>
          <Form.Group controlId="admin-product-name">
            <Form.Label>상품명</Form.Label>
            <Form.Control
              value={name}
              maxLength={100}
              onChange={(e) => setName(e.target.value)}
              placeholder="상품명"
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={2}>
          <Form.Group controlId="admin-product-type">
            <Form.Label>유형</Form.Label>
            <Form.Select value={type} onChange={(e) => setType(e.target.value as ProductType)}>
              <option value="READY_STOCK">기존 재고</option>
              <option value="MADE_TO_ORDER">예약 제작</option>
            </Form.Select>
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={2}>
          <Form.Group controlId="admin-product-category">
            <Form.Label>카테고리</Form.Label>
            <Form.Control
              value={category}
              maxLength={50}
              onChange={(e) => setCategory(e.target.value)}
              placeholder="WOOD"
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={2}>
          <Form.Group controlId="admin-product-price">
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
        <Col xs={12} sm={6} md={2}>
          <Form.Group controlId="admin-product-quantity">
            <Form.Label>수량</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={6}>
          <AdminImageField
            adminKey={adminKey}
            value={imageUrl}
            onChange={setImageUrl}
            onAuthError={onAuthError}
            controlId="admin-product-image"
            previewAlt="등록할 상품 대표 이미지 미리보기"
          />
        </Col>
        <Col xs={12} md={6}>
          <Form.Group controlId="admin-product-description">
            <Form.Label>상세 설명</Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              value={description}
              maxLength={5000}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="소재, 크기, 관리 방법을 입력하세요."
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={{ span: 3, offset: 9 }}>
          <Button type="submit" variant="primary" className="w-100" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "등록 중..." : "상품 등록"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
