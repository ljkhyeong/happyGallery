import { Container } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import {
  PRIVACY_POLICY_VERSION,
  TERMS_POLICY_VERSION,
  policyPath,
} from "@/features/policy-consent/policyVersions";

export function TermsPage() {
  const { version = TERMS_POLICY_VERSION } = useParams();

  if (version !== TERMS_POLICY_VERSION) {
    return <UnknownPolicyDocument title="이용약관" />;
  }

  return (
    <Container className="page-container legal-page" style={{ maxWidth: 860 }}>
      <header className="legal-page-header">
        <p className="store-section-kicker">TERMS OF SERVICE</p>
        <h1>이용약관</h1>
        <p>시행일: 2026년 7월 21일</p>
      </header>

      <section>
        <h2>1. 목적과 적용 범위</h2>
        <p>
          이 약관은 해피갤러리에서 제공하는 작품 주문, 클래스 예약, 8회권 구매와 관련 서비스의
          이용 조건을 정합니다. 서비스에서 개별적으로 안내한 가격, 일정, 취소 가능 시각과 상품
          설명은 해당 거래의 조건으로 함께 적용됩니다.
        </p>
      </section>

      <section>
        <h2>2. 회원과 비회원 이용</h2>
        <p>
          회원은 본인 계정과 확인된 휴대폰 번호를 사용해야 합니다. 비회원은 휴대폰 소유 확인과
          결제 완료 후 발급된 접근 정보를 통해 주문·예약을 조회합니다. 타인의 계정, 인증번호 또는
          조회 정보를 사용해서는 안 됩니다.
        </p>
      </section>

      <section>
        <h2>3. 작품 주문과 결제</h2>
        <p>
          주문은 결제 완료 후 공방의 판매 가능 확인을 거쳐 승인됩니다. 오프라인 판매와 재고를
          공유하므로 결제 후에도 품절이 확인되면 주문을 거절하고 전액 환불할 수 있습니다. 고객은
          제작이나 이행이 시작되기 전 승인 대기 주문만 직접 취소할 수 있습니다.
        </p>
        <p>
          전자상거래법상 청약철회 기간과 예외가 우선 적용됩니다. 주문에 따라 개별 제작하는 상품의
          청약철회를 제한하려면, 철회 시 공방에 회복하기 어려운 중대한 손해가 예상된다는 사실을
          거래 전에 별도로 알리고 고객의 전자문서 동의를 받습니다. 이러한 요건을 갖추지 않은 채
          제작 시작만을 이유로 법정 청약철회를 제한하지 않습니다.
        </p>
        <p>
          표시·광고 또는 계약 내용과 다르게 제공된 작품은 법령이 정한 기간 안에 청약철회를 요청할
          수 있습니다. 배송 지연, 픽업 기한과 미수령 처리 기준은 주문 상세에 표시되는 현재 상태와
          안내를 따릅니다.
        </p>
      </section>

      <section>
        <h2>4. 클래스 예약</h2>
        <p>
          클래스 예약은 예약금 결제 또는 유효한 8회권 크레딧 사용으로 확정됩니다. 예약금 환불은
          체험일 00:00(대한민국 표준시) 전까지 가능하며, 현장 잔금을 이미 납부한 예약은 온라인에서
          직접 취소할 수 없습니다.
        </p>
        <p>
          예약 변경은 원칙적으로 체험 전날까지 가능하고, 당일에는 시작 1시간 전까지 가능합니다.
          이후 취소나 결석은 8회권 1회가 소모될 수 있습니다. 실제 처리 가능 여부와 마감 시각은
          예약 상세에 표시되는 서버 기준 안내를 따릅니다.
        </p>
      </section>

      <section>
        <h2>5. 8회권</h2>
        <p>
          8회권은 결제일을 포함해 90일 동안 사용할 수 있습니다. 유효기간이 지나면 남은 횟수는
          소멸하며 환불되지 않습니다. 유효기간 안에는 남은 횟수를 기준으로 정산 환불을 요청할 수
          있고, 환불 시 아직 시작하지 않은 미래 예약은 함께 취소됩니다.
        </p>
      </section>

      <section>
        <h2>6. 서비스 이용 제한과 변경</h2>
        <p>
          부정 결제, 타인 정보 도용, 서비스 장애를 유발하는 행위가 확인되면 이용을 제한할 수
          있습니다. 천재지변, 통신 장애, 외부 결제·메시지 사업자 장애 또는 공방 운영 사정으로
          서비스 제공이 어려운 경우 필요한 범위에서 일정을 변경하거나 거래를 취소하고 안내합니다.
        </p>
      </section>

      <section>
        <h2>7. 법령과 약관의 관계</h2>
        <p>
          이 약관에서 정하지 않은 사항은 전자상거래법, 소비자기본법 등 관계 법령을 따릅니다.
          약관 내용이 관계 법령보다 고객에게 불리한 경우에는 관계 법령을 우선 적용합니다.
        </p>
      </section>

      <section>
        <h2>8. 문의</h2>
        <p>
          운영 주체와 문의 수단은 <Link to="/business-info">사업자 정보</Link>에서 확인할 수 있습니다.
          개인정보 처리에 관한 내용은{" "}
          <Link to={policyPath("privacy", PRIVACY_POLICY_VERSION)}>개인정보처리방침</Link>을 따릅니다.
        </p>
      </section>
    </Container>
  );
}

function UnknownPolicyDocument({ title }: { title: string }) {
  return (
    <Container className="page-container legal-page" style={{ maxWidth: 860 }}>
      <header className="legal-page-header">
        <h1>{title}</h1>
        <p>요청한 버전의 문서를 찾을 수 없습니다.</p>
      </header>
      <Link to={policyPath("terms", TERMS_POLICY_VERSION)}>현재 이용약관 보기</Link>
    </Container>
  );
}
