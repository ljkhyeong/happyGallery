import { useEffect, useState } from "react";
import { Button, Form, Modal, Row, Col } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { updateProduct } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import type { ProductResponse } from "@/shared/types";
import { AdminImageField } from "@/features/admin-media/AdminImageField";
import {
  optionDraftsFromProduct,
  ProductOptionEditor,
  type OptionGroupDraft,
  type VariantDraft,
} from "./ProductOptionEditor";

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
  const [specification, setSpecification] = useState("");
  const [careInstructions, setCareInstructions] = useState("");
  const [productionLeadDays, setProductionLeadDays] = useState("");
  const [quantity, setQuantity] = useState("0");
  const [optionGroups, setOptionGroups] = useState<OptionGroupDraft[]>([]);
  const [variants, setVariants] = useState<VariantDraft[]>([]);

  useEffect(() => {
    if (!product) return;
    setName(product.name);
    setCategory(product.category ?? "");
    setPrice(String(product.price));
    setDescription(product.description ?? "");
    setImageUrl(product.imageUrl ?? "");
    setSpecification(product.specification ?? "");
    setCareInstructions(product.careInstructions ?? "");
    setProductionLeadDays(product.productionLeadDays?.toString() ?? "");
    setQuantity(String(product.quantity));
    const drafts = optionDraftsFromProduct(product);
    setOptionGroups(drafts.groups);
    setVariants(drafts.variants);
  }, [product]);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateProduct(adminKey, product!.id, {
      name,
      category: category.trim() || undefined,
      price: Number(price),
      description: description.trim() || undefined,
      imageUrl: imageUrl.trim() || undefined,
      specification: specification.trim() || undefined,
      careInstructions: careInstructions.trim() || undefined,
      productionLeadDays: product!.type === "MADE_TO_ORDER"
        ? Number(productionLeadDays)
        : undefined,
      quantity: product!.type === "MADE_TO_ORDER"
        && optionGroups.every((group) => group.type !== "SELECT")
        ? Number(quantity)
        : undefined,
      optionGroups: product!.type === "MADE_TO_ORDER" ? optionGroups : [],
      variants: product!.type === "MADE_TO_ORDER" ? variants : [],
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["product-categories"] });
      toast.show("상품 정보가 수정되었습니다.");
      onClose();
    },
  });

  const leadDays = Number(productionLeadDays);
  const purchaseTermsValid = product?.type !== "MADE_TO_ORDER"
    || (specification.trim().length > 0
      && Number.isInteger(leadDays)
      && leadDays >= 1
      && leadDays <= 180);
  const hasSelectOptions = optionGroups.some((group) => group.type === "SELECT");
  const optionsValid = product?.type !== "MADE_TO_ORDER" || (
    optionGroups.every((group) => group.name.trim().length > 0
      && (group.type !== "SELECT"
        || group.values.every((value) => value.name.trim().length > 0)))
    && variants.length <= 500
    && variants.every((variant) => Number(variant.quantity) >= 0
      && Number(price) + Number(variant.priceAdjustment ?? 0) > 0)
  );
  const valid = name.trim().length > 0
    && Number(price) > 0
    && Number(quantity) >= 0
    && purchaseTermsValid
    && optionsValid;

  return (
    <Modal
      show={product != null}
      aria-labelledby="admin-product-edit-title"
      onHide={onClose}
      centered
      size="xl"
    >
      <Form onSubmit={(event) => {
        event.preventDefault();
        if (valid) mutation.mutate();
      }}>
        <Modal.Header closeButton>
          <Modal.Title id="admin-product-edit-title">상품 정보 수정</Modal.Title>
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
              <Form.Group controlId="admin-edit-product-specification">
                <Form.Label>
                  상품 사양 {product?.type === "MADE_TO_ORDER" && <span className="text-danger">*</span>}
                </Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  value={specification}
                  maxLength={2000}
                  onChange={(e) => setSpecification(e.target.value)}
                />
              </Form.Group>
            </Col>
            {product?.type === "MADE_TO_ORDER" && (
              <>
                <Col xs={12} md={6}>
                  <Form.Group controlId="admin-edit-product-production-lead-days">
                    <Form.Label>제작 기간 (일) <span className="text-danger">*</span></Form.Label>
                    <Form.Control
                      type="number"
                      min={1}
                      max={180}
                      value={productionLeadDays}
                      onChange={(e) => setProductionLeadDays(e.target.value)}
                    />
                  </Form.Group>
                </Col>
                <Col xs={12} md={6}>
                  <Form.Group controlId="admin-edit-product-default-quantity">
                    <Form.Label>기본 조합 재고</Form.Label>
                    <Form.Control
                      type="number"
                      min={0}
                      value={quantity}
                      disabled={hasSelectOptions}
                      onChange={(event) => setQuantity(event.target.value)}
                    />
                  </Form.Group>
                </Col>
                <Col xs={12}>
                  <ProductOptionEditor
                    groups={optionGroups}
                    variants={variants}
                    onChange={(nextGroups, nextVariants) => {
                      setOptionGroups(nextGroups);
                      setVariants(nextVariants);
                    }}
                  />
                </Col>
              </>
            )}
            <Col xs={12}>
              <Form.Group controlId="admin-edit-product-care-instructions">
                <Form.Label>관리 방법</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={2}
                  value={careInstructions}
                  maxLength={2000}
                  onChange={(e) => setCareInstructions(e.target.value)}
                />
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
