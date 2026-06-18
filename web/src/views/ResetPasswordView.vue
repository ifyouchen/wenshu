<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { KeyRound } from 'lucide-vue-next'
import { resetPassword } from '@/api/auth'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const token = computed(() => String(route.query.token || ''))
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)

const validationMessage = computed(() => {
  if (!token.value) return '重置链接缺少 token'
  if (newPassword.value.length < 8) return '新密码至少 8 位'
  if (newPassword.value !== confirmPassword.value) return '两次输入的新密码不一致'
  return ''
})

function messageOf(error: unknown) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || '密码重置失败，请稍后再试'
}

async function submit() {
  if (validationMessage.value) {
    toast.warning(validationMessage.value)
    return
  }
  loading.value = true
  try {
    await resetPassword(token.value, newPassword.value)
    toast.success('密码已重置，请使用新密码登录')
    router.push('/login')
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
          <h1>重置密码</h1>
          <p>{{ token ? '设置一个新的登录密码。' : '当前重置链接无效。' }}</p>
        </div>
      </div>

      <form class="ws-form" @submit.prevent="submit">
        <div v-if="!token" class="ws-alert ws-alert--warning">重置链接缺少 token，请重新发起密码重置。</div>
        <label class="ws-field">
          <span>新密码</span>
          <input v-model="newPassword" class="ws-input" type="password" autocomplete="new-password" placeholder="至少 8 位">
        </label>
        <label class="ws-field">
          <span>确认新密码</span>
          <input v-model="confirmPassword" class="ws-input" type="password" autocomplete="new-password" placeholder="再次输入新密码">
        </label>
        <button class="ws-button ws-button--primary" type="submit" :disabled="loading">
          <KeyRound :size="18" />
          <span>{{ loading ? '提交中' : '重置密码' }}</span>
        </button>
      </form>

      <div class="ws-auth__links">
        <RouterLink to="/login">返回登录</RouterLink>
        <RouterLink to="/forgot-password">重新发送邮件</RouterLink>
      </div>
    </section>
  </main>
</template>
