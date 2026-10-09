import { useWorkshopProfile } from "./useWorkshopProfile";
import { ErrorAlert } from "@/shared/ui";

/** 예약·주문·상세 화면의 간단 방문 안내. 소개 문단과 블로그 링크 없이 찾아오는 정보만 보여 준다. */
export function WorkshopVisitInfo() {
  const {
    data: profile,
    error,
    query: { isFetching, refetch },
  } = useWorkshopProfile();
  if (!profile && error) {
    return (
      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching}
      />
    );
  }
  if (!profile) return null;

  const address = [profile.addressLine1, profile.addressLine2].filter(Boolean).join(" ");
  const hasDetails = address || profile.phone || profile.businessHours || profile.parkingInfo
    || profile.introduction || profile.kakaoTalkId || profile.naverTalkUrl
    || profile.mapUrl || profile.naverBlogUrl;
  if (!hasDetails) {
    return error ? (
      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching}
      />
    ) : null;
  }

  return (
    <>
      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching}
      />
      <div className="workshop-visit-info">
        <div>
          <p className="store-section-kicker mb-2">방문 안내</p>
          <h2 className="workshop-visit-name">{profile.name}</h2>
        </div>
        <dl className="workshop-visit-details mb-0">
          {address && <><dt>주소</dt><dd>{address}</dd></>}
          {profile.phone && <><dt>전화</dt><dd><a href={`tel:${profile.phone.replace(/\D/g, "")}`}>{profile.phone}</a></dd></>}
          {profile.kakaoTalkId && <><dt>카카오톡</dt><dd>{profile.kakaoTalkId}</dd></>}
          {profile.naverTalkUrl && (
            <>
              <dt>수업 문의</dt>
              <dd>
                <a href={profile.naverTalkUrl} target="_blank" rel="noreferrer">네이버톡톡 바로가기</a>
              </dd>
            </>
          )}
          {profile.businessHours && <><dt>운영시간</dt><dd>{profile.businessHours}</dd></>}
          {profile.parkingInfo && <><dt>주차</dt><dd>{profile.parkingInfo}</dd></>}
        </dl>
        <div className="workshop-visit-links">
          {profile.mapUrl && (
            <a className="workshop-map-link" href={profile.mapUrl} target="_blank" rel="noreferrer">
              지도에서 보기 <span aria-hidden="true">↗</span>
            </a>
          )}
        </div>
      </div>
    </>
  );
}
