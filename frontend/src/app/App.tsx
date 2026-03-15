import { BrowserRouter, Routes, Route } from "react-router-dom";
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
import { MyOrdersPage } from "@/pages/MyOrdersPage";
import { MyBookingsPage } from "@/pages/MyBookingsPage";
import { MyPassesPage } from "@/pages/MyPassesPage";
import { MyBookingDetailPage } from "@/pages/MyBookingDetailPage";
import { MyOrderDetailPage } from "@/pages/MyOrderDetailPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { CustomerAuthProvider } from "@/features/customer-auth/useCustomerAuth";
import "@/styles/global.scss";

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <CustomerAuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<Layout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/products" element={<ProductListPage />} />
                <Route path="/products/:id" element={<ProductDetailPage />} />
                <Route path="/bookings/new" element={<BookingCreatePage />} />
                <Route path="/guest/bookings" element={<BookingManagePage />} />
                <Route path="/passes/purchase" element={<PassPurchasePage />} />
                <Route path="/orders/new" element={<OrderCreatePage />} />
                <Route path="/guest/orders" element={<OrderDetailPage />} />
                <Route path="/my" element={<MyPage />} />
                <Route path="/my/orders" element={<MyOrdersPage />} />
                <Route path="/my/bookings/:id" element={<MyBookingDetailPage />} />
                <Route path="/my/bookings" element={<MyBookingsPage />} />
                <Route path="/my/orders/:id" element={<MyOrderDetailPage />} />
                <Route path="/my/passes" element={<MyPassesPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/signup" element={<SignupPage />} />
                <Route path="/admin" element={<AdminPage />} />
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </ToastProvider>
      </CustomerAuthProvider>
    </QueryClientProvider>
  );
}
