<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, NSpace, useMessage, NH2 } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const message = useMessage()

const email = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!email.value || !password.value) {
    message.warning('请填写邮箱和密码')
    return
  }
  loading.value = true
  try {
    await auth.loginAction(email.value, password.value)
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    message.error(msg || '登录失败，请检查邮箱和密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="display: flex; justify-content: center; align-items: center; height: 100vh; background: #f5f5f5">
    <NCard style="width: 400px">
      <NSpace vertical :size="20">
        <NH2 style="text-align: center; margin: 0">文枢 wenshu</NH2>
        <NForm @submit.prevent="handleLogin">
          <NFormItem label="邮箱">
            <NInput v-model:value="email" type="text" placeholder="your@email.com" />
          </NFormItem>
          <NFormItem label="密码">
            <NInput v-model:value="password" type="password" placeholder="请输入密码" />
          </NFormItem>
          <NButton type="primary" block :loading="loading" attr-type="submit">登录</NButton>
        </NForm>
        <NSpace justify="space-between">
          <NButton text @click="router.push('/register')">还没有账号？注册</NButton>
          <NButton text @click="router.push('/forgot-password')">忘记密码</NButton>
        </NSpace>
      </NSpace>
    </NCard>
  </div>
</template>
