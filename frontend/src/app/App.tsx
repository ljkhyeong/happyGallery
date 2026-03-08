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
              <Route path="/bookings/manage" element={<BookingManagePage />} />
              <Route path="/admin" element={<AdminPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </QueryClientProvider>
  );
}
