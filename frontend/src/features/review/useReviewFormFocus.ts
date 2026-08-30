import { useCallback, useEffect, useRef } from "react";

export function useReviewFormTriggerFocus(open: boolean) {
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const triggerIdRef = useRef<string | undefined>(undefined);
  const wasOpenRef = useRef(open);

  useEffect(() => {
    const shouldRestoreFocus = wasOpenRef.current && !open;
    wasOpenRef.current = open;
    if (!shouldRestoreFocus) return;

    const frame = window.requestAnimationFrame(() => {
      const replacement = triggerIdRef.current
        ? document.getElementById(triggerIdRef.current)
        : null;
      const trigger = replacement instanceof HTMLButtonElement
        ? replacement
        : triggerRef.current;
      if (trigger?.isConnected) trigger.focus();
    });
    return () => window.cancelAnimationFrame(frame);
  }, [open]);

  return useCallback((trigger: HTMLButtonElement) => {
    triggerRef.current = trigger;
    triggerIdRef.current = trigger.id || undefined;
  }, []);
}
