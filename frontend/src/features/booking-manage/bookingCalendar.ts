import ICAL from "ical.js";
import { parseApiDateTime } from "../../shared/lib/format.ts";

// 이어지는 줄의 공백 1바이트를 포함해 75바이트 이내로 저장한다.
ICAL.foldLength = 74;

interface BookingCalendar {
  className: string;
  startAt: string;
  endAt: string;
  location?: string;
  phone?: string | null;
}

function stableHash(value: string): string {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16);
}

export function createBookingCalendar({ className, startAt, endAt, location, phone }: BookingCalendar): string | null {
  const start = parseApiDateTime(startAt);
  const end = parseApiDateTime(endAt);
  if (!Number.isFinite(start) || !Number.isFinite(end)) return null;

  const calendar = new ICAL.Component("vcalendar");
  calendar.updatePropertyWithValue("version", "2.0");
  calendar.updatePropertyWithValue("prodid", "-//HappyGallery//Booking//KO");
  calendar.updatePropertyWithValue("calscale", "GREGORIAN");
  calendar.updatePropertyWithValue("method", "PUBLISH");

  const event = new ICAL.Event();
  event.uid = `booking-${stableHash(`${className}|${startAt}|${endAt}`)}@happygallery.local`;
  event.summary = `해피갤러리 ${className}`;
  event.startDate = ICAL.Time.fromJSDate(new Date(start), true);
  event.endDate = ICAL.Time.fromJSDate(new Date(end), true);
  event.description = phone ? `예약한 클래스입니다. 문의: ${phone}` : "예약한 클래스입니다.";
  if (location) event.location = location;
  event.component.updatePropertyWithValue("dtstamp", ICAL.Time.fromJSDate(new Date(), true));
  calendar.addSubcomponent(event.component);
  return `${calendar.toString()}\r\n`;
}
