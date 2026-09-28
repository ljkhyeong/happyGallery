import { reactRouter } from "@react-router/dev/vite";
import { defineConfig } from "vite";
import path from "path";

const apiTarget = process.env.VITE_API_TARGET ?? "http://localhost:8080";

export default defineConfig({
  plugins: [reactRouter()],
  optimizeDeps: {
    // 첫 라우트 로딩 중 재최적화로 브라우저 모듈 URL이 바뀌지 않도록 미리 준비한다.
    include: [
      "@tanstack/react-query", "react-bootstrap", "@sentry/react", "lucide-react",
      "@marsidev/react-turnstile", "ical.js",
    ],
  },
  css: {
    preprocessorOptions: {
      scss: {
        silenceDeprecations: ["import", "global-builtin", "color-functions", "if-function"],
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 3000,
    proxy: {
      "/api": {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
});
