<script setup lang="ts">
/** 邮箱验证页面：用户点击邮件中的链接跳转至此页。 */
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NCard, NButton, NSpin, NSpace, NIcon } from 'naive-ui'
import { CheckCircle2, XCircle, ArrowRight } from 'lucide-vue-next'
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
    setTimeout(() => router.push('/'), 2500)
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    status.value = 'error'
    errorMsg.value = msg || '验证失败，链接可能已过期'
  }
})
</script>

<template>
  <div class="auth-page">
    <NCard class="verify-card" :bordered="false">
      <div class="verify-content">
        <NSpin v-if="status === 'verifying'" size="large" description="正在验证邮箱…" />

        <template v-else-if="status === 'success'">
          <div class="verify-icon verify-icon--success">
            <NIcon :component="CheckCircle2" :size="48" />
          </div>
          <h2 class="verify-title">邮箱验证成功</h2>
          <p class="verify-desc">你的邮箱已完成验证，正在进入工作台…</p>
          <NButton type="primary" class="verify-btn" @click="router.push('/')">
            <span>立即进入</span>
            <ArrowRight :size="16" />
          </NButton>
        </template>

        <template v-else>
          <div class="verify-icon verify-icon--error">
            <NIcon :component="XCircle" :size="48" />
          </div>
          <h2 class="verify-title">验证失败</h2>
          <p class="verify-desc">{{ errorMsg }}</p>
          <NSpace justify="center">
            <NButton @click="router.push('/login')">返回登录</NButton>
          </NSpace>
        </template>
      </div>
    </NCard>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background: var(--w-bg);
}

.verify-card {
  width: 420px;
  max-width: 100%;
  background: var(--w-bg-secondary) !important;
  border: 1px solid var(--w-border-default) !important;
  border-radius: var(--w-radius-lg) !important;
  padding: 12px;
}

.verify-content {
  text-align: center;
  padding: 32px 16px;
}

.verify-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
}

.verify-icon--success {
  background: var(--w-success-soft);
  color: var(--w-success);
}

.verify-icon--error {
  background: var(--w-danger-soft);
  color: var(--w-danger);
}

.verify-title {
  font-size: var(--w-text-xl);
  font-weight: 600;
  margin-bottom: 8px;
}

.verify-desc {
  color: var(--w-text-secondary);
  font-size: var(--w-text-base);
  margin-bottom: 28px;
  line-height: 1.6;
}

.verify-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  height: auto;
}
</style>
