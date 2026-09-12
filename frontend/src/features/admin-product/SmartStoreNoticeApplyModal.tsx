import { useState } from "react";
import { Button, Form, Modal, Table } from "react-bootstrap";
import type { SmartStoreNoticeResponse } from "@/generated/api/adminCatalog";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { applyNoticeToSmartStoreProducts, fetchSmartStoreProducts } from "./api";

interface Props {
  adminKey: string;
  notice: Pick<SmartStoreNoticeResponse, "sellerNoticeId" | "title">;
  onAuthError: () => void;
  onClose: () => void;
}

export function SmartStoreNoticeApplyModal({ adminKey, notice, onAuthError, onClose }: Props) {
  const toast = useToast();
  const [catalogPage, setCatalogPage] = useState(1);
  const [selectedProducts, setSelectedProducts] = useState<Set<number>>(new Set());
  const catalog = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-products", "notice", catalogPage],
    queryFn: () => fetchSmartStoreProducts(adminKey, catalogPage),
  });
  const apply = useAdminMutation(onAuthError, {
    mutationFn: (channelProductNos: number[]) => applyNoticeToSmartStoreProducts(
      adminKey, notice.sellerNoticeId, channelProductNos,
    ),
    onSuccess: () => {
      toast.show("선택한 스마트스토어 상품에 공지를 적용했습니다.");
      onClose();
    },
  });
  const close = () => { if (!apply.isPending) onClose(); };

  return <Modal show onHide={close} size="lg" centered>
    <Modal.Header closeButton={!apply.isPending}>
      <Modal.Title className="fs-6">공지를 적용할 상품 선택</Modal.Title>
    </Modal.Header>
    <Modal.Body>
      <p className="small">선택한 공지: {notice.title}</p>
      <fieldset disabled={apply.isPending}>
        {catalog.isLoading && <LoadingSpinner />}
        <ErrorAlert error={catalog.error}
          onRetry={() => { void catalog.refetch(); }} retrying={catalog.isFetching} />
        <ErrorAlert error={apply.error} />
        {catalog.data && (catalog.data.products.length ? (
          <Table responsive hover size="sm" className="align-middle">
            <thead><tr><th style={{ width: 44 }}></th><th>상품</th><th>상태·재고</th></tr></thead>
            <tbody>{catalog.data.products.map((product) => <tr key={product.channelProductNo}>
              <td><Form.Check
                aria-label={`채널상품 ${product.channelProductNo} 선택`}
                checked={selectedProducts.has(product.channelProductNo)}
                onChange={(event) => setSelectedProducts((current) => {
                  const next = new Set(current);
                  if (event.target.checked) next.add(product.channelProductNo);
                  else next.delete(product.channelProductNo);
                  return next;
                })} /></td>
              <td><div>{product.name}</div><div className="small text-muted-soft">
                채널상품 {product.channelProductNo}
              </div></td>
              <td>{product.status} · 재고 {product.stockQuantity ?? "-"}</td>
            </tr>)}</tbody>
          </Table>
        ) : <EmptyState message="선택할 스마트스토어 상품이 없습니다." />)}
        {(catalogPage > 1 || (catalog.data?.totalPages ?? 0) > 1) && (
          <nav aria-label="공지 적용 상품 페이지" className="d-flex flex-wrap align-items-center justify-content-between gap-2">
            <Button size="sm" variant="outline-secondary"
              disabled={catalogPage <= 1 || catalog.isFetching}
              onClick={() => setCatalogPage((page) => page - 1)}>이전</Button>
            <span className="small text-muted-soft">
              {catalogPage}페이지{catalog.data && ` · 총 ${catalog.data.totalElements}개`}
            </span>
            <Button size="sm" variant="outline-secondary"
              disabled={!catalog.data || catalog.isError || catalog.isFetching
                || catalogPage >= catalog.data.totalPages}
              onClick={() => setCatalogPage((page) => page + 1)}>다음</Button>
          </nav>
        )}
      </fieldset>
    </Modal.Body>
    <Modal.Footer>
      <Button variant="secondary" disabled={apply.isPending} onClick={close}>취소</Button>
      <Button disabled={!selectedProducts.size || apply.isPending}
        onClick={() => apply.mutate([...selectedProducts])}>
        {apply.isPending ? "적용 중..." : `${selectedProducts.size}개 상품에 적용`}
      </Button>
    </Modal.Footer>
  </Modal>;
}
