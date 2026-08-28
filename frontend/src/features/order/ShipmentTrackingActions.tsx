import { useState } from "react";
import { Button } from "react-bootstrap";
import { Check, Copy, ExternalLink } from "lucide-react";
import type { FulfillmentDtoCarrierCode } from "@/generated/api/order";

interface Props {
  carrierCode: FulfillmentDtoCarrierCode;
  trackingNumber: string;
}

function buildOfficialTrackingUrl(
  carrierCode: FulfillmentDtoCarrierCode,
  trackingNumber: string,
): string | null {
  const normalizedTrackingNumber = trackingNumber.replace(/[-\s]/g, "");

  if (
    carrierCode !== "HANJIN"
    || !/^\d{10,14}$/.test(normalizedTrackingNumber)
  ) {
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
  const trackingUrl = buildOfficialTrackingUrl(carrierCode, trackingNumber);

  const copyTrackingNumber = async () => {
    await navigator.clipboard.writeText(trackingNumber);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2_000);
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
          href={trackingUrl}
          target="_blank"
          rel="noreferrer"
          size="sm"
          variant="outline-primary"
        >
          <ExternalLink size={14} aria-hidden="true" className="me-1" />
          한진 배송조회
        </Button>
      )}
    </div>
  );
}
