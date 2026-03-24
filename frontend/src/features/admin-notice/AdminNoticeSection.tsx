import { useEffect, useState } from "react";
import { Card, Badge, Button, Form } from "react-bootstrap";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchAdminNotices, createNotice, updateNotice, deleteNotice } from "./api";
import type { NoticeListItem } from "@/shared/types";
import { ApiError } from "@/shared/api";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function AdminNoticeSection({ adminKey, onAuthError: _onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const { data: notices, isLoading, error } = useQuery({
    queryKey: ["admin", "notices"],
    queryFn: () => fetchAdminNotices(adminKey),
  });

  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [pinned, setPinned] = useState(false);

  const resetForm = () => {
    setShowForm(false);
    setEditId(null);
    setTitle("");
    setContent("");
    setPinned(false);
  };

  const createMutation = useMutation({
    mutationFn: () => createNotice({ title, content, pinned }, adminKey),
    onSuccess: () => {
      toast.show("공지사항이 등록되었습니다.");
      resetForm();
      queryClient.invalidateQueries({ queryKey: ["admin", "notices"] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: () => updateNotice(editId!, { title, content, pinned }, adminKey),
    onSuccess: () => {
      toast.show("공지사항이 수정되었습니다.");
      resetForm();
      queryClient.invalidateQueries({ queryKey: ["admin", "notices"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteNotice(id, adminKey),
    onSuccess: () => {
      toast.show("공지사항이 삭제되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["admin", "notices"] });
    },
  });

  const startEdit = (n: NoticeListItem) => {
    setEditId(n.id);
    setTitle(n.title);
    setPinned(n.pinned);
    setContent("");
    setShowForm(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editId) {
      updateMutation.mutate();
    } else {
      createMutation.mutate();
    }
  };

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      _onAuthError();
    }
  }, [error, _onAuthError]);

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="mb-0">공지사항 관리</h5>
        <Button size="sm" variant="outline-primary" onClick={() => { resetForm(); setShowForm(!showForm); }}>
          {showForm ? "취소" : "새 공지 작성"}
        </Button>
      </div>

      {showForm && (
        <Card className="mb-3">
          <Card.Body>
            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-2">
                <Form.Control
                  size="sm"
                  placeholder="제목"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
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
                  required
                />
              </Form.Group>
              <Form.Check
                type="checkbox"
                label="상단 고정"
                checked={pinned}
                onChange={(e) => setPinned(e.target.checked)}
                className="mb-2"
              />
              <Button
                type="submit"
                size="sm"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {editId ? "수정" : "등록"}
              </Button>
              <ErrorAlert error={createMutation.error || updateMutation.error} />
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
                <Button size="sm" variant="outline-secondary" onClick={() => startEdit(n)}>
                  수정
                </Button>
                <Button
                  size="sm"
                  variant="outline-danger"
                  onClick={() => { if (confirm("삭제하시겠습니까?")) deleteMutation.mutate(n.id); }}
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
