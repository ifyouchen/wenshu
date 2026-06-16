<script setup lang="ts">
import { onUnmounted, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInst, FormRules } from 'naive-ui'
import { NCard, NForm, NFormItem, NInput, NButton, NH2, NAlert, useMessage } from 'naive-ui'
import { User, Mail, KeyRound, Lock, ArrowRight } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { sendRegisterCode } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const sendingCode = ref(false)
const codeCooldown = ref(0)
const registered = ref(false)

let cooldownTimer: number | undefined

const form = reactive({ nickname: '', email: '', verificationCode: '', password: '', confirmPassword: '' })

const rules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur', min: 1, max: 20 }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' },
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
    await auth.registerAction(form.email, form.password, form.nickname, form.verificationCode)
    registered.value = true
    setTimeout(() => router.push('/'), 1500)
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    message.error(msg || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    message.warning('请先输入正确的邮箱')
    return
  }
  sendingCode.value = true
  try {
    await sendRegisterCode(form.email)
    message.success('验证码已发送，请查收邮箱')
    startCooldown()
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    message.error(msg || '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

function startCooldown() {
  codeCooldown.value = 60
  if (cooldownTimer) window.clearInterval(cooldownTimer)
  cooldownTimer = window.setInterval(() => {
    codeCooldown.value -= 1
    if (codeCooldown.value <= 0 && cooldownTimer) {
      window.clearInterval(cooldownTimer)
      cooldownTimer = undefined
    }
  }, 1000)
}

onUnmounted(() => {
  if (cooldownTimer) window.clearInterval(cooldownTimer)
})
</script>

<template>
  <div class="auth-page">
    <section class="auth-copy">
      <div class="brand-mark">文</div>
      <h1>创建你的写作空间</h1>
      <p class="auth-desc">
        注册后即可管理作品、章节、统计和创作素材。验证码用于确认邮箱归属，后续通知也会发送到这里。
      </p>
      <div class="auth-features">
        <span>邮箱验证</span>
        <span>作品云端管理</span>
        <span>写作数据沉淀</span>
      </div>
    </section>

    <NCard class="auth-card" :bordered="false">
      <div class="auth-card-header">
        <NH2 class="auth-title">注册账户</NH2>
        <p class="auth-subtitle">开始你的长篇创作之旅</p>
      </div>

      <NAlert v-if="registered" type="success" title="注册成功" class="auth-success">
        账号已创建，正在进入工作台…
      </NAlert>

      <NForm v-else ref="formRef" :model="form" :rules="rules" @submit.prevent="handleRegister">
        <NFormItem label="昵称（笔名）" path="nickname">
          <NInput v-model:value="form.nickname" placeholder="你的笔名" clearable>
            <template #prefix>
              <User :size="16" class="input-icon" />
            </template>
          </NInput>
        </NFormItem>

        <NFormItem label="邮箱" path="email">
          <div class="code-row">
            <NInput v-model:value="form.email" placeholder="your@email.com" clearable>
              <template #prefix>
                <Mail :size="16" class="input-icon" />
              </template>
            </NInput>
            <NButton
              class="code-button"
              :loading="sendingCode"
              :disabled="codeCooldown > 0"
              @click="handleSendCode"
            >
              {{ codeCooldown > 0 ? `${codeCooldown}s` : '发送验证码' }}
            </NButton>
          </div>
        </NFormItem>

        <NFormItem label="邮箱验证码" path="verificationCode">
          <NInput v-model:value="form.verificationCode" maxlength="6" placeholder="6 位数字验证码">
            <template #prefix>
              <KeyRound :size="16" class="input-icon" />
            </template>
          </NInput>
        </NFormItem>

        <NFormItem label="密码（≥8位）" path="password">
          <NInput v-model:value="form.password" type="password" show-password-on="click">
            <template #prefix>
              <Lock :size="16" class="input-icon" />
            </template>
          </NInput>
        </NFormItem>

        <NFormItem label="确认密码" path="confirmPassword">
          <NInput v-model:value="form.confirmPassword" type="password" show-password-on="click">
            <template #prefix>
              <Lock :size="16" class="input-icon" />
            </template>
          </NInput>
        </NFormItem>

        <NButton type="primary" block :loading="loading" attr-type="submit" class="auth-submit">
          <span>创建账号</span>
          <ArrowRight :size="16" />
        </NButton>
      </NForm>

      <div class="auth-footer">
        <NButton text size="small" @click="router.push('/login')">已有账号？登录</NButton>
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

.code-row {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 10px;
}

.code-button {
  min-width: 120px;
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

  .auth-features {
    justify-content: center;
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

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
