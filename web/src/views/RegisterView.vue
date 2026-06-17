<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { MailCheck, UserPlus } from 'lucide-vue-next'
import { sendRegisterCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const nickname = ref('')
const email = ref('')
const code = ref('')
const password = ref('')
const confirmPassword = ref('')
const sending = ref(false)
const loading = ref(false)

const canSend = computed(() => email.value.includes('@') && !sending.value)
const canSubmit = computed(() =>
  nickname.value.trim().length >= 2 &&
  email.value.includes('@') &&
  code.value.trim().length >= 4 &&
  password.value.length >= 6 &&
  password.value === confirmPassword.value &&
  !loading.value,
)

function messageOf(error: unknown) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || '操作失败，请稍后再试'
}

async function sendCode() {
  if (!canSend.value) return
  sending.value = true
  try {
    await sendRegisterCode(email.value.trim())
    toast.success('验证码已发送，请查收邮箱')
  } catch (error) {
    toast.error(messageOf(error))
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    await auth.registerAction(email.value.trim(), password.value, nickname.value.trim(), code.value.trim())
    router.push('/identity')
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
          <h1>创建文枢账号</h1>
          <p>邮箱验证后即可进入创作工作台。</p>
        </div>
      </div>

      <form class="ws-form" @submit.prevent="submit">
        <label class="ws-field">
          <span>昵称</span>
          <input v-model="nickname" class="ws-input" type="text" autocomplete="nickname" placeholder="你的创作者名">
        </label>
        <label class="ws-field">
          <span>邮箱</span>
          <div class="ws-inline">
            <input v-model="email" class="ws-input" type="email" autocomplete="email" placeholder="name@example.com">
            <button class="ws-button" type="button" :disabled="!canSend" @click="sendCode">
              <MailCheck :size="16" />
              <span>{{ sending ? '发送中' : '验证码' }}</span>
            </button>
          </div>
        </label>
        <label class="ws-field">
          <span>验证码</span>
          <input v-model="code" class="ws-input" type="text" inputmode="numeric" placeholder="邮箱验证码">
        </label>
        <label class="ws-field">
          <span>密码</span>
          <input v-model="password" class="ws-input" type="password" autocomplete="new-password" placeholder="至少 6 位">
        </label>
        <label class="ws-field">
          <span>确认密码</span>
          <input v-model="confirmPassword" class="ws-input" type="password" autocomplete="new-password" placeholder="再次输入密码">
        </label>
        <button class="ws-button ws-button--primary" type="submit" :disabled="!canSubmit">
          <UserPlus :size="18" />
          <span>{{ loading ? '创建中' : '注册并继续' }}</span>
        </button>
      </form>

      <div class="ws-auth__links">
        <RouterLink to="/login">已有账号，去登录</RouterLink>
      </div>
    </section>
  </main>
</template>
