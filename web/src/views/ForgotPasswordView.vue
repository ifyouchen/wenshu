<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Send } from 'lucide-vue-next'
import { forgotPassword } from '@/api/auth'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const email = ref('')
const loading = ref(false)
const sent = ref(false)
const canSubmit = computed(() => email.value.includes('@') && !loading.value)

function messageOf(error: unknown) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || '发送失败，请稍后再试'
}

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    await forgotPassword(email.value.trim())
    sent.value = true
    toast.success('重置邮件已发送')
  } catch (error) {
    toast.error(messageOf(error))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="ws-auth">
    <section class="ws-auth__card">
      <div class="ws-auth__brand">
        <span class="ws-brand__mark">文</span>
        <div>
          <h1>找回密码</h1>
          <p>{{ sent ? '请前往邮箱继续完成重置。' : '输入注册邮箱，我们会发送重置链接。' }}</p>
        </div>
      </div>
      <form class="ws-form" @submit.prevent="submit">
        <label class="ws-field">
          <span>邮箱</span>
          <input v-model="email" class="ws-input" type="email" autocomplete="email" placeholder="name@example.com">
        </label>
        <button class="ws-button ws-button--primary" type="submit" :disabled="!canSubmit">
          <Send :size="18" />
          <span>{{ loading ? '发送中' : '发送重置邮件' }}</span>
        </button>
      </form>
      <div class="ws-auth__links">
        <RouterLink to="/login">返回登录</RouterLink>
      </div>
    </section>
  </main>
</template>
