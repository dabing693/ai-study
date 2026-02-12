import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      "/react/chat": {
        target: "http://localhost:9081",
        changeOrigin: true,
      },
      "/api": {
        target: "http://localhost:9081",
        changeOrigin: true,
      },
      "/news-api": {
        target: "http://localhost:9082/api",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/news-api/, ""),
      },
      "/news-icons": {
        target: "http://localhost:9082",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/news-icons/, "/icons"),
      },
    },
  },
})
