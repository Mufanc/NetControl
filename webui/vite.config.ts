import { defineConfig } from "vite";
import Vue from "@vitejs/plugin-vue";

export default defineConfig({
    base: "./",
    plugins: [Vue()],
    build: {
        emptyOutDir: false,
        outDir: "../module/webroot",
        rollupOptions: {
            input: "management.html",
            output: {
                assetFileNames: "assets/management.[ext]",
                chunkFileNames: "assets/[name].js",
                entryFileNames: "assets/management.js",
            },
        },
        target: "es2022",
    },
});
