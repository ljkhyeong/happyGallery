import { Button, Container } from "react-bootstrap";
import { Link } from "react-router";
import groupResinClass from "@/assets/happygallery/group-resin-class.jpg";
import upcyclingClass from "@/assets/happygallery/upcycling-class.jpg";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { useToast } from "@/shared/ui";

const PROCESS = [
  ["01", "수업 문의", "참여 인원과 장소, 희망 일정과 관심 공예를 알려주세요."],
  ["02", "수업 구성", "대상과 시간에 맞춰 재료와 작품 난이도, 진행 방법을 함께 정합니다."],
  ["03", "함께 만들기", "준비한 재료로 각자의 작품을 완성하는 시간을 진행합니다."],
] as const;

export function GroupClassesPage() {
  const toast = useToast();
  const { data: workshop, isLoading } = useWorkshopProfile();
  const inquiryHref = workshop?.naverTalkUrl;
  const kakaoTalkId = workshop?.kakaoTalkId;
  const phone = workshop?.phone;

  const copyKakaoTalkId = async () => {
    if (!kakaoTalkId) return;
    try {
      await navigator.clipboard.writeText(kakaoTalkId);
      toast.show("카카오톡 ID를 복사했습니다.", "success");
    } catch {
      toast.show(`카카오톡에서 ${kakaoTalkId}를 검색해 주세요.`, "warning");
    }
  };

  return (
    <>
      <section className="group-class-hero">
        <img src={groupResinClass} alt="해피갤러리 단체 레진아트 수업" />
        <Container className="group-class-hero-inner">
          <div>
            <p className="store-section-kicker">단체·기관 수업</p>
            <h1>함께 만드는 경험을<br />원하는 곳에서</h1>
            <p>학교, 기관, 모임의 목적과 참여자에 맞는 공예 수업을 함께 준비합니다.</p>
          </div>
        </Container>
      </section>

      <Container className="page-container group-class-page">
        <section className="group-class-intro">
          <div>
            <p className="store-section-kicker">맞춤 수업</p>
            <h2>같은 재료로도 저마다 다른 작품이 완성됩니다</h2>
            <p>
              레진아트와 업사이클링 공예를 비롯해 해피갤러리가 운영하는 여러 공예 중에서
              참여 대상과 수업 시간에 어울리는 과정을 제안합니다.
            </p>
          </div>
          <figure>
            <img src={upcyclingClass} alt="해피갤러리 단체 업사이클링 공예 수업" />
          </figure>
        </section>

        <section className="group-class-process">
          <header>
            <p className="store-section-kicker">진행 방법</p>
            <h2>문의부터 수업까지</h2>
          </header>
          <ol>
            {PROCESS.map(([number, title, description]) => (
              <li key={number}>
                <span>{number}</span>
                <h3>{title}</h3>
                <p>{description}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="group-class-inquiry">
          <div>
            <p className="store-section-kicker">수업 문의</p>
            <h2>원하는 수업을<br />함께 정해 보세요</h2>
            <p>인원, 대상 연령, 장소와 희망 일정을 보내주시면 확인 후 안내합니다.</p>
          </div>
          <div className="group-class-inquiry-actions">
            {inquiryHref && (
              <a
                className="btn btn-dark btn-lg"
                href={inquiryHref}
                target="_blank"
                rel="noreferrer"
              >
                네이버톡톡 문의
              </a>
            )}
            {phone && (
              <a className="btn btn-outline-dark btn-lg" href={`tel:${phone.replace(/\D/g, "")}`}>
                전화 문의 {phone}
              </a>
            )}
            {kakaoTalkId && (
              <Button variant="link" className="p-0 text-start" onClick={() => void copyKakaoTalkId()}>
                카카오톡 ID {kakaoTalkId} 복사
              </Button>
            )}
            {isLoading && <span>수업 문의 채널을 확인하고 있습니다.</span>}
            {!isLoading && !inquiryHref && !phone && !kakaoTalkId && (
              <span>수업 문의 채널을 준비하고 있습니다.</span>
            )}
            <Link to="/classes" className="store-section-link">개인 클래스 보기 →</Link>
          </div>
        </section>
      </Container>
    </>
  );
}
