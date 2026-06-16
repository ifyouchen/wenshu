<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, NH2, NAlert, useMessage } from 'naive-ui'
import { Mail, ArrowLeft, Send } from 'lucide-vue-next'
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
  <div class="auth-page">
    <section class="auth-copy">
      <div class="brand-mark">文</div>
      <h1>重置密码</h1>
      <p class="auth-desc">
        输入你的注册邮箱，我们将发送密码重置链接。链接有效期为 1 小时。
      </p>
    </section>

    <NCard class="auth-card" :bordered="false">
      <div class="auth-card-header">
        <NH2 class="auth-title">找回密码</NH2>
        <p class="auth-subtitle">通过邮箱重置你的登录密码</p>
      </div>

      <NAlert v-if="sent" type="success" title="重置邮件已发送" class="auth-success">
        请检查 {{ email }} 的收件箱，点击邮件中的链接设置新密码。
      </NAlert>

      <NForm v-else @submit.prevent="handleSubmit">
        <NFormItem label="注册邮箱">
          <NInput v-model:value="email" type="text" placeholder="your@email.com">
            <template #prefix>
              <Mail :size="16" class="input-icon" />
            </template>
          </NInput>
        </NFormItem>
        <NButton type="primary" block :loading="loading" attr-type="submit" class="auth-submit">
          <Send :size="16" />
          <span>发送重置邮件</span>
        </NButton>
      </NForm>

      <div class="auth-footer">
        <NButton text size="small" @click="router.push('/login')">
          <template #icon>
            <ArrowLeft :size="14" />
          </template>
          返回登录
        </NButton>
      </div>
    </NCard>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 460px;
  gap: 80px;
  align-items: center;
  padding: 40px 80px;
  background: var(--w-bg);
}

.auth-copy {
  max-width: 480px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: var(--w-radius-md);
  background: var(--w-brand);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--w-font-serif);
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 32px;
}

.auth-copy h1 {
  font-family: var(--w-font-serif);
  font-size: var(--w-text-3xl);
  font-weight: 600;
  margin-bottom: 20px;
  letter-spacing: 0.04em;
}

.auth-desc {
  color: var(--w-text-secondary);
  font-size: var(--w-text-base);
  line-height: 1.8;
}

.auth-card {
  background: var(--w-bg-secondary) !important;
  border: 1px solid var(--w-border-default) !important;
  border-radius: var(--w-radius-lg) !important;
  padding: 8px;
}

.auth-card-header {
  margin-bottom: 24px;
}

.auth-title {
  margin: 0 0 6px;
  font-size: var(--w-text-2xl);
  font-weight: 600;
}

.auth-subtitle {
  color: var(--w-text-secondary);
  font-size: var(--w-text-base);
  margin: 0;
}

.auth-success {
  margin-bottom: 20px;
}

.input-icon {
  color: var(--w-text-tertiary);
}

.auth-submit {
  margin-top: 8px;
  height: 44px;
  font-size: var(--w-text-base);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.auth-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--w-border-subtle);
  text-align: center;
}

@media (max-width: 1023px) {
  .auth-page {
    grid-template-columns: 1fr;
    gap: 32px;
    padding: 40px 24px;
    text-align: center;
  }

  .auth-copy {
    max-width: 100%;
  }

  .brand-mark {
    margin: 0 auto 24px;
  }

  .auth-card {
    max-width: 460px;
    width: 100%;
    margin: 0 auto;
  }
}

@media (max-width: 767px) {
  .auth-page {
    padding: 24px 16px;
  }

  .auth-copy h1 {
    font-size: 32px;
  }
}
</style>
