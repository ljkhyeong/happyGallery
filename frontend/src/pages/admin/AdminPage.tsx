import { useState, useCallback } from "react";
import { Container, Card, Button } from "react-bootstrap";
import { useAdminKey } from "@/features/admin-product/useAdminKey";
import { AdminKeyGate } from "@/features/admin-product/AdminKeyGate";
import { ProductListSection } from "@/features/admin-product/ProductListSection";
import { CreateProductForm } from "@/features/admin-product/CreateProductForm";
import { CreateSlotForm } from "@/features/admin-slot/CreateSlotForm";
import { SlotListSection } from "@/features/admin-slot/SlotListSection";
import type { SlotResponse } from "@/shared/types";

export function AdminPage() {
  const { adminKey, setAdminKey, clearAdminKey, isAuthenticated } = useAdminKey();
  const [slots, setSlots] = useState<SlotResponse[]>([]);

  const handleSlotCreated = useCallback((slot: SlotResponse) => {
    setSlots((prev) => [slot, ...prev]);
  }, []);

  const handleSlotUpdated = useCallback((updated: SlotResponse) => {
    setSlots((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
  }, []);

  if (!isAuthenticated) {
    return <AdminKeyGate onSubmit={setAdminKey} />;
  }

  return (
    <Container className="page-container">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="mb-0">관리자</h4>
        <Button size="sm" variant="outline-secondary" onClick={clearAdminKey}>
          로그아웃
        </Button>
      </div>

      <Card className="mb-4">
        <Card.Header>상품 등록</Card.Header>
        <Card.Body>
          <CreateProductForm adminKey={adminKey} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>상품 목록</Card.Header>
        <Card.Body>
          <ProductListSection adminKey={adminKey} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>슬롯 생성</Card.Header>
        <Card.Body>
          <CreateSlotForm adminKey={adminKey} onCreated={handleSlotCreated} />
        </Card.Body>
      </Card>

      {slots.length > 0 && (
        <Card className="mb-4">
          <Card.Header>생성된 슬롯</Card.Header>
          <Card.Body>
            <SlotListSection
              adminKey={adminKey}
              slots={slots}
              onUpdated={handleSlotUpdated}
            />
          </Card.Body>
        </Card>
      )}
    </Container>
  );
}
