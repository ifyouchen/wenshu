<script setup lang="ts">
/**
 * 主布局（P8-01 / P8-14 / P8-15）。
 *
 * P8-14 移动端响应式：
 *  - 移动端展示底部导航栏，隐藏顶部侧边导航。
 *  - 底部导航包含：首页 / 写作 / 统计 / 设置。
 *
 * P8-15 命令面板：
 *  - 全局监听 Cmd/Ctrl+K 打开命令面板。
 *  - 注册默认导航命令（首页 / 统计 / 设置）。
 *
 * P8-16 Toast：
 *  - NMessageProvider placement 由 App.vue 统一控制，此处无需额外处理。
 */
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { NLayout } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useQuotaStore } from '@/stores/quota'
import { useCommandPaletteStore } from '@/stores/commandPalette'
import { useDevice } from '@/composables/useDevice'
import CommandPalette from '@/components/CommandPalette.vue'

const auth = useAuthStore()
const quota = useQuotaStore()
const router = useRouter()
const palette = useCommandPaletteStore()
const { isMobile } = useDevice()

onMounted(async () => {
  if (!auth.user) await auth.fetchUser()
  await quota.refresh()
  // 注册全局默认命令（导航类）
  palette.registerCommands([
    {
      id: 'nav:home',
      label: '首页',
      description: '返回作品列表',
      group: '导航',
      icon: '🏠',
      shortcut: '',
      action: () => router.push('/'),
    },
    {
      id: 'nav:stats',
      label: '写作统计',
      description: '查看今日字数、趋势和热力图',
      group: '导航',
      icon: '📊',
      shortcut: '',
      action: () => router.push('/stats'),
    },
    {
      id: 'nav:settings',
      label: '账户设置',
      description: '个人资料、订阅、隐私设置',
      group: '导航',
      icon: '⚙️',
      shortcut: '',
      action: () => router.push('/settings'),
    },
    {
      id: 'user:logout',
      label: '退出登录',
      description: '登出当前账号',
      group: '账户',
      icon: '🚪',
      shortcut: '',
      action: async () => {
        await auth.logoutAction()
        router.push('/login')
      },
    },
  ])
})

onUnmounted(() => {
  palette.unregisterCommands(['nav:home', 'nav:stats', 'nav:settings', 'user:logout'])
})

/** 全局键盘监听：Cmd/Ctrl+K 打开命令面板。 */
function handleGlobalKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    palette.toggle()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown)
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<template>
  <!-- 主布局容器（P8-01）。 -->
  <NLayout style="height: 100vh; overflow: hidden">
    <RouterView />

    <!-- P8-14：移动端底部导航栏（仅移动端显示）。 -->
    <nav v-if="isMobile" class="mobile-bottom-nav">
      <RouterLink to="/" class="mobile-nav-item">
        <span class="mobile-nav-icon">🏠</span>
        <span class="mobile-nav-label">首页</span>
      </RouterLink>
      <RouterLink to="/stats" class="mobile-nav-item">
        <span class="mobile-nav-icon">📊</span>
        <span class="mobile-nav-label">统计</span>
      </RouterLink>
      <button class="mobile-nav-item" @click="palette.toggle()">
        <span class="mobile-nav-icon">⌘</span>
        <span class="mobile-nav-label">命令</span>
      </button>
      <RouterLink to="/settings" class="mobile-nav-item">
        <span class="mobile-nav-icon">⚙️</span>
        <span class="mobile-nav-label">设置</span>
      </RouterLink>
    </nav>

    <!-- P8-15：命令面板（全局，始终挂载，通过 store.visible 控制显隐）。 -->
    <CommandPalette />
  </NLayout>
</template>

<style scoped>
/* ─── 移动端底部导航栏（P8-14） ─── */
.mobile-bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: var(--n-color, #fff);
  border-top: 1px solid rgba(128, 128, 128, 0.15);
  display: flex;
  align-items: stretch;
  z-index: 1000;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

.mobile-nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  text-decoration: none;
  color: inherit;
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  padding: 6px 0;
  transition: background 0.15s;
}

.mobile-nav-item:active {
  background: rgba(128, 128, 128, 0.1);
}

.mobile-nav-item.router-link-active .mobile-nav-icon {
  transform: scale(1.15);
}

.mobile-nav-icon { font-size: 20px; line-height: 1; }
.mobile-nav-label { font-size: 10px; opacity: 0.65; }
</style>
