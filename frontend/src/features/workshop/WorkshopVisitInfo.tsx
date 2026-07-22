import { useWorkshopProfile } from "./useWorkshopProfile";

interface Props {
  compact?: boolean;
}

export function WorkshopVisitInfo({ compact = false }: Props) {
  const { data: profile } = useWorkshopProfile();
  if (!profile) return null;

  const address = [profile.addressLine1, profile.addressLine2].filter(Boolean).join(" ");
  const hasDetails = address || profile.phone || profile.businessHours || profile.parkingInfo
    || profile.introduction || profile.kakaoTalkId || profile.naverTalkUrl
    || profile.mapUrl || profile.naverBlogUrl;
  if (!hasDetails) return null;

  return (
    <div className={compact ? "workshop-visit-info is-compact" : "workshop-visit-info"}>
      <div>
        <p className="store-section-kicker mb-2">VISIT</p>
        <h2 className="workshop-visit-name">{profile.name}</h2>
        {profile.introduction && <p className="workshop-visit-introduction">{profile.introduction}</p>}
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
        {!compact && profile.naverBlogUrl && (
          <a className="workshop-map-link" href={profile.naverBlogUrl} target="_blank" rel="noreferrer">
            공방 기록 보기 <span aria-hidden="true">↗</span>
          </a>
        )}
      </div>
    </div>
  );
}
