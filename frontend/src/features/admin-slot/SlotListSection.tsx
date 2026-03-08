import { useState, useCallback } from "react";
import { useMutation } from "@tanstack/react-query";
import { Table, Button, Badge } from "react-bootstrap";
import { deactivateSlot } from "./api";
import { useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { SlotResponse } from "@/shared/types";

interface Props {
  adminKey: string;
  slots: SlotResponse[];
  onUpdated: (slot: SlotResponse) => void;
}

export function SlotListSection({ adminKey, slots, onUpdated }: Props) {
  const toast = useToast();
  const [pendingId, setPendingId] = useState<number | null>(null);

  const mutation = useMutation({
    mutationFn: (slotId: number) => deactivateSlot(adminKey, slotId),
    onMutate: (slotId) => setPendingId(slotId),
    onSuccess: (slot) => {
      toast.show(`슬롯 #${slot.id} 비활성화 완료`);
      onUpdated(slot);
    },
    onSettled: () => setPendingId(null),
  });

  const handleDeactivate = useCallback(
    (id: number) => mutation.mutate(id),
    [mutation],
  );

  if (!slots.length) return null;

  return (
    <Table responsive hover size="sm">
      <thead>
        <tr>
          <th>ID</th>
          <th>클래스 ID</th>
          <th>시작</th>
          <th>종료</th>
          <th className="text-end">예약</th>
          <th>상태</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {slots.map((s) => (
          <tr key={s.id}>
            <td>{s.id}</td>
            <td>{s.classId}</td>
            <td>{formatDateTime(s.startAt)}</td>
            <td>{formatDateTime(s.endAt)}</td>
            <td className="text-end">
              {s.bookedCount}/{s.capacity}
            </td>
            <td>
              <Badge bg={s.isActive ? "success" : "secondary"}>
                {s.isActive ? "활성" : "비활성"}
              </Badge>
            </td>
            <td>
              {s.isActive && (
                <Button
                  size="sm"
                  variant="outline-danger"
                  disabled={pendingId === s.id}
                  onClick={() => handleDeactivate(s.id)}
                >
                  {pendingId === s.id ? "처리 중..." : "비활성화"}
                </Button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
