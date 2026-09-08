import { Button } from "react-bootstrap";
import { CalendarPlus } from "lucide-react";
import { createBookingCalendar } from "./bookingCalendar";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";

interface Props {
  className: string;
  startAt: string;
  endAt: string;
}

export function AddBookingToCalendarButton({ className, startAt, endAt }: Props) {
  const { data: workshop } = useWorkshopProfile();
  const calendar = createBookingCalendar({
    className,
    startAt,
    endAt,
    location: workshop ? [workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" ") : "",
    phone: workshop?.phone,
  });
  if (!calendar) return null;

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
    <Button type="button" variant="outline-secondary" size="sm" onClick={downloadCalendar}>
      <CalendarPlus size={15} aria-hidden="true" className="me-1" />
      캘린더에 추가
    </Button>
  );
}
