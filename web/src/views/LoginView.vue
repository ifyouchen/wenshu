<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { LogIn } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { getSystemHealth } from '@/api/system'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const email = ref('')
const password = ref('')
const loading = ref(false)
const serviceStatus = ref<'checking' | 'online' | 'offline'>('checking')
const canSubmit = computed(() => !loading.value)
const validationMessage = computed(() => {
  if (!email.value.trim()) return '请输入邮箱'
  if (!email.value.includes('@')) return '请输入有效邮箱'
  if (!password.value) return '请输入密码'
  if (password.value.length < 8) return '密码至少 8 位'
  return ''
})
const passwordHint = computed(() => {
  if (!password.value) return '密码长度需为 8 到 72 位'
  if (password.value.length < 8) return '密码至少 8 位'
  return ''
})

onMounted(checkService)

function messageOf(error: unknown) {
  const responseMessage = (error as { response?: { data?: { message?: string } } }).response?.data?.message
  if (responseMessage) return responseMessage
  return serviceStatus.value === 'offline'
    ? '后端服务暂不可用，请先启动后端服务'
    : '登录失败，请检查账号密码'
}

async function checkService() {
  serviceStatus.value = 'checking'
  try {
    await getSystemHealth()
    serviceStatus.value = 'online'
  } catch {
    serviceStatus.value = 'offline'
  }
}

async function submit() {
  if (validationMessage.value) {
    toast.warning(validationMessage.value)
    return
  }
  loading.value = true
  try {
    await auth.loginAction(email.value.trim(), password.value)
    if (auth.user?.identityType === 'new_author' && !localStorage.getItem('wenshu-identity-picked')) {
      router.push('/identity')
      return
    }
    router.push(String(route.query.redirect || '/'))
  } catch (error) {
    await checkService()
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
        <div
          class="ws-alert"
          :class="{ 'ws-alert--warning': serviceStatus === 'offline' }"
        >
          <span v-if="serviceStatus === 'checking'">正在检查后端服务...</span>
          <span v-else-if="serviceStatus === 'online'">后端服务已连接，可以使用真实账号登录。</span>
          <span v-else>后端服务不可用，请先启动后端服务后再登录。</span>
        </div>
        <label class="ws-field">
          <span>邮箱</span>
          <input v-model="email" class="ws-input" type="email" autocomplete="email" placeholder="name@example.com">
        </label>
        <label class="ws-field">
          <span>密码</span>
          <input v-model="password" class="ws-input" type="password" autocomplete="current-password" placeholder="至少 8 位">
          <small v-if="passwordHint" class="ws-field-hint">{{ passwordHint }}</small>
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
