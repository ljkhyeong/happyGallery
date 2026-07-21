import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Table } from "react-bootstrap";
import { ClassEditModal } from "./ClassEditModal";
import { fetchAdminClasses, updateClassStatus } from "./api";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function ClassListSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [pendingStatusId, setPendingStatusId] = useState<number | null>(null);
  const { data: classes, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "classes"],
    queryFn: () => fetchAdminClasses(adminKey),
  });

  const statusMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      updateClassStatus(adminKey, id, active ? "ACTIVE" : "INACTIVE"),
    onMutate: ({ id }) => setPendingStatusId(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "classes"] });
      queryClient.invalidateQueries({ queryKey: ["classes"] });
      queryClient.invalidateQueries({ queryKey: ["slots"] });
    },
    onSettled: () => setPendingStatusId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }
  if (!classes?.length) return <EmptyState message="등록된 클래스가 없습니다." />;

  const editingClass = classes.find((bookingClass) => bookingClass.id === editingId) ?? null;

  return (
    <>
      <ErrorAlert error={statusMutation.error} />
      <Table responsive hover size="sm">
        <thead>
          <tr>
            <th>ID</th>
            <th>클래스명</th>
            <th>카테고리</th>
            <th>소요 시간</th>
            <th className="text-end">가격</th>
            <th>8회권</th>
            <th>운영 상태</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {classes.map((bookingClass) => (
            <tr key={bookingClass.id}>
              <td>{bookingClass.id}</td>
              <td>{bookingClass.name}</td>
              <td>{bookingClass.category}</td>
              <td>{bookingClass.durationMin}분</td>
              <td className="text-end">{formatKRW(bookingClass.price)}</td>
              <td>
                <Badge bg={bookingClass.passEligible ? "primary" : "secondary"}>
                  {bookingClass.passEligible ? "사용 가능" : "사용 불가"}
                </Badge>
              </td>
              <td>
                <Badge bg={bookingClass.status === "ACTIVE" ? "success" : "secondary"}>
                  {bookingClass.status === "ACTIVE" ? "운영 중" : "운영 중지"}
                </Badge>
              </td>
              <td>
                <div className="d-flex gap-2 justify-content-end" style={{ minWidth: 176 }}>
                  <Button size="sm" variant="outline-dark" onClick={() => setEditingId(bookingClass.id)}>
                    정보 수정
                  </Button>
                  <Button
                    size="sm"
                    variant={bookingClass.status === "ACTIVE" ? "outline-danger" : "outline-success"}
                    disabled={pendingStatusId === bookingClass.id}
                    onClick={() => statusMutation.mutate({
                      id: bookingClass.id,
                      active: bookingClass.status !== "ACTIVE",
                    })}
                  >
                    {pendingStatusId === bookingClass.id
                      ? "처리 중..."
                      : bookingClass.status === "ACTIVE" ? "운영 중지" : "운영 재개"}
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>

      <ClassEditModal
        adminKey={adminKey}
        bookingClass={editingClass}
        onClose={() => setEditingId(null)}
        onAuthError={onAuthError}
      />
    </>
  );
}
