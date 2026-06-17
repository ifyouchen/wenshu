<script setup lang="ts">
/**
 * 主布局 - 顶部导航栏版本。
 *
 * 特性：
 * - 顶部毛玻璃导航栏
 * - 全局监听 Cmd/Ctrl+K 打开命令面板
 * - 全局监听 `?` 键打开快捷键参考面板
 * - 移动端底部导航栏
 */
import { computed, h, onMounted, onUnmounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { NButton, NDropdown, NIcon } from 'naive-ui'
import {
  BookOpen,
  BarChart3,
  Settings,
  Command,
  Sun,
  Moon,
  User,
  LogOut,
  Search,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useQuotaStore } from '@/stores/quota'
import { useCommandPaletteStore } from '@/stores/commandPalette'
import { useKeyboardHelpStore } from '@/stores/keyboardHelp'
import { useDevice } from '@/composables/useDevice'
import { useTheme } from '@/composables/useTheme'
import CommandPalette from '@/components/CommandPalette.vue'
import KeyboardHelpModal from '@/components/KeyboardHelpModal.vue'
import UpgradeModal from '@/components/UpgradeModal.vue'
import QuotaTooltip from '@/components/QuotaTooltip.vue'

const auth = useAuthStore()
const quota = useQuotaStore()
const router = useRouter()
const palette = useCommandPaletteStore()
const kbHelp = useKeyboardHelpStore()
const { isMobile, isDesktop } = useDevice()
const { isDark, toggleTheme } = useTheme()

const userName = computed(() => auth.user?.nickname || auth.user?.email || '创作者')

const navItems = [
  { label: '作品', to: '/', icon: BookOpen },
  { label: '统计', to: '/stats', icon: BarChart3 },
  { label: '设置', to: '/settings', icon: Settings },
]

const userOptions = [
  {
    key: 'settings',
    label: '账户设置',
    icon: () => h(NIcon, { component: Settings, size: 14 }),
  },
  {
    key: 'theme',
    label: computed(() => isDark.value ? '切换浅色' : '切换深色'),
    icon: () => h(NIcon, { component: isDark.value ? Sun : Moon, size: 14 }),
  },
  {
    type: 'divider',
    key: 'divider',
  },
  {
    key: 'logout',
    label: '退出登录',
    icon: () => h(NIcon, { component: LogOut, size: 14 }),
  },
]

function handleUserSelect(key: string) {
  if (key === 'settings') {
    router.push('/settings')
  } else if (key === 'theme') {
    toggleTheme()
  } else if (key === 'logout') {
    auth.logoutAction().then(() => router.push('/login'))
  }
}

onMounted(async () => {
  if (!auth.user) await auth.fetchUser()
  await quota.refresh()

  palette.registerCommands([
    {
      id: 'nav:home',
      label: '首页',
      description: '返回作品列表',
      group: '导航',
      icon: 'H',
      shortcut: '',
      action: () => router.push('/'),
    },
    {
      id: 'nav:stats',
      label: '写作统计',
      description: '查看今日字数、趋势和热力图',
      group: '导航',
      icon: 'S',
      shortcut: '',
      action: () => router.push('/stats'),
    },
    {
      id: 'nav:settings',
      label: '账户设置',
      description: '个人资料、订阅、隐私设置',
      group: '导航',
      icon: 'A',
      shortcut: '',
      action: () => router.push('/settings'),
    },
    {
      id: 'help:keyboard',
      label: '快捷键参考',
      description: '查看所有键盘快捷键',
      group: '帮助',
      icon: '?',
      shortcut: '?',
      action: () => kbHelp.open(),
    },
    {
      id: 'user:logout',
      label: '退出登录',
      description: '登出当前账号',
      group: '账户',
      icon: 'Q',
      shortcut: '',
      action: async () => {
        await auth.logoutAction()
        router.push('/login')
      },
    },
  ])
})

onUnmounted(() => {
  palette.unregisterCommands([
    'nav:home', 'nav:stats', 'nav:settings',
    'help:keyboard', 'user:logout',
  ])
})

function handleGlobalKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    palette.toggle()
    return
  }

  const target = e.target as HTMLElement
  const isInput =
    target.tagName === 'INPUT' ||
    target.tagName === 'TEXTAREA' ||
    target.contentEditable === 'true'
  if (e.key === '?' && !isInput && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    kbHelp.toggle()
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
  <div class="app-shell">
    <!-- 顶部导航栏 -->
    <header class="app-topbar">
      <div class="topbar-left">
        <RouterLink to="/" class="brand">
          <span class="brand-mark">文</span>
          <span class="brand-text">
            <strong>文枢</strong>
            <small>创作工作台</small>
          </span>
        </RouterLink>

        <nav v-if="!isMobile" class="top-nav">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="top-nav-item"
            active-class="top-nav-item--active"
          >
            <NIcon :component="item.icon" :size="16" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </div>

      <div class="topbar-right">
        <NButton
          v-if="isDesktop"
          quaternary
          size="small"
          class="command-btn"
          @click="palette.toggle()"
        >
          <template #icon>
            <NIcon :component="Search" :size="14" />
          </template>
          <span>全局命令</span>
          <kbd>Ctrl K</kbd>
        </NButton>

        <QuotaTooltip v-if="!isMobile" />

        <button class="icon-btn" title="切换主题" @click="toggleTheme()">
          <NIcon :component="isDark ? Sun : Moon" :size="18" />
        </button>

        <NDropdown
          trigger="click"
          :options="userOptions"
          @select="handleUserSelect"
        >
          <button class="user-trigger" title="账户菜单">
            <div class="user-avatar">
              <NIcon :component="User" :size="16" />
            </div>
            <span v-if="isDesktop" class="user-name">{{ userName }}</span>
          </button>
        </NDropdown>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="app-content">
      <RouterView />
    </main>

    <!-- 移动端底部导航栏 -->
    <nav v-if="isMobile" class="mobile-bottom-nav">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="mobile-nav-item"
        active-class="mobile-nav-item--active"
      >
        <NIcon :component="item.icon" :size="20" />
        <span>{{ item.label }}</span>
      </RouterLink>
      <button class="mobile-nav-item" @click="palette.toggle()">
        <NIcon :component="Command" :size="20" />
        <span>命令</span>
      </button>
    </nav>

    <!-- 全局组件 -->
    <CommandPalette />
    <KeyboardHelpModal />
    <UpgradeModal />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--w-bg);
}

.app-topbar {
  height: var(--w-topbar-height);
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--w-space-5);
  background: var(--w-bg-toolbar);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border-bottom: 1px solid var(--w-border-subtle);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: var(--w-space-5);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--w-text);
  text-decoration: none;
  min-width: 0;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: var(--w-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--w-brand);
  color: #fff;
  font-family: var(--w-font-serif);
  font-size: 16px;
  font-weight: 700;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-text strong {
  font-family: var(--w-font-serif);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0;
}

.brand-text small {
  font-size: 11px;
  color: var(--w-text-tertiary);
  letter-spacing: 0;
}

.top-nav {
  display: flex;
  align-items: center;
  gap: var(--w-space-1);
}

.top-nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 6px 12px;
  border-radius: var(--w-radius-sm);
  color: var(--w-text-secondary);
  font-size: var(--w-text-sm);
  font-weight: 500;
  transition: all var(--w-transition-base);
}

.top-nav-item:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

.top-nav-item--active {
  color: var(--w-text);
  background: var(--w-brand-soft);
}

.topbar-right {
  gap: var(--w-space-2);
}

.command-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--w-text-secondary) !important;
  border: 1px solid var(--w-border-default) !important;
  background: var(--w-bg-secondary) !important;
}

.command-btn kbd {
  margin-left: 4px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: var(--w-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--w-text-secondary);
  transition: all var(--w-transition-base);
}

.icon-btn:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: var(--w-radius-sm);
  transition: all var(--w-transition-base);
}

.user-trigger:hover {
  background: var(--w-bg-hover);
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--w-radius-sm);
  background: var(--w-brand-soft);
  border: 1px solid var(--w-border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--w-text-secondary);
}

.user-name {
  font-size: var(--w-text-sm);
  color: var(--w-text-secondary);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-content {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  background: var(--w-bg);
}

/* 移动端底部导航栏 */
.mobile-bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--w-mobile-nav-height);
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: var(--w-bg-toolbar);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid var(--w-border-subtle);
  display: flex;
  align-items: stretch;
  z-index: 1000;
}

.mobile-nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--w-text-tertiary);
  font-size: 10px;
  transition: all var(--w-transition-base);
}

.mobile-nav-item--active {
  color: var(--w-brand);
}

.mobile-nav-item:active {
  background: var(--w-bg-hover);
}

@media (max-width: 767px) {
  .app-topbar {
    padding: 0 var(--w-space-3);
  }

  .brand-text small {
    display: none;
  }
}
</style>
