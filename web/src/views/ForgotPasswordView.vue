<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, NSpace, NH2, NAlert, useMessage } from 'naive-ui'
import * as authApi from '@/api/auth'

const router = useRouter()
const message = useMessage()
const email = ref('')
const loading = ref(false)
const sent = ref(false)

async function handleSubmit() {
  if (!email.value) { message.warning('请输入邮箱'); return }
  loading.value = true
  try {
    await authApi.forgotPassword(email.value)
    sent.value = true
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="display:flex;justify-content:center;align-items:center;height:100vh;background:#f5f5f5">
    <NCard style="width:400px">
      <NSpace vertical :size="20">
        <NH2 style="text-align:center;margin:0">重置密码</NH2>
        <NAlert v-if="sent" type="success">重置邮件已发送，请检查收件箱。</NAlert>
        <NForm v-else @submit.prevent="handleSubmit">
          <NFormItem label="注册邮箱">
            <NInput v-model:value="email" type="text" placeholder="your@email.com" />
          </NFormItem>
          <NButton type="primary" block :loading="loading" attr-type="submit">发送重置邮件</NButton>
        </NForm>
        <NButton text @click="router.push('/login')">返回登录</NButton>
      </NSpace>
    </NCard>
  </div>
</template>
