import { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Form, Modal, Table } from "react-bootstrap";
import {
  fetchSmartStoreInventoryMapping,
  fetchSmartStoreProduct,
  fetchSmartStoreProducts,
  fetchSmartStoreProductPreview,
  applySmartStoreProduct,
  removeSmartStoreMapping,
  retrySmartStoreSync,
  saveSmartStoreMapping,
} from "./api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { ApiError } from "@/shared/api";
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
  const [catalogPage, setCatalogPage] = useState(1);
  const [catalogSearch, setCatalogSearch] = useState("");
  const [originChangeConfirmed, setOriginChangeConfirmed] = useState(false);
  const [unlinkRequested, setUnlinkRequested] = useState(false);
  const [unlinkConfirmed, setUnlinkConfirmed] = useState(false);
  const variants = useMemo(() => product?.variants ?? [], [product]);
  const originNumber = Number(originProductNo);
  const validOrigin = Number.isSafeInteger(originNumber) && originNumber > 0;
  const validOptions = product?.type !== "MADE_TO_ORDER" || variants.every((variant) => {
    const value = Number(optionIds[variant.id]);
    return Number.isSafeInteger(value) && value > 0;
  });
  const mappingQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products", product?.id, "smartstore-inventory"],
    queryFn: () => fetchSmartStoreInventoryMapping(adminKey, product!.id),
    enabled: product !== null,
  });
  const mapping = mappingQuery.data;
  const previousOriginProductNo = mapping?.originProductNo;
  const originChanged = previousOriginProductNo !== undefined
    && validOrigin
    && previousOriginProductNo !== originNumber;
  const canSave = mappingQuery.isSuccess
    && !unlinkRequested
    && validOrigin
    && validOptions
    && (!originChanged || originChangeConfirmed);
  const catalogQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-products", catalogPage],
    queryFn: () => fetchSmartStoreProducts(adminKey, catalogPage),
    enabled: product !== null,
  });
  const catalogProducts = useMemo(() => {
    const keyword = catalogSearch.trim().toLowerCase();
    if (!keyword) return catalogQuery.data?.products ?? [];
    return (catalogQuery.data?.products ?? []).filter((item) =>
      item.name.toLowerCase().includes(keyword)
      || String(item.originProductNo).includes(keyword)
      || String(item.channelProductNo).includes(keyword));
  }, [catalogQuery.data?.products, catalogSearch]);
  const channelProductQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-products", "detail", originNumber],
    queryFn: () => fetchSmartStoreProduct(adminKey, originNumber),
    enabled: product !== null && validOrigin,
  });
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

  useEffect(() => {
    setCatalogPage(1);
    setCatalogSearch("");
  }, [product?.id]);

  useEffect(() => {
    setOriginChangeConfirmed(false);
  }, [originProductNo, previousOriginProductNo, product?.id]);

  useEffect(() => {
    setUnlinkRequested(false);
    setUnlinkConfirmed(false);
  }, [mapping?.mappingVersion, product?.id]);

  const refreshMappingOnConflict = async (error: unknown) => {
    if (error instanceof ApiError && error.status === 409) {
      await queryClient.invalidateQueries({
        queryKey: ["admin", "products", product?.id, "smartstore-inventory"],
      });
    }
  };

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
      expectedMappingVersion: mapping?.mappingVersion ?? null,
      previousOriginConfirmed: originChangeConfirmed,
    }),
    onSuccess: async (mapping) => {
      queryClient.setQueryData(
        ["admin", "products", product?.id, "smartstore-inventory"],
        mapping,
      );
      toast.show(enabled
        ? "스마트스토어 재고 연동을 저장하고 최신 재고 반영을 예약했습니다."
        : "스마트스토어 재고 연동을 비활성화했습니다.");
      await queryClient.invalidateQueries({
        queryKey: ["admin", "products", product?.id, "smartstore-product-preview"],
      });
    },
    onError: refreshMappingOnConflict,
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
    mutationFn: () => removeSmartStoreMapping(
      adminKey,
      product!.id,
      mapping!.mappingVersion,
      unlinkConfirmed,
    ),
    onSuccess: () => {
      queryClient.setQueryData(
        ["admin", "products", product?.id, "smartstore-inventory"],
        null,
      );
      toast.show("스마트스토어 재고 연동을 해제했습니다.");
      onClose();
    },
    onError: refreshMappingOnConflict,
  });

  const applyMutation = useAdminMutation(onAuthError, {
    mutationFn: () => applySmartStoreProduct(
      adminKey,
      product!.id,
      previewQuery.data!.previewVersion,
    ),
    onSuccess: async () => {
      toast.show("해피갤러리의 가격·판매 상태·옵션 가격을 스마트스토어에 반영했습니다.");
      await queryClient.invalidateQueries({
        queryKey: ["admin", "products", product?.id, "smartstore-product-preview"],
      });
    },
    onError: async (error) => {
      if (error instanceof ApiError && error.status === 409) {
        await queryClient.invalidateQueries({
          queryKey: ["admin", "products", product?.id, "smartstore-product-preview"],
        });
      }
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
        {(mappingQuery.isLoading || catalogQuery.isLoading || channelProductQuery.isLoading)
          && <LoadingSpinner />}
        <ErrorAlert error={mappingQuery.error ?? catalogQuery.error ?? channelProductQuery.error
          ?? previewQuery.error ?? saveMutation.error
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
                <div className="small">반영 대상: 스마트스토어 원상품 {previewQuery.data.originProductNo}</div>
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
                <Button size="sm" variant="warning"
                  disabled={applyMutation.isPending || previewQuery.isFetching || previewQuery.isError}
                  onClick={() => applyMutation.mutate()}>
                  {applyMutation.isPending ? "반영 중..." : "차이 반영"}
                </Button>
              )}
            </div>
            {previewQuery.data.options.some((option) => option.different) && (
              <Table responsive size="sm" className="mt-3 mb-0 align-middle">
                <thead><tr><th>옵션</th><th>해피갤러리 옵션가</th><th>스마트스토어 옵션가</th></tr></thead>
                <tbody>{previewQuery.data.options.filter((option) => option.different).map((option) => (
                  <tr key={option.optionId}>
                    <td>
                      {product ? variantLabel(product, option.productVariantId) : option.productVariantId}
                      <div className="small text-muted-soft">
                        스마트스토어 옵션 {option.optionId}
                        {!mapping.variants.some((variant) => variant.optionId === option.optionId)
                          && " · 이전 연결 (재고 0개)"}
                      </div>
                    </td>
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
          if (canSave) saveMutation.mutate();
        }}>
          <Form.Group className="mb-3" controlId="smartstore-origin-product-no">
            <Form.Label>스마트스토어 상품</Form.Label>
            <Form.Control
              className="mb-2"
              type="search"
              value={catalogSearch}
              onChange={(event) => setCatalogSearch(event.target.value)}
              placeholder="상품명·원상품 번호·채널상품 번호 검색"
            />
            {validOrigin && !catalogQuery.data?.products.some(
              (item) => item.originProductNo === originNumber,
            ) && <Alert variant="light" className="small py-2">
              현재 연결 상품 · 원상품 {originProductNo}
            </Alert>}
            <div className="border rounded overflow-auto" style={{ maxHeight: 280 }}>
              <Table hover size="sm" className="align-middle mb-0">
                <tbody>{catalogProducts.map((item) => (
                  <tr
                    key={item.channelProductNo}
                    className={item.originProductNo === originNumber ? "table-primary" : undefined}
                    role="button"
                    onClick={() => {
                      setOriginProductNo(String(item.originProductNo));
                      setOptionIds(Object.fromEntries(variants.map((variant) => [variant.id, ""])));
                    }}
                  >
                    <td style={{ width: 48 }}>
                      {item.imageUrl
                        ? <img src={item.imageUrl} alt="" width={40} height={40}
                          className="rounded object-fit-cover" />
                        : <div className="bg-light rounded" style={{ width: 40, height: 40 }} />}
                    </td>
                    <td>
                      <div className="fw-semibold">{item.name}</div>
                      <div className="small text-muted-soft">
                        원상품 {item.originProductNo} · 채널상품 {item.channelProductNo}
                      </div>
                    </td>
                    <td className="small text-end">
                      <div>{item.salePrice.toLocaleString()}원 · {item.status}</div>
                      <div className="text-muted-soft">재고 {item.stockQuantity ?? "-"}</div>
                    </td>
                  </tr>
                ))}</tbody>
              </Table>
              {!catalogProducts.length && <div className="small text-muted-soft p-3">
                이 페이지에 일치하는 상품이 없습니다.
              </div>}
            </div>
            {catalogQuery.data && catalogQuery.data.totalPages > 1 && (
              <div className="d-flex justify-content-between align-items-center mt-2">
                <Button type="button" size="sm" variant="outline-secondary"
                  disabled={catalogPage <= 1}
                  onClick={() => setCatalogPage((page) => page - 1)}>이전</Button>
                <span className="small text-muted-soft">
                  {catalogPage} / {catalogQuery.data.totalPages}페이지
                </span>
                <Button type="button" size="sm" variant="outline-secondary"
                  disabled={catalogPage >= catalogQuery.data.totalPages}
                  onClick={() => setCatalogPage((page) => page + 1)}>다음</Button>
              </div>
            )}
          </Form.Group>

          {product?.type === "MADE_TO_ORDER" && (
            <Table responsive size="sm" className="align-middle">
              <thead>
                <tr>
                  <th>해피갤러리 옵션 조합</th>
                  <th style={{ width: 360 }}>스마트스토어 옵션 조합</th>
                </tr>
              </thead>
              <tbody>
                {variants.map((variant) => (
                  <tr key={variant.id}>
                    <td>{variantLabel(product, variant.id)}</td>
                    <td>
                      <Form.Select
                        value={optionIds[variant.id] ?? ""}
                        onChange={(event) => setOptionIds((current) => ({
                          ...current,
                          [variant.id]: event.target.value,
                        }))}
                        aria-label={`${variantLabel(product, variant.id)} 스마트스토어 옵션 조합`}
                      >
                        <option value="">옵션 선택</option>
                        {channelProductQuery.data?.options.map((option) => (
                          <option key={option.optionId} value={option.optionId}>
                            {option.name} · 옵션가 {option.price.toLocaleString()}원
                            {` · 재고 ${option.stockQuantity}${option.usable ? "" : " · 판매 중지"}`}
                          </option>
                        ))}
                      </Form.Select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}

          {originChanged && (
            <Alert variant="warning">
              <div className="fw-semibold">원상품 변경 전 기존 상품을 확인해 주세요.</div>
              <div className="small mt-1">
                기존 원상품 {previousOriginProductNo}에서 새 원상품 {originProductNo}(으)로 변경합니다.
                저장 후에는 기존 원상품의 재고를 자동으로 보정하지 않습니다.
              </div>
              <Form.Check
                className="mt-2"
                id="smartstore-previous-origin-checked"
                label={`기존 원상품 ${previousOriginProductNo}의 판매 중지·재고 확인을 완료했습니다.`}
                checked={originChangeConfirmed}
                onChange={(event) => setOriginChangeConfirmed(event.target.checked)}
              />
            </Alert>
          )}

          <Form.Check
            className="mb-3"
            type="switch"
            id="smartstore-inventory-enabled"
            label="재고 변경 시 스마트스토어에 자동 반영"
            checked={enabled}
            onChange={(event) => setEnabled(event.target.checked)}
          />

          {unlinkRequested && mapping && (
            <Alert variant="danger">
              <div className="fw-semibold">연동 해제 전 기존 원상품을 확인해 주세요.</div>
              <div className="small mt-1">
                기존 원상품 {mapping.originProductNo}의 연결과 보존된 과거 옵션 연결을 모두 삭제합니다.
                해제 후에는 기존 원상품 재고를 자동으로 보정하지 않습니다.
              </div>
              <Form.Check
                className="mt-2"
                id="smartstore-unlink-origin-checked"
                label={`기존 원상품 ${mapping.originProductNo}의 판매 중지·재고 확인을 완료했습니다.`}
                checked={unlinkConfirmed}
                onChange={(event) => setUnlinkConfirmed(event.target.checked)}
              />
              <div className="d-flex justify-content-end gap-2 mt-3">
                <Button
                  type="button"
                  size="sm"
                  variant="outline-secondary"
                  disabled={deleteMutation.isPending}
                  onClick={() => {
                    deleteMutation.reset();
                    setUnlinkRequested(false);
                    setUnlinkConfirmed(false);
                  }}
                >
                  해제 취소
                </Button>
                <Button
                  type="button"
                  size="sm"
                  variant="danger"
                  disabled={!unlinkConfirmed || deleteMutation.isPending}
                  onClick={() => deleteMutation.mutate()}
                >
                  {deleteMutation.isPending ? "해제 중..." : "연동 해제 실행"}
                </Button>
              </div>
            </Alert>
          )}

          <div className="d-flex justify-content-between gap-2">
            <div>
              {mapping && !unlinkRequested && (
                <Button
                  type="button"
                  variant="outline-danger"
                  disabled={deleteMutation.isPending}
                  onClick={() => {
                    deleteMutation.reset();
                    setUnlinkRequested(true);
                  }}
                >
                  연동 해제
                </Button>
              )}
            </div>
            <Button
              type="submit"
              disabled={!canSave || saveMutation.isPending}
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
