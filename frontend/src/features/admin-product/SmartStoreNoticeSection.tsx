import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Form, Modal, Table } from "react-bootstrap";
import type {
  SaveSmartStoreNoticeRequest,
  SaveSmartStoreNoticeRequestPostCategoryType,
} from "@/generated/api/adminCatalog";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import {
  applyNoticeToSmartStoreProducts,
  fetchSmartStoreNotice,
  fetchSmartStoreNotices,
  fetchSmartStoreProducts,
  removeSmartStoreNotice,
  saveSmartStoreNotice,
} from "./api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const CATEGORY_LABELS: Record<SaveSmartStoreNoticeRequestPostCategoryType, string> = {
  ORDINARY: "일반",
  EVENT: "이벤트",
  DELIVERY: "배송 지연",
  PRODUCT: "상품",
};

export function SmartStoreNoticeSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [applyingId, setApplyingId] = useState<number | null>(null);
  const [catalogPage, setCatalogPage] = useState(1);
  const [selectedProducts, setSelectedProducts] = useState<Set<number>>(new Set());
  const [form, setForm] = useState(emptyForm());
  const noticesKey = ["admin", "smartstore-notices"] as const;
  const notices = useAdminQuery(onAuthError, {
    queryKey: noticesKey,
    queryFn: () => fetchSmartStoreNotices(adminKey),
  });
  const detail = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-notices", editingId],
    queryFn: () => fetchSmartStoreNotice(adminKey, editingId!),
    enabled: typeof editingId === "number",
  });
  const catalog = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-products", "notice", catalogPage],
    queryFn: () => fetchSmartStoreProducts(adminKey, catalogPage),
    enabled: applyingId !== null,
  });

  useEffect(() => {
    if (!detail.data) return;
    setForm({
      postCategoryType: detail.data.postCategoryType as SaveSmartStoreNoticeRequestPostCategoryType,
      title: detail.data.title,
      detailContents: detail.data.detailContents,
      importantNotice: detail.data.importantNotice,
      importantNoticeStartDate: toLocalDateTime(detail.data.importantNoticeStartDate),
      importantNoticeEndDate: toLocalDateTime(detail.data.importantNoticeEndDate),
      wholeNotice: detail.data.wholeNotice,
      displayStartDate: toLocalDateTime(detail.data.displayStartDate),
      displayEndDate: toLocalDateTime(detail.data.displayEndDate),
      popup: detail.data.popup,
      popupStartDate: toLocalDateTime(detail.data.popupStartDate),
      popupEndDate: toLocalDateTime(detail.data.popupEndDate),
    });
  }, [detail.data]);

  const save = useAdminMutation(onAuthError, {
    mutationFn: () => saveSmartStoreNotice(
      adminKey,
      typeof editingId === "number" ? editingId : null,
      toRequest(form),
    ),
    onSuccess: async () => {
      toast.show(typeof editingId === "number"
        ? "스마트스토어 상품 공지를 수정했습니다."
        : "스마트스토어 상품 공지를 등록했습니다.");
      setEditingId(undefined);
      await queryClient.invalidateQueries({ queryKey: noticesKey });
    },
  });
  const remove = useAdminMutation(onAuthError, {
    mutationFn: (sellerNoticeId: number) => removeSmartStoreNotice(adminKey, sellerNoticeId),
    onSuccess: async () => {
      toast.show("스마트스토어 상품 공지를 삭제했습니다.");
      await queryClient.invalidateQueries({ queryKey: noticesKey });
    },
  });
  const apply = useAdminMutation(onAuthError, {
    mutationFn: () => applyNoticeToSmartStoreProducts(
      adminKey,
      applyingId!,
      [...selectedProducts],
    ),
    onSuccess: () => {
      toast.show("선택한 스마트스토어 상품에 공지를 적용했습니다.");
      setApplyingId(null);
      setSelectedProducts(new Set());
    },
  });

  if (notices.isLoading) return <LoadingSpinner />;
  if (notices.error) return <ErrorAlert error={notices.error} />;

  return <>
    <div className="d-flex justify-content-end mb-3">
      <Button size="sm" onClick={() => {
        setForm(emptyForm());
        setEditingId(null);
      }}>공지 등록</Button>
    </div>
    <ErrorAlert error={remove.error} />
    {!notices.data?.notices.length
      ? <EmptyState message="등록된 스마트스토어 상품 공지가 없습니다." />
      : <Table responsive hover size="sm" className="align-middle">
        <thead><tr><th>유형</th><th>제목</th><th>전시 기간</th><th>설정</th><th></th></tr></thead>
        <tbody>{notices.data.notices.map((notice) => <tr key={notice.sellerNoticeId}>
          <td>{CATEGORY_LABELS[notice.postCategoryType as SaveSmartStoreNoticeRequestPostCategoryType]
            ?? notice.postCategoryType}</td>
          <td>{notice.title}</td>
          <td className="small">
            {notice.displayStartDate ? formatDateTime(notice.displayStartDate) : "즉시"}
            {notice.displayEndDate ? ` ~ ${formatDateTime(notice.displayEndDate)}` : ""}
          </td>
          <td><div className="d-flex gap-1">
            {notice.importantNotice && <Badge bg="warning" text="dark">중요</Badge>}
            {notice.wholeNotice && <Badge bg="primary">전체</Badge>}
          </div></td>
          <td><div className="d-flex justify-content-end gap-1">
            <Button size="sm" variant="outline-primary" onClick={() => {
              setSelectedProducts(new Set());
              setCatalogPage(1);
              setApplyingId(notice.sellerNoticeId);
            }}>상품 적용</Button>
            <Button size="sm" variant="outline-secondary"
              onClick={() => setEditingId(notice.sellerNoticeId)}>수정</Button>
            <Button size="sm" variant="outline-danger" disabled={remove.isPending}
              onClick={() => {
                if (window.confirm("상품에 적용되지 않은 공지만 삭제할 수 있습니다. 삭제할까요?")) {
                  remove.mutate(notice.sellerNoticeId);
                }
              }}>삭제</Button>
          </div></td>
        </tr>)}</tbody>
      </Table>}

    <Modal show={editingId !== undefined} onHide={() => setEditingId(undefined)} size="lg" centered>
      <Modal.Header closeButton><Modal.Title className="fs-6">
        {editingId === null ? "스마트스토어 상품 공지 등록" : "스마트스토어 상품 공지 수정"}
      </Modal.Title></Modal.Header>
      <Form onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
        <Modal.Body>
          {detail.isLoading && <LoadingSpinner />}
          <ErrorAlert error={detail.error ?? save.error} />
          <div className="d-flex gap-2 mb-3">
            <Form.Select value={form.postCategoryType}
              onChange={(event) => setForm((current) => ({ ...current,
                postCategoryType: event.target.value as SaveSmartStoreNoticeRequestPostCategoryType }))}>
              {Object.entries(CATEGORY_LABELS).map(([value, label]) =>
                <option key={value} value={value}>{label}</option>)}
            </Form.Select>
            <Form.Control required value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              placeholder="공지 제목" />
          </div>
          <Form.Control as="textarea" rows={8} required value={form.detailContents}
            onChange={(event) => setForm((current) => ({ ...current, detailContents: event.target.value }))}
            placeholder="공지 내용" />
          <div className="d-flex flex-wrap gap-3 my-3">
            <Form.Check type="switch" id="smartstore-notice-important" label="중요 공지"
              checked={form.importantNotice}
              onChange={(event) => setForm((current) => ({ ...current, importantNotice: event.target.checked }))} />
            <Form.Check type="switch" id="smartstore-notice-whole" label="전체 상품 공지"
              checked={form.wholeNotice}
              onChange={(event) => setForm((current) => ({ ...current, wholeNotice: event.target.checked }))} />
            <Form.Check type="switch" id="smartstore-notice-popup" label="팝업 공지"
              checked={form.popup}
              onChange={(event) => setForm((current) => ({ ...current, popup: event.target.checked }))} />
          </div>
          <NoticeDateFields form={form} onChange={setForm} />
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setEditingId(undefined)}>취소</Button>
          <Button type="submit" disabled={save.isPending || detail.isLoading}>
            {save.isPending ? "저장 중..." : "저장"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>

    <Modal show={applyingId !== null} onHide={() => setApplyingId(null)} size="lg" centered>
      <Modal.Header closeButton><Modal.Title className="fs-6">공지를 적용할 상품 선택</Modal.Title></Modal.Header>
      <Modal.Body>
        {catalog.isLoading && <LoadingSpinner />}
        <ErrorAlert error={catalog.error ?? apply.error} />
        <Table responsive hover size="sm" className="align-middle">
          <thead><tr><th style={{ width: 44 }}></th><th>상품</th><th>상태·재고</th></tr></thead>
          <tbody>{catalog.data?.products.map((product) => <tr key={product.channelProductNo}>
            <td><Form.Check checked={selectedProducts.has(product.channelProductNo)}
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
        {catalog.data && catalog.data.totalPages > 1 && <div className="d-flex justify-content-between">
          <Button size="sm" variant="outline-secondary" disabled={catalogPage <= 1}
            onClick={() => setCatalogPage((page) => page - 1)}>이전</Button>
          <span className="small text-muted-soft">{catalogPage} / {catalog.data.totalPages}페이지</span>
          <Button size="sm" variant="outline-secondary" disabled={catalogPage >= catalog.data.totalPages}
            onClick={() => setCatalogPage((page) => page + 1)}>다음</Button>
        </div>}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={() => setApplyingId(null)}>취소</Button>
        <Button disabled={!selectedProducts.size || apply.isPending} onClick={() => apply.mutate()}>
          {apply.isPending ? "적용 중..." : `${selectedProducts.size}개 상품에 적용`}
        </Button>
      </Modal.Footer>
    </Modal>
  </>;
}

type NoticeForm = {
  postCategoryType: SaveSmartStoreNoticeRequestPostCategoryType;
  title: string;
  detailContents: string;
  importantNotice: boolean;
  importantNoticeStartDate: string;
  importantNoticeEndDate: string;
  wholeNotice: boolean;
  displayStartDate: string;
  displayEndDate: string;
  popup: boolean;
  popupStartDate: string;
  popupEndDate: string;
};

function emptyForm(): NoticeForm {
  return {
    postCategoryType: "ORDINARY", title: "", detailContents: "",
    importantNotice: false, importantNoticeStartDate: "", importantNoticeEndDate: "",
    wholeNotice: false, displayStartDate: "", displayEndDate: "",
    popup: false, popupStartDate: "", popupEndDate: "",
  };
}

function toRequest(form: NoticeForm): SaveSmartStoreNoticeRequest {
  return {
    postCategoryType: form.postCategoryType,
    title: form.title,
    detailContents: form.detailContents,
    importantNotice: form.importantNotice,
    importantNoticeStartDate: form.importantNotice ? toIso(form.importantNoticeStartDate) : undefined,
    importantNoticeEndDate: form.importantNotice ? toIso(form.importantNoticeEndDate) : undefined,
    wholeNotice: form.wholeNotice,
    displayStartDate: toIso(form.displayStartDate),
    displayEndDate: toIso(form.displayEndDate),
    popup: form.popup,
    popupStartDate: form.popup ? toIso(form.popupStartDate) : undefined,
    popupEndDate: form.popup ? toIso(form.popupEndDate) : undefined,
  };
}

function NoticeDateFields({ form, onChange }: {
  form: NoticeForm;
  onChange: (updater: (current: NoticeForm) => NoticeForm) => void;
}) {
  const field = (name: keyof NoticeForm, label: string) => <Form.Group>
    <Form.Label className="small">{label}</Form.Label>
    <Form.Control type="datetime-local" value={String(form[name])}
      onChange={(event) => onChange((current) => ({ ...current, [name]: event.target.value }))} />
  </Form.Group>;
  return <div className="row g-2">
    <div className="col-md-6">{field("displayStartDate", "전시 시작")}</div>
    <div className="col-md-6">{field("displayEndDate", "전시 종료")}</div>
    {form.importantNotice && <>
      <div className="col-md-6">{field("importantNoticeStartDate", "중요 공지 시작")}</div>
      <div className="col-md-6">{field("importantNoticeEndDate", "중요 공지 종료")}</div>
    </>}
    {form.popup && <>
      <div className="col-md-6">{field("popupStartDate", "팝업 시작")}</div>
      <div className="col-md-6">{field("popupEndDate", "팝업 종료")}</div>
    </>}
  </div>;
}

function toIso(value: string): string | undefined {
  return value ? new Date(value).toISOString() : undefined;
}

function toLocalDateTime(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}
