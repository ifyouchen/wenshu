import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 开发时代理到 Spring Boot 后端，避免 CORS
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // P8-22 代码分割：TipTap / 剧本工作台 / diff 独立分包
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (id.includes('@tiptap')) return 'vendor-tiptap'
          if (id.includes('naive-ui') || id.includes('@vicons')) return 'vendor-naive'
          if (id.includes('node_modules')) return 'vendor-core'
        },
      },
    },
  },
})
