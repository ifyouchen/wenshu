<script setup lang="ts">
/**
 * ToastProvider（P8-16）。
 * 必须作为 NMessageProvider 的直接子组件，用于初始化全局 Toast 单例。
 * 整个应用只需挂载一次（在 App.vue 中）。
 */
import { useMessage } from 'naive-ui'
import { initGlobalToast } from '@/composables/useToast'

// 在 NMessageProvider 上下文中获取 message API，并注册为全局单例
const message = useMessage()
initGlobalToast(message)
</script>

<template>
  <!-- 仅作为初始化容器，不渲染任何 DOM -->
  <slot />
</template>
