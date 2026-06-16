<script setup lang="ts">
/** 邮箱验证页面（P8-04）：用户点击邮件中的链接跳转至此页。 */
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NCard, NResult, NButton, NSpin, NSpace } from 'naive-ui'
import client from '@/api/client'
import type { ApiResponse } from '@/api/types'

const router = useRouter()
const route = useRoute()

const status = ref<'verifying' | 'success' | 'error'>('verifying')
const errorMsg = ref('')

onMounted(async () => {
  const token = route.query.token as string
  if (!token) {
    status.value = 'error'
    errorMsg.value = '验证链接无效，缺少 token 参数'
    return
  }
  try {
    await client.get<ApiResponse<void>>(`/auth/verify-email?token=${token}`)
    status.value = 'success'
    setTimeout(() => router.push('/'), 2000)
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    status.value = 'error'
    errorMsg.value = msg || '验证失败，链接可能已过期'
  }
})
</script>

<template>
  <div class="auth-page">
    <NCard style="width: 400px; text-align: center">
      <NSpin v-if="status === 'verifying'" size="large" description="正在验证邮箱…" />
      <NResult
        v-else-if="status === 'success'"
        status="success"
        title="邮箱验证成功！"
        description="正在跳转到工作台…"
      >
        <template #footer>
          <NButton type="primary" @click="router.push('/')">立即进入</NButton>
        </template>
      </NResult>
      <NResult
        v-else
        status="error"
        title="验证失败"
        :description="errorMsg"
      >
        <template #footer>
          <NSpace justify="center">
            <NButton @click="router.push('/login')">返回登录</NButton>
          </NSpace>
        </template>
      </NResult>
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
