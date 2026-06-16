<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, NSpace, NH2, useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const email = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)

async function handleRegister() {
  if (!email.value || !password.value || !nickname.value) {
    message.warning('请填写所有字段')
    return
  }
  loading.value = true
  try {
    await auth.registerAction(email.value, password.value, nickname.value)
    router.push('/')
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    message.error(msg || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="display: flex; justify-content: center; align-items: center; height: 100vh; background: #f5f5f5">
    <NCard style="width: 400px">
      <NSpace vertical :size="20">
        <NH2 style="text-align: center; margin: 0">创建账号</NH2>
        <NForm @submit.prevent="handleRegister">
          <NFormItem label="昵称"><NInput v-model:value="nickname" placeholder="你的笔名" /></NFormItem>
          <NFormItem label="邮箱"><NInput v-model:value="email" type="text" /></NFormItem>
          <NFormItem label="密码"><NInput v-model:value="password" type="password" /></NFormItem>
          <NButton type="primary" block :loading="loading" attr-type="submit">注册</NButton>
        </NForm>
        <NButton text @click="router.push('/login')">已有账号？登录</NButton>
      </NSpace>
    </NCard>
  </div>
</template>
