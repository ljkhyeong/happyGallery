import { Card, Badge, Button, Form } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import {
  fetchAdminNotice,
  fetchAdminNotices,
  createNotice,
  updateNotice,
  deleteNotice,
  type NoticeDetailResponse,
  type NoticeListResponse,
} from "./api";
import { queryKeys } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useVersionedCrudForm } from "@/shared/hooks/useVersionedCrudForm";
import { ErrorAlert, LoadingSpinner, EmptyState } from "@/shared/ui";
import { VersionConflictAlert } from "@/shared/ui/VersionConflictAlert";
import { formatDateTime } from "@/shared/lib";
import {
  CONTENT_BODY_MAX_LENGTH,
  CONTENT_TITLE_MAX_LENGTH,
  contentLengthLabel,
} from "@/shared/validation/contentText";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

interface NoticeFormState {
  title: string;
  content: string;
  pinned: boolean;
}

function emptyForm(): NoticeFormState {
  return { title: "", content: "", pinned: false };
}

export function AdminNoticeSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();

  const { data: notices, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.notices,
    queryFn: () => fetchAdminNotices(adminKey),
  });

  const invalidateSavedNotices = () => {
    void Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.notices }),
      queryClient.invalidateQueries({ queryKey: queryKeys.notices.all }),
    ]);
  };

  const {
    edit,
    form,
    setForm,
    showForm,
    editId,
    actionError,
    saving,
    deleteMutation,
    startEdit,
    toggleForm,
    submit,
  } = useVersionedCrudForm<NoticeListResponse, NoticeDetailResponse, NoticeFormState>({
    onAuthError,
    emptyForm,
    // 목록 응답에는 본문이 없어 제목·고정만 먼저 채우고, 본문은 최신 상세를 읽어 채운다.
    draftFrom: (notice) => ({ title: notice.title, content: "", pinned: notice.pinned }),
    formFrom: (notice) => ({ title: notice.title, content: notice.content, pinned: notice.pinned }),
    fetchLatest: (id) => fetchAdminNotice(id, adminKey),
    create: (draft) => createNotice(draft, adminKey),
    update: (id, draft, expectedVersion) => updateNotice(id, { expectedVersion, ...draft }, adminKey),
    remove: (id, version) => deleteNotice(id, version, adminKey),
    onSaved: invalidateSavedNotices,
    onConflict: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.admin.notices });
    },
    messages: {
      created: "공지사항이 등록되었습니다.",
      updated: "공지사항이 수정되었습니다.",
      deleted: "공지사항이 삭제되었습니다.",
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submit();
  };

  return (
    <div>
      <div className="d-flex justify-content-end mb-3">
        <Button size="sm" variant="outline-primary" onClick={toggleForm}>
          {showForm ? "취소" : "새 공지 작성"}
        </Button>
      </div>

      <ErrorAlert error={actionError} />

      {showForm && (
        <Card className="mb-3">
          <Card.Body>
            {edit.loading && <LoadingSpinner text="공지사항 불러오는 중..." />}
            {edit.conflict && (
              <VersionConflictAlert
                message="다른 관리자가 먼저 수정했습니다. 작성 중인 초안은 그대로 보존했습니다."
                onKeepDraft={edit.keepDraft}
                onLoadLatest={edit.loadLatest}
              />
            )}
            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-2" controlId="admin-notice-title">
                <Form.Label>제목</Form.Label>
                <Form.Control
                  size="sm"
                  placeholder="제목"
                  maxLength={CONTENT_TITLE_MAX_LENGTH}
                  value={form.title}
                  onChange={(e) => setForm((current) => ({ ...current, title: e.target.value }))}
                  disabled={edit.loading}
                  required
                  aria-describedby="admin-notice-title-count"
                />
                <Form.Text id="admin-notice-title-count" className="text-muted d-block text-end">
                  {contentLengthLabel(form.title, CONTENT_TITLE_MAX_LENGTH)}
                </Form.Text>
              </Form.Group>
              <Form.Group className="mb-2" controlId="admin-notice-content">
                <Form.Label>내용</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  size="sm"
                  placeholder="내용"
                  maxLength={CONTENT_BODY_MAX_LENGTH}
                  value={form.content}
                  onChange={(e) => setForm((current) => ({ ...current, content: e.target.value }))}
                  disabled={edit.loading}
                  required
                  aria-describedby="admin-notice-content-count"
                />
                <Form.Text id="admin-notice-content-count" className="text-muted d-block text-end">
                  {contentLengthLabel(form.content, CONTENT_BODY_MAX_LENGTH)}
                </Form.Text>
              </Form.Group>
              <Form.Check
                type="checkbox"
                label="상단 고정"
                checked={form.pinned}
                onChange={(e) => setForm((current) => ({ ...current, pinned: e.target.checked }))}
                disabled={edit.loading}
                className="mb-2"
              />
              <Button
                type="submit"
                size="sm"
                disabled={
                  edit.loading
                  || edit.conflict !== null
                  || (editId !== null && edit.version === null)
                  || saving
                }
              >
                {editId !== null ? "수정" : "등록"}
              </Button>
            </Form>
          </Card.Body>
        </Card>
      )}

      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {notices && notices.length === 0 && <EmptyState message="공지사항이 없습니다." />}

      {notices?.map((n) => (
        <Card key={n.id} className="mb-2">
          <Card.Body className="py-2 px-3">
            <div className="d-flex justify-content-between align-items-center">
              <div className="d-flex align-items-center gap-2">
                {n.pinned && <Badge bg="dark" className="badge-sm">고정</Badge>}
                <span className="fw-semibold small">{n.title}</span>
              </div>
              <div className="d-flex align-items-center gap-2">
                <span className="text-muted-soft" style={{ fontSize: "0.8rem" }}>
                  조회 {n.viewCount} | {formatDateTime(n.createdAt)}
                </span>
                <Button
                  size="sm"
                  variant="outline-secondary"
                  onClick={() => startEdit(n)}
                  disabled={edit.loading}
                >
                  수정
                </Button>
                <Button
                  size="sm"
                  variant="outline-danger"
                  onClick={() => {
                    if (confirm("삭제하시겠습니까?")) {
                      deleteMutation.mutate({ id: n.id, version: n.version });
                    }
                  }}
                  disabled={deleteMutation.isPending}
                >
                  삭제
                </Button>
              </div>
            </div>
          </Card.Body>
        </Card>
      ))}
    </div>
  );
}
