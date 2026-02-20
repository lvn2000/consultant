import js from "@eslint/js";
import tsParser from "@typescript-eslint/parser";
import tseslint from "typescript-eslint";

export default [
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2021,
      sourceType: "module",
      globals: {
        // Browser globals
        window: "readonly",
        document: "readonly",
        navigator: "readonly",
        console: "readonly",
        // Node globals
        process: "readonly",
        __dirname: "readonly",
        __filename: "readonly",
        Buffer: "readonly",
        global: "readonly",
        module: "readonly",
        require: "readonly",
        exports: "readonly",
        // ES2021
        Promise: "readonly",
        Symbol: "readonly",
        Set: "readonly",
        Map: "readonly",
        WeakMap: "readonly",
        WeakSet: "readonly",
      },
    },
    rules: {
      // Add custom rules here
    },
  },
  {
    ignores: [
      "node_modules/**",
      "dist/**",
      ".nuxt/**",
      ".output/**",
      ".cache/**",
      "coverage/**",
    ],
  },
];
