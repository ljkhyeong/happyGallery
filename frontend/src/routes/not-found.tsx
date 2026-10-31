import type { Route } from "./+types/not-found";
import { data } from "react-router";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { buildSeoMeta, siteTitle } from "@/shared/seo/metadata";

const TITLE = siteTitle("페이지를 찾을 수 없습니다");
const DESCRIPTION = "요청한 해피갤러리 페이지를 찾을 수 없습니다.";

export function loader() {
  return data(null, { status: 404 });
}

export function meta({ location }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname: location.pathname,
    image: heroWorkshop,
    indexable: false,
  });
}

export default function NotFoundRoute() {
  return <NotFoundPage />;
}
