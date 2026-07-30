import { useEffect, useRef, useState } from "react";
import { Alert, Button, Col, Form, Row } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { fetchAdminWorkshopProfile, updateWorkshopProfile } from "@/features/workshop/api";
import { ApiError } from "@/shared/api";
import type { WorkshopProfile } from "@/shared/types";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
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
  businessRegistrationNumber: "",
  representativeName: "",
  email: "",
  mailOrderRegistrationNumber: "",
  introduction: "",
  kakaoTalkId: "",
  naverTalkUrl: "",
  naverBlogUrl: "",
  instagramUrl: "",
  smartStoreUrl: "",
};

function profileToForm(profile: WorkshopProfile): typeof initialForm {
  return {
    name: profile.name,
    phone: profile.phone ?? "",
    postalCode: profile.postalCode ?? "",
    addressLine1: profile.addressLine1 ?? "",
    addressLine2: profile.addressLine2 ?? "",
    businessHours: profile.businessHours ?? "",
    mapUrl: profile.mapUrl ?? "",
    parkingInfo: profile.parkingInfo ?? "",
    businessRegistrationNumber: profile.businessRegistrationNumber ?? "",
    representativeName: profile.representativeName ?? "",
    email: profile.email ?? "",
    mailOrderRegistrationNumber: profile.mailOrderRegistrationNumber ?? "",
    introduction: profile.introduction ?? "",
    kakaoTalkId: profile.kakaoTalkId ?? "",
    naverTalkUrl: profile.naverTalkUrl ?? "",
    naverBlogUrl: profile.naverBlogUrl ?? "",
    instagramUrl: profile.instagramUrl ?? "",
    smartStoreUrl: profile.smartStoreUrl ?? "",
  };
}

export function WorkshopProfileForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [form, setForm] = useState(initialForm);
  const [expectedVersion, setExpectedVersion] = useState<number | null>(null);
  const [conflict, setConflict] = useState<WorkshopProfile | null>(null);
  const [conflictLoading, setConflictLoading] = useState(false);
  const [conflictRefreshError, setConflictRefreshError] = useState<Error | null>(null);
  const initiallyHydrated = useRef(false);
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "workshop-profile"],
    queryFn: () => fetchAdminWorkshopProfile(adminKey),
  });

  useEffect(() => {
    if (!query.data || initiallyHydrated.current) return;
    initiallyHydrated.current = true;
    setExpectedVersion(query.data.version);
    setForm(profileToForm(query.data));
  }, [query.data]);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateWorkshopProfile(adminKey, {
      expectedVersion: expectedVersion!,
      name: form.name,
      phone: form.phone.trim() || null,
      postalCode: form.postalCode.trim() || null,
      addressLine1: form.addressLine1.trim() || null,
      addressLine2: form.addressLine2.trim() || null,
      businessHours: form.businessHours.trim() || null,
      mapUrl: form.mapUrl.trim() || null,
      parkingInfo: form.parkingInfo.trim() || null,
      businessRegistrationNumber: form.businessRegistrationNumber.trim() || null,
      representativeName: form.representativeName.trim() || null,
      email: form.email.trim() || null,
      mailOrderRegistrationNumber: form.mailOrderRegistrationNumber.trim() || null,
      introduction: form.introduction.trim() || null,
      kakaoTalkId: form.kakaoTalkId.trim() || null,
      naverTalkUrl: form.naverTalkUrl.trim() || null,
      naverBlogUrl: form.naverBlogUrl.trim() || null,
      instagramUrl: form.instagramUrl.trim() || null,
      smartStoreUrl: form.smartStoreUrl.trim() || null,
    }),
    onMutate: () => setConflictRefreshError(null),
    onSuccess: (saved) => {
      setExpectedVersion(saved.version);
      setForm(profileToForm(saved));
      setConflict(null);
      queryClient.setQueryData(["admin", "workshop-profile"], saved);
      queryClient.invalidateQueries({ queryKey: ["workshop-profile"] });
      toast.show("공방 정보가 저장되었습니다.");
    },
    onError: async (error) => {
      if (!(error instanceof ApiError) || error.status !== 409) return;

      setConflictLoading(true);
      try {
        const latest = await fetchAdminWorkshopProfile(adminKey);
        queryClient.setQueryData(["admin", "workshop-profile"], latest);
        setConflict(latest);
      } catch (refreshError) {
        if (isAdminSessionUnauthorized(refreshError)) onAuthError();
        setConflictRefreshError(
          refreshError instanceof Error
            ? refreshError
            : new Error("최신 공방 정보를 불러오지 못했습니다."),
        );
      } finally {
        setConflictLoading(false);
      }
    },
  });

  const update = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  if (query.isLoading) return <LoadingSpinner />;

  return (
    <Form onSubmit={(event) => {
      event.preventDefault();
      if (form.name.trim() && expectedVersion !== null) mutation.mutate();
    }}>
      <ErrorAlert
        error={
          query.error
          || conflictRefreshError
          || (conflict ? null : mutation.error)
        }
      />
      {conflictLoading && <LoadingSpinner text="최신 공방 정보 확인 중..." />}
      {conflict && (
        <Alert variant="warning">
          <p className="mb-2">
            다른 관리자가 공방 정보를 먼저 수정했습니다. 작성 중인 초안은 그대로 보존했습니다.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Button
              type="button"
              size="sm"
              variant="outline-dark"
              onClick={() => {
                setExpectedVersion(conflict.version);
                setConflict(null);
                mutation.reset();
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
                setExpectedVersion(conflict.version);
                setForm(profileToForm(conflict));
                setConflict(null);
                mutation.reset();
              }}
            >
              서버 최신 내용 불러오기
            </Button>
          </div>
        </Alert>
      )}
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
            <Form.Text className="text-danger">공개 결제 운영 전 필수</Form.Text>
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group controlId="admin-workshop-business-number">
            <Form.Label>사업자등록번호</Form.Label>
            <Form.Control
              value={form.businessRegistrationNumber}
              placeholder="000-00-00000"
              maxLength={20}
              onChange={(e) => update("businessRegistrationNumber", e.target.value)}
            />
            <Form.Text className="text-danger">공개 결제 운영 전 필수</Form.Text>
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group controlId="admin-workshop-kakao-talk-id">
            <Form.Label>카카오톡 ID</Form.Label>
            <Form.Control
              value={form.kakaoTalkId}
              maxLength={100}
              onChange={(e) => update("kakaoTalkId", e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={8}>
          <Form.Group controlId="admin-workshop-naver-talk-url">
            <Form.Label>네이버톡톡 URL</Form.Label>
            <Form.Control type="url" value={form.naverTalkUrl} maxLength={500} onChange={(e) => update("naverTalkUrl", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group controlId="admin-workshop-representative-name">
            <Form.Label>대표자명</Form.Label>
            <Form.Control
              value={form.representativeName}
              maxLength={100}
              onChange={(e) => update("representativeName", e.target.value)}
            />
            <Form.Text className="text-danger">공개 결제 운영 전 필수</Form.Text>
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group controlId="admin-workshop-email">
            <Form.Label>전자우편주소</Form.Label>
            <Form.Control
              type="email"
              value={form.email}
              maxLength={254}
              onChange={(e) => update("email", e.target.value)}
            />
            <Form.Text className="text-danger">공개 결제 운영 전 필수</Form.Text>
          </Form.Group>
        </Col>
        <Col md={4}>
          <Form.Group controlId="admin-workshop-mail-order-number">
            <Form.Label>통신판매업 신고번호</Form.Label>
            <Form.Control
              value={form.mailOrderRegistrationNumber}
              maxLength={100}
              onChange={(e) => update("mailOrderRegistrationNumber", e.target.value)}
            />
            <Form.Text className="text-danger">공개 결제 운영 전 필수</Form.Text>
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
            <Form.Text className="text-danger">공개 결제 운영 전 필수</Form.Text>
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
        <Col md={12}>
          <Form.Group controlId="admin-workshop-introduction">
            <Form.Label>공방 소개</Form.Label>
            <Form.Control
              as="textarea"
              rows={4}
              value={form.introduction}
              maxLength={2000}
              onChange={(e) => update("introduction", e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="admin-workshop-naver-blog-url">
            <Form.Label>네이버 블로그 URL</Form.Label>
            <Form.Control type="url" value={form.naverBlogUrl} maxLength={500} onChange={(e) => update("naverBlogUrl", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="admin-workshop-instagram-url">
            <Form.Label>인스타그램 URL</Form.Label>
            <Form.Control type="url" value={form.instagramUrl} maxLength={500} onChange={(e) => update("instagramUrl", e.target.value)} />
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="admin-workshop-smart-store-url">
            <Form.Label>스마트스토어 URL</Form.Label>
            <Form.Control type="url" value={form.smartStoreUrl} maxLength={500} onChange={(e) => update("smartStoreUrl", e.target.value)} />
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
          <Button
            className="w-100"
            type="submit"
            disabled={
              !form.name.trim()
              || expectedVersion === null
              || conflict !== null
              || conflictLoading
              || mutation.isPending
            }
          >
            {mutation.isPending ? "저장 중..." : "공방 정보 저장"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
