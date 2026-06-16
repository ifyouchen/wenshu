<script setup lang="ts">
/**
 * 根组件。
 * 统一配置 Naive UI 主题覆盖、全局 Provider 和 Toast 初始化。
 */
import {
  NConfigProvider,
  NDialogProvider,
  NGlobalStyle,
  NMessageProvider,
  NNotificationProvider,
  dateZhCN,
  zhCN,
} from 'naive-ui'
import ToastProvider from '@/components/ToastProvider.vue'
import { useDevice } from '@/composables/useDevice'
import { useTheme } from '@/composables/useTheme'

const { toastPlacement } = useDevice()
const { themeOverrides } = useTheme()
</script>

<template>
  <NConfigProvider
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme-overrides="themeOverrides"
  >
    <NGlobalStyle />
    <NMessageProvider :placement="toastPlacement" :max="5">
      <NDialogProvider>
        <NNotificationProvider>
          <ToastProvider>
            <RouterView />
          </ToastProvider>
        </NNotificationProvider>
      </NDialogProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>
