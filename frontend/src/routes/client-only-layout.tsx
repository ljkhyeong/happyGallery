import { Container } from "react-bootstrap";
import { Outlet } from "react-router";
import { clientOnlyMeta } from "@/shared/seo/metadata";
import { LoadingSpinner } from "@/shared/ui";

export function meta() {
  return clientOnlyMeta();
}

export function clientLoader() {
  return null;
}

clientLoader.hydrate = true as const;

export function HydrateFallback() {
  return (
    <Container className="page-container">
      <LoadingSpinner />
    </Container>
  );
}

export default function ClientOnlyLayout() {
  return <Outlet />;
}
