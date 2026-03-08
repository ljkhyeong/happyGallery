import { createContext, useCallback, useContext, useState } from "react";
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

  const remove = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext value={{ show }}>
      {children}
      <BsToastContainer position="top-end" className="p-3" style={{ zIndex: 1080 }}>
        {toasts.map((t) => (
          <Toast
            key={t.id}
            bg={t.variant}
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
