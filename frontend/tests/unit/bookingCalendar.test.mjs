import assert from "node:assert/strict";
import { test } from "node:test";
import ICAL from "ical.js";
import { createBookingCalendar } from "../../src/features/booking-manage/bookingCalendar.ts";

const booking = {
  className: "도자기 클래스",
  startAt: "2026-09-07T09:00:00",
  endAt: "2026-09-07T10:30:00",
};

test("예약 파일은 서울 시각을 UTC로 저장하고 일정 식별자를 유지한다", () => {
  const first = createBookingCalendar(booking);
  const second = createBookingCalendar(booking);
  assert.match(first, /DTSTART:20260907T000000Z\r\n/);
  assert.match(first, /DTEND:20260907T013000Z\r\n/);
  assert.equal(first.match(/UID:(.+)/)[1], second.match(/UID:(.+)/)[1]);
  assert.notEqual(first.match(/UID:(.+)/)[1], createBookingCalendar({
    ...booking, startAt: "2026-09-07T09:30:00",
  }).match(/UID:(.+)/)[1]);
});

test("긴 한글과 특수문자를 보존하고 줄마다 UTF-8 75바이트를 넘지 않는다", () => {
  const className = "도자기, 꽃; 그림\\만들기\n".repeat(10) + "🎨";
  const location = "충청북도 충주시 계명대로 161, 2층; 공방\\입구\n안내";
  const calendar = createBookingCalendar({ ...booking, className, location, phone: "043-123-4567" });
  const parsed = new ICAL.Event(new ICAL.Component(ICAL.parse(calendar)).getFirstSubcomponent("vevent"));
  assert.equal(parsed.summary, `해피갤러리 ${className}`);
  assert.equal(parsed.location, location);
  assert.equal(parsed.description, "예약한 클래스입니다. 문의: 043-123-4567");
  for (const line of calendar.split("\r\n")) {
    assert.ok(Buffer.byteLength(line, "utf8") <= 75, line);
  }
});

test("시간이 잘못된 예약은 캘린더 파일을 만들지 않는다", () => {
  assert.equal(createBookingCalendar({ ...booking, startAt: "잘못된 날짜" }), null);
  assert.equal(createBookingCalendar({ ...booking, endAt: "" }), null);
});
