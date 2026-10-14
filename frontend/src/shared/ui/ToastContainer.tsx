import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { Toast, ToastContainer as BsToastContainer } from "react-bootstrap";

type Variant = "success" | "danger" | "warning" | "info";

interface ToastItem {
  id: number;
  message: string;
  variant: Variant;
}

interface ToastContextValue {
  show: (message: string, variant?: Variant) => void;
}

// 색은 먹색 하나로 쓴다: 완료·정보 알림은 먹색, 확인이 필요한 알림만 노랑, 실패만 빨강이다.
const TOAST_BG: Record<Variant, string> = {
  success: "dark",
  info: "dark",
  warning: "warning",
  danger: "danger",
};

const ToastContext = createContext<ToastContextValue>({ show: () => {} });

export function useToast() {
  return useContext(ToastContext);
}

let nextId = 0;

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const show = useCallback((message: string, variant: Variant = "success") => {
    const id = nextId++;
    setToasts((prev) => [...prev, { id, message, variant }]);
  }, []);

  function remove(id: number) {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }
  const contextValue = useMemo(() => ({ show }), [show]);

  return (
    <ToastContext value={contextValue}>
      {children}
      <BsToastContainer position="top-end" className="p-3" style={{ zIndex: 1080 }}>
        {toasts.map((t) => (
          <Toast
            key={t.id}
            bg={TOAST_BG[t.variant]}
            autohide
            delay={3000}
            onClose={() => remove(t.id)}
          >
            <Toast.Body className={t.variant === "warning" ? "text-dark" : "text-white"}>
              {t.message}
            </Toast.Body>
          </Toast>
        ))}
      </BsToastContainer>
    </ToastContext>
  );
}
