import assert from "node:assert/strict";
import test from "node:test";

import {
  CLASS_CATEGORY_OPTIONS,
  isPerfumeClassCategory,
} from "../../src/shared/lib/labels.ts";
import {
  CUSTOM_CLASS_CATEGORY,
  createClassCategoryValue,
  enterCustomClassCategory,
  formatClassCategory,
  getClassCategorySelection,
  normalizeClassCategoryInput,
  selectClassCategory,
} from "../../src/features/admin-class/classCategories.ts";

test("알려진 수업 종류는 관리자에게 내부 코드 없이 한국어 이름으로 보여 준다", () => {
  assert.equal(formatClassCategory("PERFUME"), "향수");
  assert.equal(formatClassCategory("resin"), "레진아트");
  assert.deepEqual(
    CLASS_CATEGORY_OPTIONS.map(({ code }) => code),
    ["PERFUME", "RESIN", "WOOD", "KNIT", "LEATHER", "UPCYCLING"],
  );
});

test("목록에 없는 수업 종류는 이해하기 쉬운 이름으로 입력하고 이전 코드는 감춘다", () => {
  assert.equal(formatClassCategory("CERAMIC"), "기타 수업");
  assert.equal(formatClassCategory("도자기"), "도자기");
  assert.equal(getClassCategorySelection("CERAMIC"), CUSTOM_CLASS_CATEGORY);
  assert.deepEqual(createClassCategoryValue("CERAMIC"), {
    category: "CERAMIC",
    selection: CUSTOM_CLASS_CATEGORY,
  });
  assert.deepEqual(selectClassCategory(CUSTOM_CLASS_CATEGORY), {
    category: "",
    selection: CUSTOM_CLASS_CATEGORY,
  });
});

test("향수 종류는 대소문자와 바깥 공백에 관계없이 8회권 제외 대상으로 판단한다", () => {
  assert.equal(normalizeClassCategoryInput("향수"), "PERFUME");
  assert.equal(normalizeClassCategoryInput(" perfume "), "PERFUME");
  assert.equal(getClassCategorySelection("향수"), "PERFUME");
  assert.deepEqual(enterCustomClassCategory("향수"), {
    category: "PERFUME",
    selection: "PERFUME",
  });
  assert.equal(isPerfumeClassCategory(" perfume "), true);
  assert.equal(isPerfumeClassCategory("RESIN"), false);
});
