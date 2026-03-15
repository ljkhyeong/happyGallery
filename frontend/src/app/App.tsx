import { BrowserRouter, Navigate, Routes, Route } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "@/shared/api";
import { Layout, ToastProvider } from "@/shared/ui";
import { HomePage } from "@/pages/HomePage";
import { AdminPage } from "@/pages/admin/AdminPage";
import { BookingCreatePage } from "@/pages/BookingCreatePage";
import { BookingManagePage } from "@/pages/BookingManagePage";
import { ProductListPage } from "@/pages/ProductListPage";
import { ProductDetailPage } from "@/pages/ProductDetailPage";
import { PassPurchasePage } from "@/pages/PassPurchasePage";
import { OrderCreatePage } from "@/pages/OrderCreatePage";
import { OrderDetailPage } from "@/pages/OrderDetailPage";
import { LoginPage } from "@/pages/LoginPage";
import { SignupPage } from "@/pages/SignupPage";
import { MyPage } from "@/pages/MyPage";
import { MyBookingDetailPage } from "@/pages/MyBookingDetailPage";
import { MyOrderDetailPage } from "@/pages/MyOrderDetailPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import "@/styles/global.scss";

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route path="/" element={<HomePage />} />
              <Route path="/products" element={<ProductListPage />} />
              <Route path="/products/:id" element={<ProductDetailPage />} />
              <Route path="/bookings/new" element={<BookingCreatePage />} />
              <Route path="/guest/bookings" element={<BookingManagePage />} />
              <Route path="/bookings/manage" element={<Navigate to="/guest/bookings" replace />} />
              <Route path="/passes/purchase" element={<PassPurchasePage />} />
              <Route path="/orders/new" element={<OrderCreatePage />} />
              <Route path="/guest/orders" element={<OrderDetailPage />} />
              <Route path="/orders/detail" element={<Navigate to="/guest/orders" replace />} />
              <Route path="/my" element={<MyPage />} />
              <Route path="/my/bookings/:id" element={<MyBookingDetailPage />} />
              <Route path="/my/orders/:id" element={<MyOrderDetailPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupPage />} />
              <Route path="/admin" element={<AdminPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </QueryClientProvider>
  );
}
