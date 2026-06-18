import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const IDENTITY_PICKED_KEY = 'wenshu-identity-picked'

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
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('@/views/ResetPasswordView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/verify-email',
      name: 'verify-email',
      component: () => import('@/views/VerifyEmailView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/identity',
      name: 'identity',
      component: () => import('@/views/IdentityView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
        { path: 'write', name: 'write-flow', component: () => import('@/views/NovelWriteFlowView.vue') },
        { path: 'rewrite', name: 'rewrite-flow', component: () => import('@/views/NovelRewriteFlowView.vue') },
        { path: 'script-flow', name: 'script-flow', component: () => import('@/views/ScriptFlowView.vue') },
        {
          path: 'projects/:projectId/editor/:chapterId?',
          name: 'editor',
          component: () => import('@/views/EditorView.vue'),
        },
        {
          path: 'projects/:projectId/script/:draftId?',
          name: 'script',
          component: () => import('@/views/ScriptView.vue'),
          meta: { desktopOnly: true },
        },
        { path: 'stats', name: 'stats', component: () => import('@/views/StatsView.vue') },
        {
          path: 'consistency/reports/:reportId',
          name: 'consistency-report',
          component: () => import('@/views/ConsistencyReportView.vue'),
        },
        { path: 'settings', name: 'settings', component: () => import('@/views/SettingsView.vue') },
      ],
    },
  ],
})

function needsIdentitySelection(identityType?: string | null) {
  return !localStorage.getItem(IDENTITY_PICKED_KEY) && (!identityType || identityType === 'new_author')
}

router.beforeEach(async (to, from) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (auth.isLoggedIn && !auth.user) {
    await auth.fetchUser()
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAuth === false && auth.isLoggedIn) {
    return { name: needsIdentitySelection(auth.user?.identityType) ? 'identity' : 'home' }
  }

  if (to.meta.requiresAuth && to.name !== 'identity' && needsIdentitySelection(auth.user?.identityType)) {
    return { name: 'identity', query: { redirect: to.fullPath } }
  }

  if (to.name === 'identity' && auth.user && !needsIdentitySelection(auth.user.identityType)) {
    return { name: 'home' }
  }

  if (to.meta.desktopOnly && typeof window !== 'undefined' && window.innerWidth < 768) {
    return from.name ? false : { name: 'home' }
  }
})

export default router
