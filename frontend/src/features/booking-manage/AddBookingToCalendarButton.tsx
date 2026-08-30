import { Button } from "react-bootstrap";
import { CalendarPlus } from "lucide-react";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";

interface Props {
  className: string;
  startAt: string;
  endAt: string;
}

function toUtcIcsDateTime(value: string): string | null {
  const match = value.match(
    /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/,
  );
  if (!match) return null;

  const [, year, month, day, hour, minute, second = "00"] = match;
  const utc = new Date(Date.UTC(
    Number(year),
    Number(month) - 1,
    Number(day),
    Number(hour) - 9,
    Number(minute),
    Number(second),
  ));
  return utc.toISOString().replace(/[-:]/g, "").replace(/\.\d{3}Z$/, "Z");
}

function escapeIcsText(value: string): string {
  return value
    .replace(/\\/g, "\\\\")
    .replace(/\r?\n/g, "\\n")
    .replace(/,/g, "\\,")
    .replace(/;/g, "\\;");
}

function stableHash(value: string): string {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16);
}

function foldIcsLine(line: string): string {
  const encoder = new TextEncoder();
  const folded: string[] = [];
  let chunk = "";
  let chunkBytes = 0;
  let firstLine = true;

  for (const character of line) {
    const characterBytes = encoder.encode(character).length;
    const contentLimit = firstLine ? 75 : 74;
    if (chunkBytes + characterBytes > contentLimit) {
      folded.push(firstLine ? chunk : ` ${chunk}`);
      firstLine = false;
      chunk = character;
      chunkBytes = characterBytes;
    } else {
      chunk += character;
      chunkBytes += characterBytes;
    }
  }

  folded.push(firstLine ? chunk : ` ${chunk}`);
  return folded.join("\r\n");
}

export function AddBookingToCalendarButton({ className, startAt, endAt }: Props) {
  const { data: workshop } = useWorkshopProfile();
  const startsAtUtc = toUtcIcsDateTime(startAt);
  const endsAtUtc = toUtcIcsDateTime(endAt);

  if (!startsAtUtc || !endsAtUtc) return null;

  const downloadCalendar = () => {
    const address = workshop
      ? [workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" ")
      : "";
    const uid = stableHash(`${className}|${startAt}|${endAt}`);
    const lines = [
      "BEGIN:VCALENDAR",
      "VERSION:2.0",
      "PRODID:-//HappyGallery//Booking//KO",
      "CALSCALE:GREGORIAN",
      "METHOD:PUBLISH",
      "BEGIN:VEVENT",
      `UID:booking-${uid}@happygallery.local`,
      `DTSTAMP:${new Date().toISOString().replace(/[-:]/g, "").replace(/\.\d{3}Z$/, "Z")}`,
      `DTSTART:${startsAtUtc}`,
      `DTEND:${endsAtUtc}`,
      `SUMMARY:${escapeIcsText(`해피갤러리 ${className}`)}`,
      address ? `LOCATION:${escapeIcsText(address)}` : null,
      workshop?.phone
        ? `DESCRIPTION:${escapeIcsText(`예약한 클래스입니다. 문의: ${workshop.phone}`)}`
        : "DESCRIPTION:예약한 클래스입니다.",
      "END:VEVENT",
      "END:VCALENDAR",
    ].filter((line): line is string => line !== null);
    const blob = new Blob([`\uFEFF${lines.map(foldIcsLine).join("\r\n")}\r\n`], {
      type: "text/calendar;charset=utf-8",
    });
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
