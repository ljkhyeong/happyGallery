import { Container } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import {
  PRIVACY_POLICY_VERSION,
  policyPath,
} from "@/features/policy-consent/policyVersions";

export function PrivacyPolicyPage() {
  const { version = PRIVACY_POLICY_VERSION } = useParams();

  if (version !== PRIVACY_POLICY_VERSION) {
    return (
      <Container className="page-container legal-page" style={{ maxWidth: 860 }}>
        <header className="legal-page-header">
          <h1>개인정보처리방침</h1>
          <p>요청한 버전의 문서를 찾을 수 없습니다.</p>
        </header>
        <Link to={policyPath("privacy", PRIVACY_POLICY_VERSION)}>
          현재 개인정보처리방침 보기
        </Link>
      </Container>
    );
  }

  return (
    <Container className="page-container legal-page" style={{ maxWidth: 860 }}>
      <header className="legal-page-header">
        <p className="store-section-kicker">PRIVACY POLICY</p>
        <h1>개인정보처리방침</h1>
        <p>시행일: 2026년 7월 21일</p>
      </header>

      <section>
        <h2>1. 처리하는 개인정보</h2>
        <ul>
          <li>회원가입·로그인: 이메일, 이름, 비밀번호의 일방향 해시, 확인된 휴대폰 번호</li>
          <li>소셜 로그인: Google 또는 Naver가 제공하는 계정 식별자, 이메일, 이름</li>
          <li>비회원 주문·예약: 이름, 확인된 휴대폰 번호, 거래 조회용 접근 정보</li>
          <li>주문·배송: 주문 내역, 받는 사람, 연락처, 우편번호와 배송 주소</li>
          <li>결제·환불: 주문번호, 결제 금액, 결제·환불 식별자와 처리 상태</li>
          <li>고객지원: 상품 Q&amp;A, 1:1 문의, 답변과 알림 이력</li>
          <li>서비스 이용 과정: 접속 IP, 요청 식별자, 접속·오류 기록, 세션 정보</li>
        </ul>
      </section>

      <section>
        <h2>2. 처리 목적</h2>
        <p>
          본인 확인과 계정 관리, 작품 주문·배송·픽업, 클래스 예약과 8회권 운영, 결제·환불 처리,
          문의 응대와 거래 알림, 부정 이용 방지와 서비스 장애 대응을 위해 개인정보를 처리합니다.
        </p>
      </section>

      <section>
        <h2>3. 보유와 파기</h2>
        <p>
          회원 정보는 회원 탈퇴 때까지 보유하며, 탈퇴가 완료되면 계정 개인정보와 로그인 수단을
          삭제하거나 익명화합니다. 다만 전자상거래법 등 관계 법령에 따라 다음 기록은 해당 기간
          동안 거래 처리 목적과 분리해 보관합니다.
        </p>
        <ul>
          <li>계약 또는 청약철회 등에 관한 기록: 5년</li>
          <li>대금결제 및 재화·용역 공급에 관한 기록: 5년</li>
          <li>소비자 불만 또는 분쟁처리에 관한 기록: 3년</li>
          <li>표시·광고에 관한 기록: 6개월</li>
        </ul>
        <p>
          완료된 결제 시도의 민감한 요청 정보와 비회원 접근 정보는 30일 뒤 제거합니다. 휴대폰
          인증 정보는 만료 후 1일이 지나면 삭제하며, 로그인 전 장바구니 병합 요청 기록은 7일 뒤
          삭제합니다. 발송 완료 또는 최종 실패로 종결된 알림과 발송 채널 감사 기록은 180일 뒤 삭제합니다.
        </p>
        <p>
          보유 목적과 기간이 끝난 개인정보는 복구하기 어려운 방식으로 삭제합니다. 다른 법령상
          보존 의무나 진행 중인 분쟁이 있으면 해당 정보만 분리 보관하고 그 목적 외로 이용하지
          않으며, 보존 사유가 끝나면 지체 없이 파기합니다.
        </p>
      </section>

      <section>
        <h2>4. 제3자 제공</h2>
        <p>
          해피갤러리에서는 개인정보를 판매하지 않으며, 정보주체의 동의 또는 법령상 근거 없이 제3자에게
          제공하지 않습니다. 결제, 메시지 발송과 소셜 로그인을 위해 필요한 처리는 아래 위탁·외부
          서비스 범위에서만 이루어집니다.
        </p>
      </section>

      <section>
        <h2>5. 처리 위탁과 외부 서비스</h2>
        <p>
          결제 승인·취소에는 Toss Payments, 알림과 휴대폰 인증 메시지 발송에는 NHN Cloud를
          사용합니다. 고객이 선택한 소셜 로그인에는 Google 또는 Naver를 사용하며, 오류 분석은
          운영 설정에서 활성화한 경우 Sentry를 사용합니다. 각 서비스에는 주문번호·결제금액,
          수신 전화번호·메시지 내용, 소셜 계정 식별자 또는 오류 진단 정보처럼 해당 처리에 필요한
          최소 정보만 전달합니다. 카드번호나 계좌 비밀번호는 해피갤러리에서 직접 저장하지 않습니다.
        </p>
      </section>

      <section>
        <h2>6. 자동 수집과 세션</h2>
        <p>
          로그인 유지와 요청 위조 방지를 위해 세션 쿠키와 CSRF 보호 쿠키를 사용합니다. 광고 목적의
          추적 쿠키는 사용하지 않습니다. 브라우저에서 쿠키 저장을 거부할 수 있지만 로그인과 회원용
          거래 기능은 이용하기 어려울 수 있습니다. 접속 IP, 요청 식별자와 오류 기록은 보안·장애
          대응에 필요한 범위에서 자동 생성될 수 있습니다.
        </p>
      </section>

      <section>
        <h2>7. 보호 조치</h2>
        <p>
          비밀번호는 일방향 해시로, 전화번호·배송지·외부 계정 식별자 등 복원이 필요한 민감정보는
          암호화해 저장합니다. 접근 권한을 제한하고 요청 처리율 제한, 접속 기록 보호와 암호화 키
          분리 운영을 적용합니다.
        </p>
      </section>

      <section>
        <h2>8. 이용자의 권리</h2>
        <p>
          회원은 내 정보에서 개인정보를 확인·변경하고 계정 탈퇴를 요청할 수 있습니다. 진행 중인
          거래가 있으면 해당 거래가 종료된 뒤 탈퇴할 수 있습니다. 개인정보에 관한 문의나 정정·삭제
          요청은 개인정보 문의 담당 연락처로 접수할 수 있습니다.
        </p>
      </section>

      <section>
        <h2>9. 개인정보 문의 담당</h2>
        <p>담당 부서: 해피갤러리 개인정보 문의 담당</p>
        <p>전화 010-9635-5608 · 카카오톡 ssim1972 · 네이버톡톡</p>
      </section>

      <section>
        <h2>10. 방침 변경</h2>
        <p>
          처리 항목이나 외부 서비스가 달라지면 시행 전에 이 페이지를 갱신하고 중요한 변경은
          서비스 공지로 알립니다. 운영 주체 정보는 <Link to="/business-info">사업자 정보</Link>에서
          확인할 수 있습니다.
        </p>
      </section>
    </Container>
  );
}
