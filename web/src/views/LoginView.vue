<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LogIn } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const email = ref('')
const password = ref('')
const loading = ref(false)
const canSubmit = computed(() => email.value.includes('@') && password.value.length >= 6 && !loading.value)

function messageOf(error: unknown) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || '操作失败，请稍后再试'
}

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    await auth.loginAction(email.value.trim(), password.value)
    if (auth.user?.identityType === 'new_author' && !localStorage.getItem('wenshu-identity-picked')) {
      router.push('/identity')
      return
    }
    router.push(String(route.query.redirect || '/'))
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
          <h1>欢迎回到文枢</h1>
          <p>继续你的长篇、短剧和世界观工程。</p>
        </div>
      </div>

      <form class="ws-form" @submit.prevent="submit">
        <label class="ws-field">
          <span>邮箱</span>
          <input v-model="email" class="ws-input" type="email" autocomplete="email" placeholder="name@example.com">
        </label>
        <label class="ws-field">
          <span>密码</span>
          <input v-model="password" class="ws-input" type="password" autocomplete="current-password" placeholder="至少 6 位">
        </label>
        <button class="ws-button ws-button--primary" type="submit" :disabled="!canSubmit">
          <LogIn :size="18" />
          <span>{{ loading ? '登录中' : '登录' }}</span>
        </button>
      </form>

      <div class="ws-auth__links">
        <RouterLink to="/forgot-password">忘记密码</RouterLink>
        <RouterLink to="/register">注册账号</RouterLink>
      </div>
    </section>
  </main>
</template>
