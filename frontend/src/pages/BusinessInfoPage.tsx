import { Container } from "react-bootstrap";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";

export function BusinessInfoPage() {
  const { data: workshop, isLoading, error } = useWorkshopProfile();

  if (isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (error || !workshop) {
    return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  }

  const address = [workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" ");

  return (
    <Container className="page-container legal-page" style={{ maxWidth: 860 }}>
      <header className="legal-page-header">
        <p className="store-section-kicker">BUSINESS INFORMATION</p>
        <h1>사업자 정보</h1>
      </header>

      {workshop.introduction && <p className="legal-page-lead">{workshop.introduction}</p>}

      <dl className="business-info-list">
        <div><dt>상호</dt><dd>{workshop.name}</dd></div>
        {workshop.businessRegistrationNumber && (
          <div><dt>사업자등록번호</dt><dd>{workshop.businessRegistrationNumber}</dd></div>
        )}
        {workshop.representativeName && (
          <div><dt>대표자</dt><dd>{workshop.representativeName}</dd></div>
        )}
        {workshop.mailOrderRegistrationNumber && (
          <div><dt>통신판매업 신고번호</dt><dd>{workshop.mailOrderRegistrationNumber}</dd></div>
        )}
        {address && <div><dt>사업장 주소</dt><dd>{address}</dd></div>}
        {workshop.phone && (
          <div>
            <dt>전화</dt>
            <dd><a href={`tel:${workshop.phone.replace(/\D/g, "")}`}>{workshop.phone}</a></dd>
          </div>
        )}
        {workshop.email && (
          <div><dt>전자우편주소</dt><dd><a href={`mailto:${workshop.email}`}>{workshop.email}</a></dd></div>
        )}
        {workshop.kakaoTalkId && (
          <div><dt>카카오톡</dt><dd>{workshop.kakaoTalkId}</dd></div>
        )}
        {workshop.naverTalkEnabled && (
          <div><dt>수업 문의</dt><dd>원데이클래스·단체수업 네이버톡톡</dd></div>
        )}
      </dl>
    </Container>
  );
}
