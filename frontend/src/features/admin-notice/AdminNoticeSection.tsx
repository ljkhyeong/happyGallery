import { useRef, useState } from "react";
import { Alert, Card, Badge, Button, Form } from "react-bootstrap";
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
import { ApiError } from "@/shared/api";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function AdminNoticeSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const { data: notices, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "notices"],
    queryFn: () => fetchAdminNotices(adminKey),
  });

  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [editVersion, setEditVersion] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [pinned, setPinned] = useState(false);
  const [actionError, setActionError] = useState<Error | null>(null);
  const [editLoading, setEditLoading] = useState(false);
  const [conflict, setConflict] = useState<NoticeDetailResponse | null>(null);
  const editRequestId = useRef(0);

  const resetForm = () => {
    editRequestId.current += 1;
    setShowForm(false);
    setEditId(null);
    setEditVersion(null);
    setTitle("");
    setContent("");
    setPinned(false);
    setActionError(null);
    setEditLoading(false);
    setConflict(null);
  };

  const hydrateEditForm = (notice: NoticeDetailResponse) => {
    setEditVersion(notice.version);
    setTitle(notice.title);
    setContent(notice.content);
    setPinned(notice.pinned);
  };

  const createMutation = useAdminMutation(onAuthError, {
    mutationFn: () => createNotice({ title, content, pinned }, adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("공지사항이 등록되었습니다.");
      resetForm();
      queryClient.invalidateQueries({ queryKey: ["admin", "notices"] });
    },
    onError: setActionError,
  });

  const updateMutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateNotice(
      editId!,
      { expectedVersion: editVersion!, title, content, pinned },
      adminKey,
    ),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("공지사항이 수정되었습니다.");
      resetForm();
      queryClient.invalidateQueries({ queryKey: ["admin", "notices"] });
    },
    onError: async (error) => {
      if (!(error instanceof ApiError) || error.status !== 409 || editId === null) {
        setActionError(error);
        return;
      }

      const requestId = editRequestId.current;
      setEditLoading(true);
      try {
        const latest = await fetchAdminNotice(editId, adminKey);
        if (requestId !== editRequestId.current) return;
        setConflict(latest);
        setActionError(null);
        queryClient.invalidateQueries({ queryKey: ["admin", "notices"] });
      } catch (refreshError) {
        if (requestId !== editRequestId.current) return;
        if (isAdminSessionUnauthorized(refreshError)) onAuthError();
        setActionError(
          refreshError instanceof Error
            ? refreshError
            : new Error("최신 공지사항을 불러오지 못했습니다."),
        );
      } finally {
        if (requestId === editRequestId.current) setEditLoading(false);
      }
    },
  });

  const deleteMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ id, version }: Pick<NoticeListResponse, "id" | "version">) =>
      deleteNotice(id, version, adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("공지사항이 삭제되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["admin", "notices"] });
    },
    onError: (error) => {
      setActionError(error);
      if (error instanceof ApiError && error.status === 409) {
        queryClient.invalidateQueries({ queryKey: ["admin", "notices"] });
      }
    },
  });

  const startEdit = async (notice: NoticeListResponse) => {
    const requestId = ++editRequestId.current;
    setEditId(notice.id);
    setEditVersion(null);
    setTitle(notice.title);
    setContent("");
    setPinned(notice.pinned);
    setActionError(null);
    setConflict(null);
    setShowForm(true);
    setEditLoading(true);

    try {
      const detail = await fetchAdminNotice(notice.id, adminKey);
      if (requestId !== editRequestId.current) return;
      hydrateEditForm(detail);
    } catch (editError) {
      if (requestId !== editRequestId.current) return;
      if (isAdminSessionUnauthorized(editError)) onAuthError();
      setActionError(
        editError instanceof Error
          ? editError
          : new Error("공지사항을 불러오지 못했습니다."),
      );
    } finally {
      if (requestId === editRequestId.current) setEditLoading(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editId !== null && editVersion !== null) {
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
            {editLoading && <LoadingSpinner text="공지사항 불러오는 중..." />}
            {conflict && (
              <Alert variant="warning">
                <p className="mb-2">
                  다른 관리자가 먼저 수정했습니다. 작성 중인 초안은 그대로 보존했습니다.
                </p>
                <div className="d-flex flex-wrap gap-2">
                  <Button
                    type="button"
                    size="sm"
                    variant="outline-dark"
                    onClick={() => {
                      setEditVersion(conflict.version);
                      setConflict(null);
                      toast.show("최신 버전을 반영했습니다. 초안을 확인한 뒤 다시 저장해 주세요.");
                    }}
                  >
                    내 초안 유지
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline-secondary"
                    onClick={() => {
                      hydrateEditForm(conflict);
                      setConflict(null);
                    }}
                  >
                    서버 최신 내용 불러오기
                  </Button>
                </div>
              </Alert>
            )}
            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-2">
                <Form.Control
                  size="sm"
                  placeholder="제목"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  disabled={editLoading}
                  required
                />
              </Form.Group>
              <Form.Group className="mb-2">
                <Form.Control
                  as="textarea"
                  rows={4}
                  size="sm"
                  placeholder="내용"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  disabled={editLoading}
                  required
                />
              </Form.Group>
              <Form.Check
                type="checkbox"
                label="상단 고정"
                checked={pinned}
                onChange={(e) => setPinned(e.target.checked)}
                disabled={editLoading}
                className="mb-2"
              />
              <Button
                type="submit"
                size="sm"
                disabled={
                  editLoading
                  || conflict !== null
                  || (editId !== null && editVersion === null)
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
                  onClick={() => void startEdit(n)}
                  disabled={editLoading}
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
