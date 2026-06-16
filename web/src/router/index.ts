/**
 * 应用路由配置（P8-01 / P8-22）。
 *
 * P8-22 性能优化：
 * - 所有视图均路由级懒加载（动态 import），减少首屏 JS 体积。
 * - 全局导航守卫：移动端访问剧本工作台时重定向回上一页，
 *   防止下载剧本工作台 + TipTap 之外的大型 JS 包。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/** 应用路由配置（P8-01）。 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/views/ForgotPasswordView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/verify-email',
      name: 'verify-email',
      component: () => import('@/views/VerifyEmailView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'projects/:projectId/editor/:chapterId?',
          name: 'editor',
          component: () => import('@/views/EditorView.vue'),
        },
        {
          /**
           * 剧本工作台（P8-13）。
           * P8-22：移动端守卫在下方 beforeEach 中拦截此路由，
           * 防止移动端下载剧本工作台 JS 包。
           */
          path: 'projects/:projectId/script/:draftId?',
          name: 'script',
          component: () => import('@/views/ScriptView.vue'),
          meta: { desktopOnly: true },
        },
        {
          path: 'stats',
          name: 'stats',
          component: () => import('@/views/StatsView.vue'),
        },
        {
          path: 'consistency/reports/:reportId',
          name: 'consistency-report',
          component: () => import('@/views/ConsistencyReportView.vue'),
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/SettingsView.vue'),
        },
      ],
    },
  ],
})

/**
 * 全局导航守卫。
 *
 * 1. 鉴权守卫：未登录自动跳转登录页。
 * 2. P8-22 移动端守卫：移动端（< 768px）访问 desktopOnly 路由时，
 *    重定向到首页并不加载对应 JS 包（动态 import 只在路由真正激活时执行）。
 */
router.beforeEach(async (to, from) => {
  const auth = useAuthStore()

  // 未登录自动跳转
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAuth === false && auth.isLoggedIn) {
    return { name: 'home' }
  }

  // P8-22：移动端守卫 — 阻止下载桌面端专用路由 JS
  if (to.meta.desktopOnly && typeof window !== 'undefined') {
    const isMobileDevice = window.innerWidth < 768
    if (isMobileDevice) {
      // 返回来源页（若有）或首页，不加载目标路由组件
      return from.name ? false : { name: 'home' }
    }
  }
})

export default router
