import {
  getGetPublicClassUrl,
  getListPublicClassesUrl,
  type ClassResponse,
} from "@/generated/api/booking";
import {
  getGetPublicEventUrl,
  getListPublicEventsUrl,
  type EventResponse,
} from "@/generated/api/event";
import {
  getGetPublicNoticeUrl,
  getListPublicNoticesUrl,
  type NoticeDetailResponse,
  type NoticeListResponse,
} from "@/generated/api/notice";
import {
  getGetProductUrl,
  getListProductCategoriesUrl,
  getListProductsUrl,
  type ProductDetailResponse,
  type ListProductsParams,
} from "@/generated/api/product";
import {
  getGetWorkshopProfileUrl,
  type WorkshopProfileResponse,
} from "@/generated/api/workshop";
import { isPositiveSafeIntegerString } from "@/shared/lib";

const INTERNAL_API_ORIGIN = process.env.INTERNAL_API_ORIGIN ?? "http://127.0.0.1:8080";
const REQUEST_TIMEOUT_MS = 10_000;

async function serverGet<T>(path: string, requestSignal?: AbortSignal): Promise<T> {
  const timeoutSignal = AbortSignal.timeout(REQUEST_TIMEOUT_MS);
  const signal = requestSignal
    ? AbortSignal.any([requestSignal, timeoutSignal])
    : timeoutSignal;
  const response = await fetch(new URL(path, INTERNAL_API_ORIGIN), {
    method: "GET",
    headers: { Accept: "application/json" },
    signal,
  });

  if (!response.ok) {
    throw new Response(null, {
      status: response.status,
      statusText: response.statusText,
    });
  }

  return response.json() as Promise<T>;
}

export function requirePublicId(value: string | undefined): number {
  if (!isPositiveSafeIntegerString(value)) {
    throw new Response(null, { status: 404, statusText: "Not Found" });
  }
  return Number(value);
}

export function loadProducts(signal?: AbortSignal, filters?: ListProductsParams): Promise<ProductDetailResponse[]> {
  return serverGet(getListProductsUrl(filters), signal);
}

export function loadProduct(id: number, signal?: AbortSignal): Promise<ProductDetailResponse> {
  return serverGet(getGetProductUrl(id), signal);
}

export function loadProductCategories(signal?: AbortSignal): Promise<string[]> {
  return serverGet(getListProductCategoriesUrl(), signal);
}

export function loadClasses(signal?: AbortSignal): Promise<ClassResponse[]> {
  return serverGet(getListPublicClassesUrl(), signal);
}

export function loadClass(id: number, signal?: AbortSignal): Promise<ClassResponse> {
  return serverGet(getGetPublicClassUrl(id), signal);
}

export function loadEvents(signal?: AbortSignal): Promise<EventResponse[]> {
  return serverGet(getListPublicEventsUrl(), signal);
}

export function loadEvent(id: number, signal?: AbortSignal): Promise<EventResponse> {
  return serverGet(getGetPublicEventUrl(id), signal);
}

export function loadNotices(signal?: AbortSignal): Promise<NoticeListResponse[]> {
  return serverGet(getListPublicNoticesUrl(), signal);
}

export function loadNotice(id: number, signal?: AbortSignal): Promise<NoticeDetailResponse> {
  return serverGet(getGetPublicNoticeUrl(id), signal);
}

export function loadWorkshop(signal?: AbortSignal): Promise<WorkshopProfileResponse> {
  return serverGet(getGetWorkshopProfileUrl(), signal);
}
