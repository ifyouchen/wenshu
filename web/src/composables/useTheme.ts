import { computed, ref } from 'vue'

const STORAGE_KEY = 'wenshu-theme'
type ThemeMode = 'dark' | 'light'

const themeMode = ref<ThemeMode>(
  (typeof localStorage !== 'undefined' ? (localStorage.getItem(STORAGE_KEY) as ThemeMode | null) : null) || 'dark',
)

function applyTheme(mode: ThemeMode) {
  themeMode.value = mode
  if (typeof document !== 'undefined') {
    document.documentElement.dataset.theme = mode
    document.documentElement.style.colorScheme = mode
    localStorage.setItem(STORAGE_KEY, mode)
  }
}

applyTheme(themeMode.value)

export function useTheme() {
  const isDark = computed(() => themeMode.value === 'dark')
  const themeIcon = computed(() => (isDark.value ? '☀' : '☾'))

  function setTheme(mode: ThemeMode) {
    applyTheme(mode)
  }

  function toggleTheme() {
    applyTheme(themeMode.value === 'dark' ? 'light' : 'dark')
  }

  return { themeMode, isDark, themeIcon, setTheme, toggleTheme }
}
