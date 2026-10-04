import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import groupResinClass from "@/assets/happygallery/group-resin-class.jpg";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import upcyclingClass from "@/assets/happygallery/upcycling-class.jpg";

const CATEGORY_IMAGES: Record<string, string> = {
  LEATHER: leatherClass,
  RESIN: groupResinClass,
  UPCYCLING: upcyclingClass,
};

/** 관리자가 수업 사진을 등록하지 않았으면 같은 공예 분야의 공방 사진을 보여 준다. */
export function classImageSrc(bookingClass: { imageUrl?: string | null; category: string }): string {
  if (bookingClass.imageUrl) return bookingClass.imageUrl;
  return CATEGORY_IMAGES[bookingClass.category.trim().toUpperCase()] ?? heroWorkshop;
}
