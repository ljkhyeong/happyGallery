import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import tseslint from "typescript-eslint";

const typescriptRecommended = [
  js.configs.recommended,
  ...tseslint.configs.recommended,
];

export default tseslint.config(
  {
    ignores: ["dist", "src/generated"],
  },
  {
    files: ["src/**/*.{ts,tsx}"],
    extends: typescriptRecommended,
    languageOptions: {
      ecmaVersion: "latest",
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
    },
    rules: {
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "error",
    },
  },
  {
    files: ["*.config.ts", "tests/**/*.ts"],
    extends: typescriptRecommended,
    languageOptions: {
      ecmaVersion: "latest",
      globals: globals.node,
    },
  },
);
