import { useState } from "react";
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
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { isVersionConflict, useVersionedEdit } from "@/shared/hooks/useVersionedEdit";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
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

export function AdminNoticeSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();

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

  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [pinned, setPinned] = useState(false);
  const [actionError, setActionError] = useState<Error | null>(null);
  const edit = useVersionedEdit<NoticeDetailResponse>({
    onAuthError,
    isConflict: isVersionConflict,
    applyToForm: (notice) => {
      setTitle(notice.title);
      setContent(notice.content);
      setPinned(notice.pinned);
    },
    setError: setActionError,
    onConflict: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.admin.notices });
    },
  });

  const resetForm = () => {
    edit.reset();
    setShowForm(false);
    setEditId(null);
    setTitle("");
    setContent("");
    setPinned(false);
    setActionError(null);
  };

  const createMutation = useAdminMutation(onAuthError, {
    mutationFn: () => createNotice({ title, content, pinned }, adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("공지사항이 등록되었습니다.");
      resetForm();
      invalidateSavedNotices();
    },
    onError: setActionError,
  });

  const updateMutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateNotice(
      editId!,
      { expectedVersion: edit.version!, title, content, pinned },
      adminKey,
    ),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("공지사항이 수정되었습니다.");
      resetForm();
      invalidateSavedNotices();
    },
    onError: (error) => edit.handleSaveError(error, () => fetchAdminNotice(editId!, adminKey)),
  });

  const deleteMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ id, version }: Pick<NoticeListResponse, "id" | "version">) =>
      deleteNotice(id, version, adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("공지사항이 삭제되었습니다.");
      invalidateSavedNotices();
    },
    onError: edit.handleDeleteError,
  });

  const startEdit = (notice: NoticeListResponse) => {
    setEditId(notice.id);
    setTitle(notice.title);
    setContent("");
    setPinned(notice.pinned);
    setShowForm(true);
    void edit.startEdit(() => fetchAdminNotice(notice.id, adminKey));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editId !== null && edit.version !== null) {
      updateMutation.mutate();
    } else if (editId === null) {
      createMutation.mutate();
    }
  };

  return (
    <div>
      <div className="d-flex justify-content-end mb-3">
        <Button size="sm" variant="outline-primary" onClick={() => { resetForm(); setShowForm(!showForm); }}>
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
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  disabled={edit.loading}
                  required
                  aria-describedby="admin-notice-title-count"
                />
                <Form.Text id="admin-notice-title-count" className="text-muted d-block text-end">
                  {contentLengthLabel(title, CONTENT_TITLE_MAX_LENGTH)}
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
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  disabled={edit.loading}
                  required
                  aria-describedby="admin-notice-content-count"
                />
                <Form.Text id="admin-notice-content-count" className="text-muted d-block text-end">
                  {contentLengthLabel(content, CONTENT_BODY_MAX_LENGTH)}
                </Form.Text>
              </Form.Group>
              <Form.Check
                type="checkbox"
                label="상단 고정"
                checked={pinned}
                onChange={(e) => setPinned(e.target.checked)}
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
                  || createMutation.isPending
                  || updateMutation.isPending
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
