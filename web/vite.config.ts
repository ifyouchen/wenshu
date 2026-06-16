/**
 * Vite 构建配置（P8-01 / P8-22）。
 *
 * P8-22 性能边界与代码分割策略：
 *  - TipTap 相关包独立分包（vendor-tiptap），编辑器路由才加载。
 *  - Naive UI / @vicons 独立分包（vendor-naive），UI 库按需加载。
 *  - Pinia 独立分包（vendor-pinia），状态管理与核心 Vue 解耦。
 *  - Vue Router 独立分包（vendor-router），路由库独立缓存。
 *  - 剧本工作台路由已经路由级懒加载，不与编辑器路由合包。
 *  - SnapshotDrawer（diff 逻辑）通过 defineAsyncComponent 组件级懒加载，
 *    仅用户点击"历史"按钮时才下载，移动端通常不触发。
 *  - CommandPalette / KeyboardHelpModal 通过 defineAsyncComponent 懒加载，
 *    只在用户触发（Ctrl+K / ?）时才下载。
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

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
    // P8-22：精细代码分割，减少首屏加载体积
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          // TipTap 编辑器相关（仅编辑器路由加载）
          if (id.includes('@tiptap') || id.includes('prosemirror')) {
            return 'vendor-tiptap'
          }
          // Naive UI 组件库（按需 tree-shake，独立缓存）
          if (id.includes('naive-ui') || id.includes('@vicons')) {
            return 'vendor-naive'
          }
          // Pinia 状态管理（独立缓存，变更频率低）
          if (id.includes('pinia')) {
            return 'vendor-pinia'
          }
          // Vue Router（独立缓存，变更频率极低）
          if (id.includes('vue-router')) {
            return 'vendor-router'
          }
          // VueUse 工具库（P8-14 useWindowSize 等）
          if (id.includes('@vueuse')) {
            return 'vendor-vueuse'
          }
          // Axios HTTP 客户端
          if (id.includes('axios')) {
            return 'vendor-axios'
          }
          // 其余第三方包（Vue 核心等）
          if (id.includes('node_modules')) {
            return 'vendor-core'
          }
        },
      },
    },
    // 调大 chunk 警告阈值：Naive UI 是已知的大包，已独立分包，接受此体积
    chunkSizeWarningLimit: 1500,
  },
})
