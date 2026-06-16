<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInst, FormRules } from 'naive-ui'
import { NCard, NForm, NFormItem, NInput, NButton, NSpace, useMessage, NH2, NDivider } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const message = useMessage()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)

const form = reactive({ email: '', password: '' })

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不少于 6 位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await auth.loginAction(form.email, form.password)
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
  <div class="auth-page">
    <NCard style="width: 420px">
      <NSpace vertical :size="24">
        <div style="text-align: center">
          <NH2 style="margin: 0 0 4px">文枢 wenshu</NH2>
          <span style="color: #888; font-size: 14px">AI 智能写作工作台</span>
        </div>
        <NForm ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
          <NFormItem label="邮箱" path="email">
            <NInput v-model:value="form.email" placeholder="your@email.com" clearable />
          </NFormItem>
          <NFormItem label="密码" path="password">
            <NInput v-model:value="form.password" type="password" placeholder="请输入密码"
                    show-password-on="click" @keydown.enter="handleLogin" />
          </NFormItem>
          <NButton type="primary" block :loading="loading" attr-type="submit" style="margin-top: 8px">
            登录
          </NButton>
        </NForm>
        <NDivider style="margin: 0" />
        <NSpace justify="space-between">
          <NButton text size="small" @click="router.push('/register')">还没有账号？注册</NButton>
          <NButton text size="small" @click="router.push('/forgot-password')">忘记密码</NButton>
        </NSpace>
      </NSpace>
    </NCard>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef1f5 100%);
}
</style>
