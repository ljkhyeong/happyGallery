import { CLASS_CATEGORY_OPTIONS } from "../../shared/lib/labels.ts";

export const CUSTOM_CLASS_CATEGORY = "__CUSTOM__";

export type ClassCategorySelection =
  | ""
  | (typeof CLASS_CATEGORY_OPTIONS)[number]["code"]
  | typeof CUSTOM_CLASS_CATEGORY;

export interface ClassCategoryValue {
  category: string;
  selection: ClassCategorySelection;
}

export function findClassCategoryOption(category: string) {
  const trimmedCategory = category.trim();
  const normalizedCategory = trimmedCategory.toUpperCase();
  return CLASS_CATEGORY_OPTIONS.find(
    ({ code, label }) => code === normalizedCategory || label === trimmedCategory,
  );
}

export function getClassCategorySelection(category: string): ClassCategorySelection {
  if (!category.trim()) return "";
  return findClassCategoryOption(category)?.code ?? CUSTOM_CLASS_CATEGORY;
}

export function normalizeClassCategoryInput(category: string): string {
  return findClassCategoryOption(category)?.code ?? category.trim();
}

export function createClassCategoryValue(category = ""): ClassCategoryValue {
  const normalizedCategory = normalizeClassCategoryInput(category);
  return {
    category: normalizedCategory,
    selection: getClassCategorySelection(normalizedCategory),
  };
}

export function selectClassCategory(selection: ClassCategorySelection): ClassCategoryValue {
  return {
    category: selection === "" || selection === CUSTOM_CLASS_CATEGORY ? "" : selection,
    selection,
  };
}

export function enterCustomClassCategory(category: string): ClassCategoryValue {
  const normalizedCategory = normalizeClassCategoryInput(category);
  const knownCategory = findClassCategoryOption(normalizedCategory);
  return {
    category: normalizedCategory,
    selection: knownCategory?.code ?? CUSTOM_CLASS_CATEGORY,
  };
}

export function formatClassCategory(category: string): string {
  const option = findClassCategoryOption(category);
  if (option) return option.label;
  const trimmedCategory = category.trim();
  return /[가-힣]/.test(trimmedCategory) ? trimmedCategory : "기타 수업";
}
