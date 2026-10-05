import { Camera } from "lucide-react";

/** 대표 사진이 없는 작품은 다른 작품 사진으로 대신하지 않고 준비 중임을 알린다. */
export function ProductMediaPlaceholder({ size = "card" }: { size?: "card" | "detail" }) {
  return (
    <span
      className={`product-media-empty${size === "detail" ? " is-large" : ""}`}
      aria-hidden="true"
    >
      <Camera size={size === "detail" ? 36 : 26} strokeWidth={1.5} />
      <span>사진 준비 중</span>
    </span>
  );
}
