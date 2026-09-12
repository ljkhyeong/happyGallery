import { useState } from "react";
import { Button } from "react-bootstrap";
import { Check, Copy, ExternalLink } from "lucide-react";
import type { FulfillmentDtoCarrierCode } from "@/generated/api/order";

interface Props {
  carrierCode: FulfillmentDtoCarrierCode;
  trackingNumber: string;
}

const OFFICIAL_TRACKING_PAGES: Partial<Record<NonNullable<FulfillmentDtoCarrierCode>, { name: string; url: string }>> = {
  CJ_LOGISTICS: { name: "CJ대한통운", url: "https://www.cjlogistics.com/ko/tool/parcel/tracking" },
  LOTTE: { name: "롯데택배", url: "https://www.lottegl.com/mobile/reservation/tracking/index" },
  KOREA_POST: { name: "우체국택배", url: "https://service.epost.go.kr/iservice/usr/trace/usrtrc001k01.jsp" },
};

function buildOfficialTrackingUrl(
  carrierCode: FulfillmentDtoCarrierCode,
  trackingNumber: string,
): string | null {
  const normalizedTrackingNumber = trackingNumber.replace(/[-\s]/g, "");

  if (carrierCode !== "HANJIN") {
    return carrierCode ? OFFICIAL_TRACKING_PAGES[carrierCode]?.url ?? null : null;
  }
  if (!/^\d{10,14}$/.test(normalizedTrackingNumber)) {
    return null;
  }

  const url = new URL("https://www.hanjin.com/kor/CMS/DeliveryMgr/WaybillResult.do");
  url.searchParams.set("mCode", "MN038");
  url.searchParams.set("schLang", "KR");
  url.searchParams.set("wblnumText2", normalizedTrackingNumber);
  return url.toString();
}

export function ShipmentTrackingActions({ carrierCode, trackingNumber }: Props) {
  const [copied, setCopied] = useState(false);
  const [copyFailed, setCopyFailed] = useState(false);
  const trackingUrl = buildOfficialTrackingUrl(carrierCode, trackingNumber);
  const carrierName = carrierCode === "HANJIN" ? "한진" : carrierCode && OFFICIAL_TRACKING_PAGES[carrierCode]?.name;

  const copyTrackingNumber = async () => {
    setCopyFailed(false);
    try {
      await navigator.clipboard.writeText(trackingNumber.replace(/[-\s]/g, ""));
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2_000);
    } catch {
      setCopied(false);
      setCopyFailed(true);
    }
  };

  return (
    <div className="d-flex flex-wrap gap-2 mt-2">
      <Button type="button" size="sm" variant="outline-secondary" onClick={copyTrackingNumber}>
        {copied
          ? <Check size={14} aria-hidden="true" className="me-1" />
          : <Copy size={14} aria-hidden="true" className="me-1" />}
        {copied ? "복사됨" : "운송장 복사"}
      </Button>
      {trackingUrl && (
        <Button
          as="a"
          role="link"
          href={trackingUrl}
          target="_blank"
          rel="noreferrer"
          size="sm"
          variant="outline-primary"
        >
          <ExternalLink size={14} aria-hidden="true" className="me-1" />
          {carrierName} 배송조회
        </Button>
      )}
      {trackingUrl && carrierCode !== "HANJIN" && (
        <small className="text-muted-soft w-100">운송장 번호를 복사한 뒤 택배사 사이트에 붙여넣으세요.</small>
      )}
      {copyFailed && (
        <small role="alert" className="text-danger w-100">복사하지 못했습니다. 운송장 번호를 직접 선택해 복사해 주세요.</small>
      )}
    </div>
  );
}
