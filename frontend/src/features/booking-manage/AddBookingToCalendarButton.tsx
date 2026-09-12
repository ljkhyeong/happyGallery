import { Button } from "react-bootstrap";
import { CalendarPlus, Download } from "lucide-react";
import { createBookingCalendar, createGoogleCalendarUrl } from "./bookingCalendar";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import type { BookingStatus } from "@/shared/types/booking";

interface Props {
  className: string;
  startAt: string;
  endAt: string;
  status: BookingStatus;
}

export function AddBookingToCalendarButton({ className, startAt, endAt, status }: Props) {
  const { data: workshop } = useWorkshopProfile();
  if (status !== "BOOKED") return null;

  const booking = {
    className,
    startAt,
    endAt,
    location: workshop ? [workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" ") : "",
    phone: workshop?.phone,
  };
  const calendar = createBookingCalendar(booking);
  const googleCalendarUrl = createGoogleCalendarUrl(booking);
  if (!calendar || !googleCalendarUrl) return null;

  const downloadCalendar = () => {
    const blob = new Blob([calendar], { type: "text/calendar;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `해피갤러리-${className.replace(/[\\/:*?"<>|]/g, "-")}.ics`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  };

  return (
    <div>
      <div className="d-flex flex-wrap gap-2">
        <Button as="a" role="link" href={googleCalendarUrl} target="_blank" rel="noopener noreferrer"
          variant="outline-secondary" size="sm">
          <CalendarPlus size={15} aria-hidden="true" className="me-1" />
          Google 캘린더에 추가
        </Button>
        <Button type="button" variant="outline-secondary" size="sm" onClick={downloadCalendar}>
          <Download size={15} aria-hidden="true" className="me-1" />
          캘린더 파일 받기
        </Button>
      </div>
      <p className="small text-muted-soft mt-2 mb-0">
        예약을 변경·취소하면 저장한 일정도 직접 수정해 주세요.
      </p>
    </div>
  );
}
