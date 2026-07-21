import { useWorkshopProfile } from "./useWorkshopProfile";

interface Props {
  compact?: boolean;
}

export function WorkshopVisitInfo({ compact = false }: Props) {
  const { data: profile } = useWorkshopProfile();
  if (!profile) return null;

  const address = [profile.addressLine1, profile.addressLine2].filter(Boolean).join(" ");
  const hasDetails = address || profile.phone || profile.businessHours || profile.parkingInfo;
  if (!hasDetails) return null;

  return (
    <div className={compact ? "workshop-visit-info is-compact" : "workshop-visit-info"}>
      <div>
        <p className="store-section-kicker mb-2">VISIT</p>
        <h2 className="workshop-visit-name">{profile.name}</h2>
      </div>
      <dl className="workshop-visit-details mb-0">
        {address && <><dt>주소</dt><dd>{address}</dd></>}
        {profile.phone && <><dt>연락처</dt><dd><a href={`tel:${profile.phone}`}>{profile.phone}</a></dd></>}
        {profile.businessHours && <><dt>운영시간</dt><dd>{profile.businessHours}</dd></>}
        {profile.parkingInfo && <><dt>주차</dt><dd>{profile.parkingInfo}</dd></>}
      </dl>
      {profile.mapUrl && (
        <a className="workshop-map-link" href={profile.mapUrl} target="_blank" rel="noreferrer">
          지도에서 보기 <span aria-hidden="true">↗</span>
        </a>
      )}
    </div>
  );
}
