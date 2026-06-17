<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInst, FormRules } from 'naive-ui'
import { NCard, NForm, NFormItem, NInput, NButton, NSpace, useMessage, NH2 } from 'naive-ui'
import { Mail, Lock, ArrowRight } from 'lucide-vue-next'
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
    <section class="auth-copy">
      <div class="brand-mark">文</div>
      <h1>文枢</h1>
      <p class="auth-tagline">把故事稳定写下去</p>
      <p class="auth-desc">
        为长篇创作者准备的专业写作工作台。沉淀项目、章节、角色和统计，把每一次推进都留在同一个地方。
      </p>
      <div class="auth-features">
        <span>项目管理</span>
        <span>章节写作</span>
        <span>统计复盘</span>
      </div>
    </section>

    <NCard class="auth-card" :bordered="false">
      <div class="auth-card-header">
        <NH2 class="auth-title">欢迎回来</NH2>
        <p class="auth-subtitle">登录你的创作账户</p>
      </div>

      <NForm ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <NFormItem label="邮箱" path="email">
          <NInput v-model:value="form.email" placeholder="your@email.com" clearable>
            <template #prefix>
              <Mail :size="16" class="input-icon" />
            </template>
          </NInput>
        </NFormItem>
        <NFormItem label="密码" path="password">
          <NInput
            v-model:value="form.password"
            type="password"
            placeholder="请输入密码"
            show-password-on="click"
            @keydown.enter="handleLogin"
          >
            <template #prefix>
              <Lock :size="16" class="input-icon" />
            </template>
          </NInput>
        </NFormItem>
        <NButton type="primary" block :loading="loading" attr-type="submit" class="auth-submit">
          <span>登录</span>
          <ArrowRight :size="16" />
        </NButton>
      </NForm>

      <div class="auth-footer">
        <NSpace justify="space-between" align="center">
          <NButton text size="small" @click="router.push('/register')">还没有账号？注册</NButton>
          <NButton text size="small" @click="router.push('/forgot-password')">忘记密码</NButton>
        </NSpace>
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
  margin-bottom: 8px;
  letter-spacing: 0.04em;
}

.auth-tagline {
  font-size: var(--w-text-xl);
  color: var(--w-brand);
  font-weight: 500;
  margin-bottom: 24px;
}

.auth-desc {
  color: var(--w-text-secondary);
  font-size: var(--w-text-base);
  line-height: 1.8;
  margin-bottom: 32px;
}

.auth-features {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.auth-features span {
  padding: 8px 14px;
  border-radius: var(--w-radius-sm);
  background: var(--w-bg-tertiary);
  border: 1px solid var(--w-border-default);
  color: var(--w-text-secondary);
  font-size: var(--w-text-sm);
}

.auth-card {
  background: var(--w-bg-secondary) !important;
  border: 1px solid var(--w-border-default) !important;
  border-radius: var(--w-radius-lg) !important;
  padding: 8px;
}

.auth-card-header {
  margin-bottom: 28px;
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
}

@media (max-width: 1023px) {
  .auth-page {
    grid-template-columns: 1fr;
    gap: 40px;
    padding: 40px 24px;
    text-align: center;
  }

  .auth-copy {
    max-width: 100%;
  }

  .brand-mark {
    margin: 0 auto 24px;
  }

  .auth-features {
    justify-content: center;
  }

  .auth-card {
    max-width: 420px;
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

  .auth-tagline {
    font-size: var(--w-text-lg);
  }
}
</style>
