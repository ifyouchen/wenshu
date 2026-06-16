import { computed, ref } from 'vue'
import type { GlobalThemeOverrides } from 'naive-ui'

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

  function setTheme(mode: ThemeMode) {
    applyTheme(mode)
  }

  function toggleTheme() {
    applyTheme(themeMode.value === 'dark' ? 'light' : 'dark')
  }

  const themeOverrides = computed<GlobalThemeOverrides>(() => {
    const isDarkValue = isDark.value
    const bg = isDarkValue ? '#0c0c0e' : '#f7f5f2'
    const surface = isDarkValue ? '#131316' : '#ffffff'
    const elevated = isDarkValue ? '#212126' : '#ffffff'
    const tertiary = isDarkValue ? '#1a1a1f' : '#f0eeeb'
    const text = isDarkValue ? '#f2f2f0' : '#1a1a1a'
    const textSecondary = isDarkValue ? '#9a9a9e' : '#5a5a5e'
    const textTertiary = isDarkValue ? '#6b6b70' : '#8a8a8e'
    const border = isDarkValue ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'
    const borderSubtle = isDarkValue ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'
    const brand = isDarkValue ? '#5a6e8a' : '#4a5e7a'
    const brandHover = isDarkValue ? '#6b7f9b' : '#3d506b'
    const brandSoft = isDarkValue ? 'rgba(90,110,138,0.14)' : 'rgba(74,94,122,0.1)'
    const success = isDarkValue ? '#5b8a72' : '#4a7a62'

    return {
      common: {
        baseColor: bg,
        primaryColor: brand,
        primaryColorHover: brandHover,
        primaryColorPressed: isDarkValue ? '#4a5e7a' : '#2f4055',
        primaryColorSuppl: brandHover,
        successColor: success,
        successColorHover: isDarkValue ? '#6b9a82' : '#5a8a72',
        warningColor: isDarkValue ? '#b8894a' : '#a07030',
        errorColor: isDarkValue ? '#b8564f' : '#a84040',
        infoColor: isDarkValue ? '#6b8ba0' : '#5a7a8a',
        textColorBase: text,
        textColor1: text,
        textColor2: textSecondary,
        textColor3: textTertiary,
        bodyColor: bg,
        cardColor: surface,
        modalColor: elevated,
        popoverColor: elevated,
        tableColor: surface,
        tableHeaderColor: tertiary,
        tagColor: tertiary,
        dividerColor: border,
        borderColor: border,
        inputColor: tertiary,
        inputColorDisabled: isDarkValue ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)',
        placeholderColor: textTertiary,
        hoverColor: isDarkValue ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.04)',
        pressedColor: isDarkValue ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)',
        fontFamily: "'Inter', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif",
        fontFamilyMono: "'JetBrains Mono', 'SF Mono', 'Menlo', 'Consolas', monospace",
        borderRadius: '10px',
        borderRadiusSmall: '6px',
      },
      Button: {
        color: surface,
        colorHover: isDarkValue ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
        colorPressed: isDarkValue ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)',
        textColor: text,
        textColorHover: text,
        border: `1px solid ${border}`,
        borderHover: `1px solid ${isDarkValue ? 'rgba(255,255,255,0.16)' : 'rgba(0,0,0,0.16)'}`,
      },
      Input: {
        color: tertiary,
        colorFocus: tertiary,
        textColor: text,
        caretColor: brand,
        border: `1px solid ${border}`,
        borderHover: `1px solid ${isDarkValue ? 'rgba(255,255,255,0.16)' : 'rgba(0,0,0,0.16)'}`,
        borderFocus: `1px solid ${brand}`,
        boxShadowFocus: `0 0 0 3px ${brandSoft}`,
        placeholderColor: textTertiary,
      },
      Card: {
        color: surface,
        borderColor: border,
      },
      Modal: {
        color: elevated,
        borderColor: border,
      },
      Drawer: {
        color: surface,
        headerBorderColor: border,
      },
      Tabs: {
        tabTextColor: textSecondary,
        tabTextColorActive: text,
        tabTextColorHover: text,
        barColor: brand,
      },
      Tag: {
        color: tertiary,
        textColor: textSecondary,
        border: `1px solid ${borderSubtle}`,
      },
      Dropdown: {
        color: elevated,
        border: `1px solid ${border}`,
        optionColorHover: isDarkValue ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.04)',
      },
      Message: {
        color: elevated,
        textColor: text,
      },
      Notification: {
        color: elevated,
      },
      Progress: {
        fillColor: brand,
      },
      Popover: {
        color: elevated,
      },
      Tooltip: {
        color: elevated,
        textColor: text,
      },
      Slider: {
        fillColor: brand,
      },
      Switch: {
        railColorActive: brand,
      },
      Checkbox: {
        colorChecked: brand,
        borderChecked: `1px solid ${brand}`,
      },
      Radio: {
        buttonColorActive: brand,
        buttonTextColorActive: '#fff',
      },
      Spin: {
        color: brand,
      },
    }
  })

  return {
    themeMode,
    isDark,
    setTheme,
    toggleTheme,
    themeOverrides,
  }
}
