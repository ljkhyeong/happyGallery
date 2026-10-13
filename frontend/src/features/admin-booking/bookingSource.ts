import type { AdminBookingResponse } from "@/generated/api/adminBooking";

/** 예약 접수 경로 이름. 관리자 직접 등록은 WEB을 제외한 경로만 고른다. */
export const BOOKING_SOURCE_LABEL: Record<AdminBookingResponse["source"], string> = {
  WEB: "웹",
  PHONE: "전화",
  NAVER_TALK: "네이버톡톡",
  KAKAO: "카카오톡",
  VISIT: "방문",
};
