import { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Form, Modal, Table } from "react-bootstrap";
import {
  fetchSmartStoreInventoryMapping,
  fetchSmartStoreProductPreview,
  applySmartStoreProduct,
  removeSmartStoreMapping,
  retrySmartStoreSync,
  saveSmartStoreMapping,
} from "./api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import type { ProductResponse } from "@/shared/types";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  adminKey: string;
  product: ProductResponse | null;
  onClose: () => void;
  onAuthError: () => void;
}

const STATUS_LABEL = {
  PENDING: "반영 대기",
  PROCESSING: "반영 중",
  SYNCED: "동기화 완료",
  FAILED: "확인 필요",
} as const;

export function SmartStoreInventoryModal({
  adminKey,
  product,
  onClose,
  onAuthError,
}: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [originProductNo, setOriginProductNo] = useState("");
  const [enabled, setEnabled] = useState(true);
  const [optionIds, setOptionIds] = useState<Record<number, string>>({});

  const mappingQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products", product?.id, "smartstore-inventory"],
    queryFn: () => fetchSmartStoreInventoryMapping(adminKey, product!.id),
    enabled: product !== null,
  });
  const mapping = mappingQuery.data;
  const previewQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products", product?.id, "smartstore-product-preview"],
    queryFn: () => fetchSmartStoreProductPreview(adminKey, product!.id),
    enabled: product !== null && mapping?.enabled === true,
  });

  useEffect(() => {
    if (!product) return;
    const mapping = mappingQuery.data;
    setOriginProductNo(mapping?.originProductNo?.toString() ?? "");
    setEnabled(mapping?.enabled ?? true);
    setOptionIds(Object.fromEntries(product.variants.map((variant) => [
      variant.id,
      mapping?.variants.find((item) => item.productVariantId === variant.id)?.optionId.toString() ?? "",
    ])));
  }, [mappingQuery.data, product]);

  const variants = useMemo(() => product?.variants ?? [], [product]);
  const originNumber = Number(originProductNo);
  const validOrigin = Number.isSafeInteger(originNumber) && originNumber > 0;
  const validOptions = product?.type !== "MADE_TO_ORDER" || variants.every((variant) => {
    const value = Number(optionIds[variant.id]);
    return Number.isSafeInteger(value) && value > 0;
  });

  const saveMutation = useAdminMutation(onAuthError, {
    mutationFn: () => saveSmartStoreMapping(adminKey, product!.id, {
      originProductNo: originNumber,
      enabled,
      variants: product!.type === "MADE_TO_ORDER"
        ? variants.map((variant) => ({
          productVariantId: variant.id,
          optionId: Number(optionIds[variant.id]),
        }))
        : [],
    }),
    onSuccess: (mapping) => {
      queryClient.setQueryData(
        ["admin", "products", product?.id, "smartstore-inventory"],
        mapping,
      );
      toast.show(enabled
        ? "스마트스토어 재고 연동을 저장하고 최신 재고 반영을 예약했습니다."
        : "스마트스토어 재고 연동을 비활성화했습니다.");
    },
  });

  const retryMutation = useAdminMutation(onAuthError, {
    mutationFn: () => retrySmartStoreSync(adminKey, product!.id),
    onSuccess: (mapping) => {
      queryClient.setQueryData(
        ["admin", "products", product?.id, "smartstore-inventory"],
        mapping,
      );
      toast.show("스마트스토어 재고를 다시 반영하도록 예약했습니다.");
    },
  });

  const deleteMutation = useAdminMutation(onAuthError, {
    mutationFn: () => removeSmartStoreMapping(adminKey, product!.id),
    onSuccess: () => {
      queryClient.setQueryData(
        ["admin", "products", product?.id, "smartstore-inventory"],
        null,
      );
      toast.show("스마트스토어 재고 연동을 해제했습니다.");
      onClose();
    },
  });

  const applyMutation = useAdminMutation(onAuthError, {
    mutationFn: () => applySmartStoreProduct(
      adminKey,
      product!.id,
      previewQuery.data!.productVersion,
    ),
    onSuccess: async () => {
      toast.show("해피갤러리의 가격·판매 상태·옵션 가격을 스마트스토어에 반영했습니다.");
      await queryClient.invalidateQueries({
        queryKey: ["admin", "products", product?.id, "smartstore-product-preview"],
      });
    },
  });

  const close = () => {
    saveMutation.reset();
    retryMutation.reset();
    deleteMutation.reset();
    applyMutation.reset();
    onClose();
  };

  return (
    <Modal show={product !== null} onHide={close} centered size="lg">
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">{product?.name} 스마트스토어 재고 연동</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {mappingQuery.isLoading && <LoadingSpinner />}
        <ErrorAlert error={mappingQuery.error ?? previewQuery.error ?? saveMutation.error
          ?? retryMutation.error ?? deleteMutation.error ?? applyMutation.error} />

        {mapping && (
          <Alert variant={mapping.syncStatus === "FAILED" ? "warning" : "light"}>
            <div className="d-flex justify-content-between align-items-center gap-2">
              <span>
                현재 상태:{" "}
                <Badge bg={mapping.syncStatus === "SYNCED" ? "success" : mapping.syncStatus === "FAILED" ? "danger" : "secondary"}>
                  {mapping.syncStatus ? STATUS_LABEL[mapping.syncStatus] : "동기화 전"}
                </Badge>
              </span>
              {mapping.syncStatus === "FAILED" && (
                <Button
                  size="sm"
                  variant="outline-danger"
                  disabled={retryMutation.isPending}
                  onClick={() => retryMutation.mutate()}
                >
                  {retryMutation.isPending ? "예약 중..." : "재시도"}
                </Button>
              )}
            </div>
            {mapping.lastError && <div className="small mt-2">{mapping.lastError}</div>}
          </Alert>
        )}

        {mapping?.enabled && previewQuery.isLoading && <LoadingSpinner />}
        {mapping?.enabled && previewQuery.data && (
          <Alert variant={previewQuery.data.different ? "warning" : "success"}>
            <div className="d-flex justify-content-between align-items-start gap-3">
              <div>
                <div className="fw-semibold">가격·판매 상태 비교</div>
                <div className="small mt-1">
                  판매가: 해피갤러리 {previewQuery.data.localSalePrice.toLocaleString()}원
                  {" · "}스마트스토어 {previewQuery.data.channelSalePrice.toLocaleString()}원
                </div>
                <div className="small">
                  판매 상태: 해피갤러리 기준 {previewQuery.data.localStatus}
                  {" · "}스마트스토어 {previewQuery.data.channelStatus}
                </div>
              </div>
              {previewQuery.data.different && (
                <Button size="sm" variant="warning" disabled={applyMutation.isPending}
                  onClick={() => applyMutation.mutate()}>
                  {applyMutation.isPending ? "반영 중..." : "차이 반영"}
                </Button>
              )}
            </div>
            {previewQuery.data.options.some((option) => option.different) && (
              <Table responsive size="sm" className="mt-3 mb-0 align-middle">
                <thead><tr><th>옵션</th><th>해피갤러리 옵션가</th><th>스마트스토어 옵션가</th></tr></thead>
                <tbody>{previewQuery.data.options.filter((option) => option.different).map((option) => (
                  <tr key={option.productVariantId}>
                    <td>{product ? variantLabel(product, option.productVariantId) : option.productVariantId}</td>
                    <td>{option.localPrice.toLocaleString()}원</td>
                    <td>{option.channelPrice === null ? "옵션 없음" : `${option.channelPrice.toLocaleString()}원`}</td>
                  </tr>
                ))}</tbody>
              </Table>
            )}
          </Alert>
        )}

        <Form onSubmit={(event) => {
          event.preventDefault();
          if (validOrigin && validOptions) saveMutation.mutate();
        }}>
          <Form.Group className="mb-3" controlId="smartstore-origin-product-no">
            <Form.Label>스마트스토어 원상품 번호</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={originProductNo}
              onChange={(event) => setOriginProductNo(event.target.value)}
              placeholder="스마트스토어 상품 관리에서 확인한 원상품 번호"
            />
          </Form.Group>

          {product?.type === "MADE_TO_ORDER" && (
            <Table responsive size="sm" className="align-middle">
              <thead>
                <tr>
                  <th>해피갤러리 옵션 조합</th>
                  <th style={{ width: 220 }}>스마트스토어 옵션 ID</th>
                </tr>
              </thead>
              <tbody>
                {variants.map((variant) => (
                  <tr key={variant.id}>
                    <td>{variantLabel(product, variant.id)}</td>
                    <td>
                      <Form.Control
                        type="number"
                        min={1}
                        value={optionIds[variant.id] ?? ""}
                        onChange={(event) => setOptionIds((current) => ({
                          ...current,
                          [variant.id]: event.target.value,
                        }))}
                        aria-label={`${variantLabel(product, variant.id)} 스마트스토어 옵션 ID`}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}

          <Form.Check
            className="mb-3"
            type="switch"
            id="smartstore-inventory-enabled"
            label="재고 변경 시 스마트스토어에 자동 반영"
            checked={enabled}
            onChange={(event) => setEnabled(event.target.checked)}
          />

          <div className="d-flex justify-content-between gap-2">
            <div>
              {mapping && (
                <Button
                  type="button"
                  variant="outline-danger"
                  disabled={deleteMutation.isPending}
                  onClick={() => deleteMutation.mutate()}
                >
                  연동 해제
                </Button>
              )}
            </div>
            <Button
              type="submit"
              disabled={!validOrigin || !validOptions || saveMutation.isPending}
            >
              {saveMutation.isPending ? "저장 중..." : "연동 저장"}
            </Button>
          </div>
        </Form>
      </Modal.Body>
    </Modal>
  );
}

function variantLabel(product: ProductResponse, variantId: number): string {
  const variant = product.variants.find((candidate) => candidate.id === variantId);
  if (!variant || variant.selections.length === 0) return "기본 조합";
  return variant.selections.map((selection) => {
    const group = product.optionGroups.find((candidate) => candidate.key === selection.groupKey);
    const value = group?.values.find((candidate) => candidate.key === selection.valueKey);
    return `${group?.name ?? "옵션"}: ${value?.name ?? "값"}`;
  }).join(" / ");
}
