import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { createProduct } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { ProductType } from "@/shared/types";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { AdminImageField } from "@/features/admin-media/AdminImageField";
import {
  ProductOptionEditor,
  type OptionGroupDraft,
  type VariantDraft,
} from "./ProductOptionEditor";

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
  const [specification, setSpecification] = useState("");
  const [careInstructions, setCareInstructions] = useState("");
  const [productionLeadDays, setProductionLeadDays] = useState("");
  const [optionGroups, setOptionGroups] = useState<OptionGroupDraft[]>([]);
  const [variants, setVariants] = useState<VariantDraft[]>([]);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () =>
      createProduct(adminKey, {
        name,
        type,
        category: category.trim() || undefined,
        price: Number(price),
        quantity: type === "READY_STOCK"
          || optionGroups.every((group) => group.type !== "SELECT")
          ? Number(quantity)
          : undefined,
        description: description.trim() || undefined,
        imageUrl: imageUrl.trim() || undefined,
        specification: specification.trim() || undefined,
        careInstructions: careInstructions.trim() || undefined,
        productionLeadDays: type === "MADE_TO_ORDER"
          ? Number(productionLeadDays)
          : undefined,
        optionGroups: type === "MADE_TO_ORDER" ? optionGroups : [],
        variants: type === "MADE_TO_ORDER" ? variants : [],
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
      setSpecification("");
      setCareInstructions("");
      setProductionLeadDays("");
      setOptionGroups([]);
      setVariants([]);
    },
  });

  const leadDays = Number(productionLeadDays);
  const purchaseTermsValid = type === "READY_STOCK"
    || (specification.trim().length > 0
      && Number.isInteger(leadDays)
      && leadDays >= 1
      && leadDays <= 180);
  const hasSelectOptions = optionGroups.some((group) => group.type === "SELECT");
  const optionsValid = type === "READY_STOCK" || (
    optionGroups.every((group) => group.name.trim().length > 0
      && (group.type !== "SELECT"
        || group.values.every((value) => value.name.trim().length > 0)))
    && variants.length <= 500
    && variants.every((variant) => Number(variant.quantity) >= 0
      && Number(price) + Number(variant.priceAdjustment ?? 0) > 0)
  );
  const valid = name.trim().length > 0
    && Number(price) > 0
    && (type === "READY_STOCK" ? Number(quantity) >= 1 : Number(quantity) >= 0)
    && purchaseTermsValid
    && optionsValid;

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
            <Form.Select
              value={type}
              onChange={(e) => {
                const nextType = e.target.value as ProductType;
                setType(nextType);
                if (nextType === "READY_STOCK") {
                  setProductionLeadDays("");
                  setOptionGroups([]);
                  setVariants([]);
                }
              }}
            >
              <option value="READY_STOCK">기존 재고</option>
              <option value="MADE_TO_ORDER">주문제작</option>
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
              placeholder="예: 목공 소품"
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
            <Form.Label>{type === "MADE_TO_ORDER" ? "기본 조합 재고" : "수량"}</Form.Label>
            <Form.Control
              type="number"
              min={type === "READY_STOCK" ? 1 : 0}
              value={quantity}
              disabled={type === "MADE_TO_ORDER" && hasSelectOptions}
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
        {type === "MADE_TO_ORDER" && (
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
        )}
        <Col xs={12} md={8}>
          <Form.Group controlId="admin-product-specification">
            <Form.Label>
              상품 사양 {type === "MADE_TO_ORDER" && <span className="text-danger">*</span>}
            </Form.Label>
            <Form.Control
              as="textarea"
              rows={3}
              value={specification}
              maxLength={2000}
              onChange={(e) => setSpecification(e.target.value)}
              placeholder="재료, 크기, 색상 등 온라인 주문에 적용할 고정 사양"
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={4}>
          <Form.Group controlId="admin-product-production-lead-days">
            <Form.Label>
              제작 기간 (일) {type === "MADE_TO_ORDER" && <span className="text-danger">*</span>}
            </Form.Label>
            <Form.Control
              type="number"
              min={1}
              max={180}
              value={productionLeadDays}
              disabled={type !== "MADE_TO_ORDER"}
              onChange={(e) => setProductionLeadDays(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12}>
          <Form.Group controlId="admin-product-care-instructions">
            <Form.Label>관리 방법</Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              value={careInstructions}
              maxLength={2000}
              onChange={(e) => setCareInstructions(e.target.value)}
              placeholder="보관, 세척, 사용 시 주의사항"
            />
          </Form.Group>
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
              placeholder="상품의 특징과 소개를 입력하세요."
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
