<script setup lang="ts">
import ToastProvider from '@/components/ToastProvider.vue'
import { useTheme } from '@/composables/useTheme'

useTheme()
</script>

<template>
  <ToastProvider>
    <RouterView />
  </ToastProvider>
</template>
