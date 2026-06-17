<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { BarChart3, BookOpen, Clapperboard, Edit3, LogOut, Moon, PenLine, Settings, Sun, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'

const auth = useAuthStore()
const router = useRouter()
const { isDark, toggleTheme } = useTheme()

const userName = computed(() => auth.user?.nickname || auth.user?.email || '创作者')
const navItems = [
  { label: '工作台', to: '/', icon: BookOpen },
  { label: '写小说', to: '/write', icon: PenLine },
  { label: '改小说', to: '/rewrite', icon: Edit3 },
  { label: '改剧本', to: '/script-flow', icon: Clapperboard },
  { label: '统计', to: '/stats', icon: BarChart3 },
  { label: '设置', to: '/settings', icon: Settings },
]

onMounted(async () => {
  if (!auth.user) await auth.fetchUser()
})

async function logout() {
  await auth.logoutAction()
  localStorage.removeItem('wenshu-identity-picked')
  router.push('/login')
}
</script>

<template>
  <div class="ws-app">
    <header class="ws-topbar">
      <RouterLink to="/" class="ws-brand">
        <span class="ws-brand__mark">文</span>
        <span>
          <strong>文枢</strong>
          <small>创作工作台</small>
        </span>
      </RouterLink>

      <nav class="ws-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="ws-nav__item"
          active-class="is-active"
        >
          <component :is="item.icon" :size="16" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="ws-topbar__actions">
        <button class="ws-icon-button" type="button" title="切换主题" @click="toggleTheme()">
          <Sun v-if="isDark" :size="18" />
          <Moon v-else :size="18" />
        </button>
        <RouterLink to="/settings" class="ws-user">
          <UserRound :size="17" />
          <span>{{ userName }}</span>
        </RouterLink>
        <button class="ws-icon-button" type="button" title="退出登录" @click="logout">
          <LogOut :size="18" />
        </button>
      </div>
    </header>

    <main class="ws-app__content">
      <RouterView />
    </main>
  </div>
</template>
