import { GroupInquirySection } from "@/features/group-inquiry/GroupInquirySection";
import { Button, Container } from "react-bootstrap";
import { Link } from "react-router";
import groupResinClass from "@/assets/happygallery/group-resin-class.jpg";
import upcyclingClass from "@/assets/happygallery/upcycling-class.jpg";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { WorkshopProfileResponse } from "@/generated/api/workshop";
import { telHref } from "@/shared/lib";

const PROCESS = [
  ["1", "문의", "인원, 대상 연령, 장소, 희망 날짜와 관심 있는 공예를 알려 주세요."],
  ["2", "수업 정하기", "참여자와 수업 시간에 맞춰 재료, 작품 난이도, 진행 방법을 정합니다."],
  ["3", "수업", "정한 날짜에 재료와 도구를 챙겨 가서 수업합니다."],
] as const;

export function GroupClassesPage({ initialWorkshop }: { initialWorkshop: WorkshopProfileResponse }) {
  const toast = useToast();
  const {
    data: workshop,
    error,
    query: { isFetching, refetch },
  } = useWorkshopProfile(initialWorkshop);
  const inquiryHref = workshop.naverTalkUrl;
  const kakaoTalkId = workshop.kakaoTalkId;
  const phone = workshop.phone;

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
            <h1>단체·출강 수업</h1>
            <p>학교, 기관, 회사, 동아리 모임에 공예 수업을 엽니다. 인원과 장소에 맞춰 재료와 도구를 준비해 찾아갑니다.</p>
          </div>
        </Container>
      </section>

      <Container className="page-container group-class-page">
        <section className="group-class-intro">
          <div>
            <h2>할 수 있는 수업</h2>
            <p>
              레진아트와 양말목 업사이클링을 비롯해 공방에서 여는 공예 가운데
              참여 연령과 수업 시간에 맞는 과정을 골라 드립니다.
            </p>
          </div>
          <figure>
            <img src={upcyclingClass} alt="해피갤러리 단체 업사이클링 공예 수업" />
          </figure>
        </section>

        <section className="group-class-process">
          <header>
            <h2>진행 순서</h2>
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

        <section id="group-inquiry" className="group-class-inquiry">
          <div>
            <h2>단체 수업 문의</h2>
            <p>인원, 대상 연령, 장소와 희망 날짜를 남겨 주시면 확인한 뒤 연락드립니다.</p>
          </div>
          <div className="group-class-inquiry-actions">
            <GroupInquirySection />
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
              <a className="btn btn-outline-dark btn-lg" href={telHref(phone)}>
                전화 문의 {phone}
              </a>
            )}
            {kakaoTalkId && (
              <Button variant="link" className="p-0 text-start" onClick={() => void copyKakaoTalkId()}>
                카카오톡 ID {kakaoTalkId} 복사
              </Button>
            )}
            <ErrorAlert
              error={error}
              onRetry={() => { void refetch(); }}
              retrying={isFetching}
            />
            {!error && !inquiryHref && !phone && !kakaoTalkId && (
              <span>수업 문의 채널을 준비하고 있습니다.</span>
            )}
            <Link to="/classes" className="store-section-link">개인 클래스 보기</Link>
          </div>
        </section>
      </Container>
    </>
  );
}
