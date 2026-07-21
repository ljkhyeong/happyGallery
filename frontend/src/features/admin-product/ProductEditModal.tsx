import { useEffect, useState } from "react";
import { Button, Form, Modal, Row, Col } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { updateProduct } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import type { ProductResponse } from "@/shared/types";
import { AdminImageField } from "@/features/admin-media/AdminImageField";

interface Props {
  adminKey: string;
  product: ProductResponse | null;
  onClose: () => void;
  onAuthError: () => void;
}

export function ProductEditModal({ adminKey, product, onClose, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState("");
  const [category, setCategory] = useState("");
  const [price, setPrice] = useState("");
  const [description, setDescription] = useState("");
  const [imageUrl, setImageUrl] = useState("");

  useEffect(() => {
    if (!product) return;
    setName(product.name);
    setCategory(product.category ?? "");
    setPrice(String(product.price));
    setDescription(product.description ?? "");
    setImageUrl(product.imageUrl ?? "");
  }, [product]);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateProduct(adminKey, product!.id, {
      name,
      category: category.trim() || undefined,
      price: Number(price),
      description: description.trim() || undefined,
      imageUrl: imageUrl.trim() || undefined,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["product-categories"] });
      toast.show("상품 정보가 수정되었습니다.");
      onClose();
    },
  });

  const valid = name.trim().length > 0 && Number(price) > 0;

  return (
    <Modal show={product != null} onHide={onClose} centered>
      <Form onSubmit={(event) => {
        event.preventDefault();
        if (valid) mutation.mutate();
      }}>
        <Modal.Header closeButton>
          <Modal.Title>상품 정보 수정</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={mutation.error} />
          <Row className="g-3">
            <Col xs={12} md={8}>
              <Form.Group controlId="admin-edit-product-name">
                <Form.Label>상품명</Form.Label>
                <Form.Control value={name} maxLength={100} onChange={(e) => setName(e.target.value)} />
              </Form.Group>
            </Col>
            <Col xs={12} md={4}>
              <Form.Group controlId="admin-edit-product-price">
                <Form.Label>가격</Form.Label>
                <Form.Control type="number" min={1} value={price} onChange={(e) => setPrice(e.target.value)} />
              </Form.Group>
            </Col>
            <Col xs={12}>
              <Form.Group controlId="admin-edit-product-category">
                <Form.Label>카테고리</Form.Label>
                <Form.Control value={category} maxLength={50} onChange={(e) => setCategory(e.target.value)} />
              </Form.Group>
            </Col>
            <Col xs={12}>
              <AdminImageField
                adminKey={adminKey}
                value={imageUrl}
                onChange={setImageUrl}
                onAuthError={onAuthError}
                controlId="admin-edit-product-image"
                previewAlt="수정할 상품 대표 이미지 미리보기"
              />
            </Col>
            <Col xs={12}>
              <Form.Group controlId="admin-edit-product-description">
                <Form.Label>상세 설명</Form.Label>
                <Form.Control as="textarea" rows={5} value={description} maxLength={5000} onChange={(e) => setDescription(e.target.value)} />
              </Form.Group>
            </Col>
          </Row>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={onClose}>취소</Button>
          <Button type="submit" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "저장 중..." : "저장"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
