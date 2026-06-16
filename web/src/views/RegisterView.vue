<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInst, FormRules } from 'naive-ui'
import { NCard, NForm, NFormItem, NInput, NButton, NSpace, NH2, NAlert, useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const registered = ref(false)

const form = reactive({ nickname: '', email: '', password: '', confirmPassword: '' })

const rules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur', min: 1, max: 20 }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码不少于 8 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value) => value === form.password,
      message: '两次密码不一致',
      trigger: 'blur',
    },
  ],
}

async function handleRegister() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await auth.registerAction(form.email, form.password, form.nickname)
    registered.value = true
    setTimeout(() => router.push('/'), 1500)
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    message.error(msg || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <NCard style="width: 440px">
      <NSpace vertical :size="24">
        <NH2 style="text-align: center; margin: 0">创建账号</NH2>
        <NAlert v-if="registered" type="success" title="注册成功">
          注册成功！验证邮件已发送，正在跳转…
        </NAlert>
        <NForm v-else ref="formRef" :model="form" :rules="rules" @submit.prevent="handleRegister">
          <NFormItem label="昵称（笔名）" path="nickname">
            <NInput v-model:value="form.nickname" placeholder="你的笔名" />
          </NFormItem>
          <NFormItem label="邮箱" path="email">
            <NInput v-model:value="form.email" placeholder="your@email.com" />
          </NFormItem>
          <NFormItem label="密码（≥8位）" path="password">
            <NInput v-model:value="form.password" type="password" show-password-on="click" />
          </NFormItem>
          <NFormItem label="确认密码" path="confirmPassword">
            <NInput v-model:value="form.confirmPassword" type="password" show-password-on="click" />
          </NFormItem>
          <NButton type="primary" block :loading="loading" attr-type="submit" style="margin-top: 8px">
            创建账号
          </NButton>
        </NForm>
        <NButton text size="small" @click="router.push('/login')">已有账号？登录</NButton>
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
