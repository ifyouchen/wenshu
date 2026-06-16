<script setup lang="ts">
/**
 * 根组件（P8-01 / P8-16）。
 * - P8-16：NMessageProvider placement 响应移动端（顶部居中）/ 桌面端（右下角）。
 * - ToastProvider 在 NMessageProvider 内初始化全局 Toast 单例。
 */
import { NConfigProvider, NMessageProvider, NNotificationProvider, NGlobalStyle, zhCN, dateZhCN } from 'naive-ui'
import ToastProvider from '@/components/ToastProvider.vue'
import { useDevice } from '@/composables/useDevice'

const { toastPlacement } = useDevice()
</script>

<template>
  <NConfigProvider :locale="zhCN" :date-locale="dateZhCN">
    <NGlobalStyle />
    <!-- P8-16：placement 动态绑定，移动端顶部居中，桌面端右下角堆叠 -->
    <NMessageProvider :placement="toastPlacement" :max="5">
      <NNotificationProvider>
        <!-- 初始化全局 Toast 单例（P8-16） -->
        <ToastProvider>
          <RouterView />
        </ToastProvider>
      </NNotificationProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>
