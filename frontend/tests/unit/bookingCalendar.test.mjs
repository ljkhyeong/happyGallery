import assert from "node:assert/strict";
import { test } from "node:test";
import ICAL from "ical.js";
import { createBookingCalendar, createGoogleCalendarUrl } from "../../src/features/booking-manage/bookingCalendar.ts";

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
  for (const create of [createBookingCalendar, createGoogleCalendarUrl]) {
    assert.equal(create({ ...booking, startAt: "잘못된 날짜" }), null);
    assert.equal(create({ ...booking, endAt: "" }), null);
    assert.equal(create({ ...booking, endAt: booking.startAt }), null);
    assert.equal(create({ ...booking, endAt: "2026-09-07T08:59:00" }), null);
  }
});

test("Google 일정 링크는 서울 시각과 공방 정보를 보존하며 개인 식별정보를 포함하지 않는다", () => {
  const className = "도자기 & 꽃+그림 #1";
  const location = "충주시 계명대로 161, 2층 (공방)";
  const url = new URL(createGoogleCalendarUrl({
    ...booking, className, location, phone: "043-123-4567",
    guestPhone: "01012345678", accessToken: "private-booking-token", bookingId: 12,
  }));
  assert.equal(url.origin, "https://calendar.google.com");
  assert.equal(url.pathname, "/calendar/r/eventedit");
  assert.deepEqual(Object.fromEntries(url.searchParams), {
    action: "TEMPLATE", text: `해피갤러리 ${className}`,
    dates: "20260907T000000Z/20260907T013000Z", stz: "Asia/Seoul", etz: "Asia/Seoul",
    details: "예약한 클래스입니다. 문의: 043-123-4567", location,
  });
  assert.equal(url.hash, "");
});

test("Google 일정 링크와 파일은 같은 시각을 표현하고 UTC·날짜 경계도 처리한다", () => {
  const values = { ...booking, startAt: "2026-09-06T15:00:00Z", endAt: "2026-09-07T01:30:00+09:00" };
  const event = new ICAL.Event(new ICAL.Component(ICAL.parse(createBookingCalendar(values))).getFirstSubcomponent("vevent"));
  const url = new URL(createGoogleCalendarUrl(values));
  assert.equal(url.searchParams.get("dates"), "20260906T150000Z/20260906T163000Z");
  assert.equal(url.searchParams.get("dates"), `${event.startDate.toICALString()}/${event.endDate.toICALString()}`);
  assert.equal(url.searchParams.get("location"), "");
});
