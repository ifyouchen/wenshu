<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { CircleCheck, CircleX, Loader2 } from 'lucide-vue-next'
import client from '@/api/client'

const route = useRoute()
const status = ref<'loading' | 'success' | 'error'>('loading')
const message = ref('正在验证邮箱，请稍候')

onMounted(async () => {
  const token = String(route.query.token || '')
  if (!token) {
    status.value = 'error'
    message.value = '验证链接缺少 token'
    return
  }
  try {
    await client.get(`/auth/verify-email?token=${encodeURIComponent(token)}`)
    status.value = 'success'
    message.value = '邮箱验证完成，现在可以登录文枢'
  } catch (error) {
    status.value = 'error'
    message.value =
      (error as { response?: { data?: { message?: string } } }).response?.data?.message || '邮箱验证失败或链接已过期'
  }
})
</script>

<template>
  <main class="ws-auth">
    <section class="ws-auth__card ws-auth__card--center">
      <Loader2 v-if="status === 'loading'" class="ws-spin" :size="40" />
      <CircleCheck v-else-if="status === 'success'" class="ws-state-icon success" :size="44" />
      <CircleX v-else class="ws-state-icon danger" :size="44" />
      <h1>邮箱验证</h1>
      <p>{{ message }}</p>
      <RouterLink class="ws-button ws-button--primary" to="/login">去登录</RouterLink>
    </section>
  </main>
</template>
