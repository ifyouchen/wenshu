import { computed, ref } from 'vue'
import type { GlobalThemeOverrides } from 'naive-ui'

const STORAGE_KEY = 'wenshu-theme'
type ThemeMode = 'dark' | 'light'

const themeMode = ref<ThemeMode>(
  (typeof localStorage !== 'undefined' ? (localStorage.getItem(STORAGE_KEY) as ThemeMode | null) : null) || 'dark',
)

type Palette = {
  bg: string
  surface: string
  elevated: string
  tertiary: string
  text: string
  textSecondary: string
  textTertiary: string
  border: string
  borderSubtle: string
  borderStrong: string
  brand: string
  brandHover: string
  brandPressed: string
  brandSoft: string
  success: string
  warning: string
  error: string
  info: string
}

const palettes: Record<ThemeMode, Palette> = {
  dark: {
    bg: '#101113',
    surface: '#16181b',
    elevated: '#24272d',
    tertiary: '#202328',
    text: '#f0efec',
    textSecondary: '#b4b0aa',
    textTertiary: '#85817a',
    border: 'rgba(255,255,255,0.12)',
    borderSubtle: 'rgba(255,255,255,0.075)',
    borderStrong: 'rgba(255,255,255,0.18)',
    brand: '#6f8197',
    brandHover: '#8392a6',
    brandPressed: '#586b82',
    brandSoft: 'rgba(111,129,151,0.16)',
    success: '#6f947f',
    warning: '#c29a62',
    error: '#c06a62',
    info: '#7c9aaa',
  },
  light: {
    bg: '#f3f1ec',
    surface: '#fbfaf7',
    elevated: '#ffffff',
    tertiary: '#efede8',
    text: '#1d1c1a',
    textSecondary: '#5d5a54',
    textTertiary: '#858077',
    border: 'rgba(38,34,29,0.12)',
    borderSubtle: 'rgba(38,34,29,0.075)',
    borderStrong: 'rgba(38,34,29,0.2)',
    brand: '#53677f',
    brandHover: '#44576f',
    brandPressed: '#36485f',
    brandSoft: 'rgba(83,103,127,0.11)',
    success: '#557d65',
    warning: '#a97936',
    error: '#a94f4b',
    info: '#5b8192',
  },
}

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
    const p = palettes[themeMode.value]

    return {
      common: {
        baseColor: p.bg,
        primaryColor: p.brand,
        primaryColorHover: p.brandHover,
        primaryColorPressed: p.brandPressed,
        primaryColorSuppl: p.brandHover,
        successColor: p.success,
        successColorHover: p.success,
        warningColor: p.warning,
        errorColor: p.error,
        infoColor: p.info,
        textColorBase: p.text,
        textColor1: p.text,
        textColor2: p.textSecondary,
        textColor3: p.textTertiary,
        bodyColor: p.bg,
        cardColor: p.surface,
        modalColor: p.elevated,
        popoverColor: p.elevated,
        tableColor: p.surface,
        tableHeaderColor: p.tertiary,
        tagColor: p.tertiary,
        dividerColor: p.border,
        borderColor: p.border,
        inputColor: p.tertiary,
        inputColorDisabled: p.borderSubtle,
        placeholderColor: p.textTertiary,
        hoverColor: p.brandSoft,
        pressedColor: p.borderSubtle,
        fontFamily: '"Inter", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif',
        fontFamilyMono: '"JetBrains Mono", "SF Mono", "Menlo", "Consolas", monospace',
        borderRadius: '8px',
        borderRadiusSmall: '6px',
      },
      Button: {
        color: p.surface,
        colorHover: p.tertiary,
        colorPressed: p.brandSoft,
        textColor: p.text,
        textColorHover: p.text,
        border: `1px solid ${p.border}`,
        borderHover: `1px solid ${p.borderStrong}`,
      },
      Input: {
        color: p.tertiary,
        colorFocus: p.tertiary,
        textColor: p.text,
        caretColor: p.brand,
        border: `1px solid ${p.border}`,
        borderHover: `1px solid ${p.borderStrong}`,
        borderFocus: `1px solid ${p.brand}`,
        boxShadowFocus: `0 0 0 3px ${p.brandSoft}`,
        placeholderColor: p.textTertiary,
      },
      Card: {
        color: p.surface,
        borderColor: p.border,
      },
      Modal: {
        color: p.elevated,
        borderColor: p.border,
      },
      Drawer: {
        color: p.surface,
        headerBorderColor: p.border,
        footerBorderColor: p.border,
      },
      Tabs: {
        tabTextColor: p.textSecondary,
        tabTextColorActive: p.text,
        tabTextColorHover: p.text,
        barColor: p.brand,
      },
      DataTable: {
        thColor: p.tertiary,
        tdColor: p.surface,
        borderColor: p.borderSubtle,
        thTextColor: p.textSecondary,
        tdTextColor: p.text,
      },
      Tag: {
        color: p.tertiary,
        textColor: p.textSecondary,
        border: `1px solid ${p.borderSubtle}`,
      },
      Dropdown: {
        color: p.elevated,
        border: `1px solid ${p.border}`,
        optionColorHover: p.tertiary,
      },
      Message: {
        color: p.elevated,
        textColor: p.text,
      },
      Notification: {
        color: p.elevated,
        textColor: p.text,
      },
      Progress: {
        fillColor: p.brand,
      },
      Popover: {
        color: p.elevated,
        textColor: p.text,
      },
      Tooltip: {
        color: p.elevated,
        textColor: p.text,
      },
      Slider: {
        fillColor: p.brand,
      },
      Switch: {
        railColorActive: p.brand,
      },
      Checkbox: {
        colorChecked: p.brand,
        borderChecked: `1px solid ${p.brand}`,
      },
      Radio: {
        buttonColorActive: p.brand,
        buttonTextColorActive: '#fff',
      },
      Spin: {
        color: p.brand,
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
