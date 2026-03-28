import { Suspense, lazy, type ReactNode } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Container } from "react-bootstrap";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "@/shared/api";
import { ErrorBoundary, Layout, LoadingSpinner, ToastProvider } from "@/shared/ui";
import { HomePage } from "@/pages/HomePage";
import { BookingCreatePage } from "@/pages/BookingCreatePage";
import { ProductListPage } from "@/pages/ProductListPage";
import { ProductDetailPage } from "@/pages/ProductDetailPage";
import { OrderCreatePage } from "@/pages/OrderCreatePage";
import { CustomerAuthProvider } from "@/features/customer-auth/useCustomerAuth";
import "@/styles/global.scss";

const GuestLookupPage = lazy(() =>
  import("@/pages/GuestLookupPage").then((module) => ({ default: module.GuestLookupPage })),
);
const BookingManagePage = lazy(() =>
  import("@/pages/BookingManagePage").then((module) => ({ default: module.BookingManagePage })),
);
const PassPurchasePage = lazy(() =>
  import("@/pages/PassPurchasePage").then((module) => ({ default: module.PassPurchasePage })),
);
const OrderDetailPage = lazy(() =>
  import("@/pages/OrderDetailPage").then((module) => ({ default: module.OrderDetailPage })),
);
const MyPage = lazy(() =>
  import("@/pages/MyPage").then((module) => ({ default: module.MyPage })),
);
const MyOrdersPage = lazy(() =>
  import("@/pages/MyOrdersPage").then((module) => ({ default: module.MyOrdersPage })),
);
const MyBookingsPage = lazy(() =>
  import("@/pages/MyBookingsPage").then((module) => ({ default: module.MyBookingsPage })),
);
const MyBookingDetailPage = lazy(() =>
  import("@/pages/MyBookingDetailPage").then((module) => ({ default: module.MyBookingDetailPage })),
);
const MyOrderDetailPage = lazy(() =>
  import("@/pages/MyOrderDetailPage").then((module) => ({ default: module.MyOrderDetailPage })),
);
const MyPassesPage = lazy(() =>
  import("@/pages/MyPassesPage").then((module) => ({ default: module.MyPassesPage })),
);
const MyInquiriesPage = lazy(() =>
  import("@/pages/MyInquiriesPage").then((module) => ({ default: module.MyInquiriesPage })),
);
const MyInquiryCreatePage = lazy(() =>
  import("@/pages/MyInquiryCreatePage").then((module) => ({ default: module.MyInquiryCreatePage })),
);
const LoginPage = lazy(() =>
  import("@/pages/LoginPage").then((module) => ({ default: module.LoginPage })),
);
const SignupPage = lazy(() =>
  import("@/pages/SignupPage").then((module) => ({ default: module.SignupPage })),
);
const AdminPage = lazy(() =>
  import("@/pages/admin/AdminPage").then((module) => ({ default: module.AdminPage })),
);
const NoticeDetailPage = lazy(() =>
  import("@/pages/NoticeDetailPage").then((module) => ({ default: module.NoticeDetailPage })),
);
const CartPage = lazy(() =>
  import("@/pages/CartPage").then((module) => ({ default: module.CartPage })),
);
const NotFoundPage = lazy(() =>
  import("@/pages/NotFoundPage").then((module) => ({ default: module.NotFoundPage })),
);

function RouteFallback() {
  return (
    <Container className="page-container">
      <LoadingSpinner />
    </Container>
  );
}

function LazyRoute({ children }: { children: ReactNode }) {
  return <Suspense fallback={<RouteFallback />}>{children}</Suspense>;
}

export function App() {
  return (
    <ErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <CustomerAuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<Layout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/notices/:id" element={<LazyRoute><NoticeDetailPage /></LazyRoute>} />
                <Route path="/products" element={<ProductListPage />} />
                <Route path="/products/:id" element={<ProductDetailPage />} />
                <Route path="/bookings/new" element={<BookingCreatePage />} />
                <Route path="/guest" element={<LazyRoute><GuestLookupPage /></LazyRoute>} />
                <Route path="/guest/bookings" element={<LazyRoute><BookingManagePage /></LazyRoute>} />
                <Route path="/passes/purchase" element={<LazyRoute><PassPurchasePage /></LazyRoute>} />
                <Route path="/cart" element={<LazyRoute><CartPage /></LazyRoute>} />
                <Route path="/orders/new" element={<OrderCreatePage />} />
                <Route path="/guest/orders" element={<LazyRoute><OrderDetailPage /></LazyRoute>} />
                <Route path="/my" element={<LazyRoute><MyPage /></LazyRoute>} />
                <Route path="/my/orders" element={<LazyRoute><MyOrdersPage /></LazyRoute>} />
                <Route path="/my/bookings/:id" element={<LazyRoute><MyBookingDetailPage /></LazyRoute>} />
                <Route path="/my/bookings" element={<LazyRoute><MyBookingsPage /></LazyRoute>} />
                <Route path="/my/orders/:id" element={<LazyRoute><MyOrderDetailPage /></LazyRoute>} />
                <Route path="/my/passes" element={<LazyRoute><MyPassesPage /></LazyRoute>} />
                <Route path="/my/inquiries" element={<LazyRoute><MyInquiriesPage /></LazyRoute>} />
                <Route path="/my/inquiries/new" element={<LazyRoute><MyInquiryCreatePage /></LazyRoute>} />
                <Route path="/login" element={<LazyRoute><LoginPage /></LazyRoute>} />
                <Route path="/signup" element={<LazyRoute><SignupPage /></LazyRoute>} />
                <Route path="/admin" element={<LazyRoute><AdminPage /></LazyRoute>} />
                <Route path="*" element={<LazyRoute><NotFoundPage /></LazyRoute>} />
              </Route>
            </Routes>
          </BrowserRouter>
        </ToastProvider>
      </CustomerAuthProvider>
    </QueryClientProvider>
    </ErrorBoundary>
  );
}
