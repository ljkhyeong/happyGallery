import { useEffect, useState } from "react";
import { Button, Col, Form, Row } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { fetchAdminWorkshopProfile, updateWorkshopProfile } from "@/features/workshop/api";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const initialForm = {
  name: "",
  phone: "",
  postalCode: "",
  addressLine1: "",
  addressLine2: "",
  businessHours: "",
  mapUrl: "",
  parkingInfo: "",
};

export function WorkshopProfileForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [form, setForm] = useState(initialForm);
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "workshop-profile"],
    queryFn: () => fetchAdminWorkshopProfile(adminKey),
  });

  useEffect(() => {
    if (!query.data) return;
    setForm({
      name: query.data.name,
      phone: query.data.phone ?? "",
      postalCode: query.data.postalCode ?? "",
      addressLine1: query.data.addressLine1 ?? "",
      addressLine2: query.data.addressLine2 ?? "",
      businessHours: query.data.businessHours ?? "",
      mapUrl: query.data.mapUrl ?? "",
      parkingInfo: query.data.parkingInfo ?? "",
    });
  }, [query.data]);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateWorkshopProfile(adminKey, {
      name: form.name,
      phone: form.phone.trim() || null,
      postalCode: form.postalCode.trim() || null,
      addressLine1: form.addressLine1.trim() || null,
      addressLine2: form.addressLine2.trim() || null,
      businessHours: form.businessHours.trim() || null,
      mapUrl: form.mapUrl.trim() || null,
      parkingInfo: form.parkingInfo.trim() || null,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "workshop-profile"] });
      queryClient.invalidateQueries({ queryKey: ["workshop-profile"] });
      toast.show("공방 방문 정보가 저장되었습니다.");
    },
  });

  const update = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  if (query.isLoading) return <LoadingSpinner />;

  return (
    <Form onSubmit={(event) => {
      event.preventDefault();
      if (form.name.trim()) mutation.mutate();
    }}>
      <ErrorAlert error={query.error || mutation.error} />
      <Row className="g-3">
        <Col md={6}>
          <Form.Group controlId="admin-workshop-name">
            <Form.Label>공방명</Form.Label>
            <Form.Control value={form.name} maxLength={100} onChange={(e) => update("name", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="admin-workshop-phone">
            <Form.Label>연락처</Form.Label>
            <Form.Control value={form.phone} maxLength={30} onChange={(e) => update("phone", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group controlId="admin-workshop-postal-code">
            <Form.Label>우편번호</Form.Label>
            <Form.Control value={form.postalCode} maxLength={20} onChange={(e) => update("postalCode", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={5}>
          <Form.Group controlId="admin-workshop-address-line1">
            <Form.Label>기본 주소</Form.Label>
            <Form.Control value={form.addressLine1} maxLength={200} onChange={(e) => update("addressLine1", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group controlId="admin-workshop-address-line2">
            <Form.Label>상세 주소</Form.Label>
            <Form.Control value={form.addressLine2} maxLength={200} onChange={(e) => update("addressLine2", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="admin-workshop-business-hours">
            <Form.Label>운영시간</Form.Label>
            <Form.Control as="textarea" rows={3} value={form.businessHours} maxLength={1000} onChange={(e) => update("businessHours", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="admin-workshop-parking-info">
            <Form.Label>주차 안내</Form.Label>
            <Form.Control as="textarea" rows={3} value={form.parkingInfo} maxLength={1000} onChange={(e) => update("parkingInfo", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={9}>
          <Form.Group controlId="admin-workshop-map-url">
            <Form.Label>지도 URL</Form.Label>
            <Form.Control type="url" value={form.mapUrl} maxLength={500} onChange={(e) => update("mapUrl", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={3} className="d-flex align-items-end">
          <Button className="w-100" type="submit" disabled={!form.name.trim() || mutation.isPending}>
            {mutation.isPending ? "저장 중..." : "방문 정보 저장"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
