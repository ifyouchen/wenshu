<script setup lang="ts">
/**
 * 账户设置页面（P8-20）。
 *
 * 五个分区：
 * - 个人资料：昵称、头像链接、邮箱（只读）
 * - 创作偏好：每日目标字数、身份类型
 * - AI 内容：AI 训练授权开关
 * - 订阅用量：当前套餐 + 当月配额用量（P9-01/P9-02）
 * - 账户安全：修改密码、注销账号
 */
import { ref, onMounted, reactive, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import {
  NLayout, NLayoutContent, NPageHeader, NTabs, NTabPane, NForm, NFormItem,
  NInput, NInputNumber, NButton, NSpace, NSpin, NSwitch, NSelect, NProgress,
  NAlert, NText, NTag, NDivider, NIcon, useMessage, useDialog,
} from 'naive-ui'
import {
  User,
  Pencil,
  Bot,
  Gem,
  Shield,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { getMe, updateProfile, changePassword, setWritingGoal, updateAiConsent, setIdentityType, deleteAccount } from '@/api/user'
import { getCurrentSubscription } from '@/api/subscription'
import type { UserInfo } from '@/api/types'
import type { CurrentSubscriptionInfo } from '@/api/subscription'

const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const user = ref<UserInfo | null>(null)
const subscription = ref<CurrentSubscriptionInfo | null>(null)
const subLoading = ref(false)

// 个人资料表单
const profileForm = reactive({ nickname: '', avatarUrl: '' })
const profileSaving = ref(false)

// 创作偏好表单
const goalForm = reactive({ dailyCharGoal: 2000, identityType: '' })
const goalSaving = ref(false)

const identityOptions = [
  { label: '网文作者', value: 'web_novel_author' },
  { label: '短剧编剧', value: 'short_drama_writer' },
  { label: '新人作者', value: 'new_author' },
]

// AI 授权开关
const aiConsent = ref(false)
const aiConsentSaving = ref(false)

// 修改密码表单
const pwdForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const pwdSaving = ref(false)

// 配额进度
const charUsedPct = computed(() => {
  if (!subscription.value) return 0
  const { usedChars, limitChars } = subscription.value.quota
  return Math.min(Math.round((usedChars / limitChars) * 100), 100)
})
const adaptUsedPct = computed(() => {
  if (!subscription.value) return 0
  const { usedAdaptations, limitAdaptations } = subscription.value.quota
  return Math.min(Math.round((usedAdaptations / limitAdaptations) * 100), 100)
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMe()
    user.value = res.data.data
    profileForm.nickname = user.value.nickname ?? ''
    profileForm.avatarUrl = user.value.avatarUrl ?? ''
    goalForm.dailyCharGoal = (user.value as any).dailyCharGoal ?? 2000
    goalForm.identityType = (user.value as any).identityType ?? ''
    aiConsent.value = (user.value as any).aiTrainingConsent ?? false
  } catch {
    message.error('加载用户信息失败')
  } finally {
    loading.value = false
  }

  subLoading.value = true
  try {
    const res = await getCurrentSubscription()
    subscription.value = res.data.data
  } catch {
    // 静默失败
  } finally {
    subLoading.value = false
  }
})

async function saveProfile() {
  profileSaving.value = true
  try {
    await updateProfile({
      nickname: profileForm.nickname || undefined,
      avatarUrl: profileForm.avatarUrl || undefined,
    })
    message.success('资料已更新')
  } catch {
    message.error('保存失败，请重试')
  } finally {
    profileSaving.value = false
  }
}

async function saveGoalAndIdentity() {
  goalSaving.value = true
  try {
    await setWritingGoal(goalForm.dailyCharGoal)
    if (goalForm.identityType) {
      await setIdentityType(goalForm.identityType)
    }
    message.success('偏好已保存')
  } catch {
    message.error('保存失败，请重试')
  } finally {
    goalSaving.value = false
  }
}

async function handleAiConsentChange(val: boolean) {
  aiConsentSaving.value = true
  try {
    await updateAiConsent(val)
    aiConsent.value = val
    message.success(val ? 'AI 训练授权已开启' : 'AI 训练授权已关闭')
  } catch {
    aiConsent.value = !val
    message.error('设置失败，请重试')
  } finally {
    aiConsentSaving.value = false
  }
}

async function savePassword() {
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    message.error('两次输入的新密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 8) {
    message.error('新密码至少 8 位')
    return
  }
  pwdSaving.value = true
  try {
    await changePassword({ currentPassword: pwdForm.currentPassword, newPassword: pwdForm.newPassword })
    message.success('密码已修改，请重新登录')
    setTimeout(() => auth.logoutAction().then(() => router.push('/login')), 1500)
  } catch {
    message.error('密码修改失败（当前密码错误？）')
  } finally {
    pwdSaving.value = false
  }
}

function handleDeleteAccount() {
  dialog.warning({
    title: '注销账号',
    content: '确定要注销账号吗？账号将被软删除，30 天内可通过恢复令牌撤销。注销后将立即退出所有设备。',
    positiveText: '确认注销',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await deleteAccount()
        const token = res.data.data.restoreToken
        message.info(`账号已注销。恢复令牌（30 天内有效）：${token}`, { duration: 0, closable: true })
        setTimeout(() => auth.logoutAction().then(() => router.push('/login')), 3000)
      } catch {
        message.error('注销失败，请稍后重试')
      }
    },
  })
}

function fmtChars(n: number): string {
  return n >= 10000 ? `${(n / 10000).toFixed(1)}万` : `${n}`
}

function tabLabel(icon: any, label: string) {
  return () => h('span', { style: { display: 'inline-flex', alignItems: 'center', gap: '6px' } }, [
    h(NIcon, { component: icon, size: 14 }),
    label,
  ])
}
</script>

<template>
  <NLayout class="settings-layout">
    <NLayoutContent class="settings-content">
      <NPageHeader
        title="账户设置"
        subtitle="管理你的个人信息和偏好"
        @back="router.back()"
      />

      <div v-if="loading" class="settings-loading">
        <NSpin size="large" />
      </div>

      <NTabs v-else type="line" animated class="settings-tabs">
        <NTabPane name="profile" :tab="tabLabel(User, '个人资料')">
          <NForm label-placement="left" label-width="100" class="settings-form">
            <NFormItem label="邮箱">
              <NInput :value="user?.email ?? ''" readonly placeholder="邮箱" />
            </NFormItem>
            <NFormItem label="昵称">
              <NInput v-model:value="profileForm.nickname" placeholder="请输入昵称" maxlength="50" />
            </NFormItem>
            <NFormItem label="头像链接">
              <NInput v-model:value="profileForm.avatarUrl" placeholder="头像图片 URL（可选）" clearable />
            </NFormItem>
            <NFormItem>
              <NButton type="primary" :loading="profileSaving" @click="saveProfile">保存资料</NButton>
            </NFormItem>
          </NForm>
        </NTabPane>

        <NTabPane name="pref" :tab="tabLabel(Pencil, '创作偏好')">
          <NForm label-placement="left" label-width="120" class="settings-form">
            <NFormItem label="每日目标字数" feedback="写作统计页将以此目标计算进度">
              <NInputNumber
                v-model:value="goalForm.dailyCharGoal"
                placeholder="每日目标字数（如 2000）"
                :min="100"
                :max="100000"
                style="width: 100%"
              />
            </NFormItem>
            <NFormItem label="身份类型">
              <NSelect
                v-model:value="goalForm.identityType"
                :options="identityOptions"
                placeholder="选择创作身份（影响入口排序）"
                clearable
              />
            </NFormItem>
            <NFormItem>
              <NButton type="primary" :loading="goalSaving" @click="saveGoalAndIdentity">保存偏好</NButton>
            </NFormItem>
          </NForm>
        </NTabPane>

        <NTabPane name="ai" :tab="tabLabel(Bot, 'AI 内容')">
          <div class="settings-form">
            <NAlert type="info" style="margin-bottom: 16px">
              开启后，你的写作内容可能被用于改善 AI 模型（经过去标识化处理）。
              你可以随时关闭，关闭后立即生效。
            </NAlert>

            <NFormItem label="AI 训练授权" label-placement="left" label-width="130">
              <NSwitch
                :value="aiConsent"
                :loading="aiConsentSaving"
                @update:value="handleAiConsentChange"
              >
                <template #checked>已开启</template>
                <template #unchecked>已关闭</template>
              </NSwitch>
            </NFormItem>
          </div>
        </NTabPane>

        <NTabPane name="sub" :tab="tabLabel(Gem, '订阅用量')">
          <div class="settings-form settings-form--wide">
            <NSpin :show="subLoading">
              <template v-if="subscription">
                <NSpace align="center" style="margin-bottom: 16px">
                  <NTag :type="subscription.planKey === 'free' ? 'default' : 'success'" size="large">
                    {{ subscription.planName }}
                  </NTag>
                  <NTag type="info">{{ subscription.status === 'active' ? '有效中' : subscription.status }}</NTag>
                  <NText v-if="subscription.expiresAt" depth="3" style="font-size: 13px">
                    到期：{{ new Date(subscription.expiresAt).toLocaleDateString() }}
                  </NText>
                  <NText v-else depth="3" style="font-size: 13px">永久有效</NText>
                </NSpace>

                <div class="quota-row">
                  <div class="quota-label">
                    <NText style="font-size: 14px; font-weight: 500">AI 字符用量</NText>
                    <NText depth="3" style="font-size: 12px">
                      {{ fmtChars(subscription.quota.usedChars) }} /
                      {{ fmtChars(subscription.quota.limitChars) }} 字
                    </NText>
                  </div>
                  <NProgress
                    type="line"
                    :percentage="charUsedPct"
                    :height="10"
                    :show-indicator="false"
                  />
                </div>

                <div class="quota-row">
                  <div class="quota-label">
                    <NText style="font-size: 14px; font-weight: 500">改编 / 审查次数</NText>
                    <NText depth="3" style="font-size: 12px">
                      {{ subscription.quota.usedAdaptations }} /
                      {{ subscription.quota.limitAdaptations }} 次
                    </NText>
                  </div>
                  <NProgress
                    type="line"
                    :percentage="adaptUsedPct"
                    :height="10"
                    :show-indicator="false"
                  />
                </div>

                <NAlert
                  v-if="subscription.planKey === 'free'"
                  type="warning"
                  style="margin-top: 16px"
                >
                  <template #header>升级到专业版，获得 200 万字 AI 月配额</template>
                  订阅升级功能正在开发中（P8-19），敬请期待。
                </NAlert>

                <div class="invite-section">
                  <NText strong style="font-size: 14px">邀请好友赠额度</NText>
                  <NText depth="3" style="font-size: 13px; display: block; margin-top: 4px">
                    每成功邀请一位新用户注册，双方各额外获得 10,000 字体验额度（上限 50,000 字）。
                  </NText>
                  <NText depth="3" style="font-size: 12px; display: block; margin-top: 8px">
                    邀请功能即将上线，敬请期待。
                  </NText>
                </div>
              </template>

              <NAlert v-else-if="!subLoading" type="default">
                订阅信息暂时无法加载，请稍后刷新。
              </NAlert>
            </NSpin>
          </div>
        </NTabPane>

        <NTabPane name="security" :tab="tabLabel(Shield, '账户安全')">
          <div class="settings-form">
            <NText class="section-title">修改密码</NText>
            <NDivider style="margin: 10px 0 16px" />
            <NForm label-placement="left" label-width="110">
              <NFormItem label="当前密码">
                <NInput v-model:value="pwdForm.currentPassword" type="password" placeholder="当前密码" show-password-on="click" />
              </NFormItem>
              <NFormItem label="新密码">
                <NInput v-model:value="pwdForm.newPassword" type="password" placeholder="新密码（至少 8 位）" show-password-on="click" />
              </NFormItem>
              <NFormItem label="确认新密码">
                <NInput v-model:value="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password-on="click" />
              </NFormItem>
              <NFormItem>
                <NButton type="primary" :loading="pwdSaving" @click="savePassword">修改密码</NButton>
              </NFormItem>
            </NForm>

            <NText class="section-title section-title--danger">危险操作</NText>
            <NDivider style="margin: 10px 0 16px" />
            <NAlert type="error" style="margin-bottom: 12px">
              注销账号将软删除你的数据，30 天内可撤销。注销后立即退出所有设备。
            </NAlert>
            <NButton type="error" @click="handleDeleteAccount">注销账号</NButton>
          </div>
        </NTabPane>
      </NTabs>
    </NLayoutContent>
  </NLayout>
</template>

<style scoped>
.settings-layout {
  height: calc(100vh - var(--w-topbar-height));
  background: var(--w-bg);
}

.settings-content {
  padding: var(--w-space-4);
  overflow-y: auto;
  max-width: 900px;
  margin: 0 auto;
  padding-bottom: 72px;
}

@media (min-width: 768px) {
  .settings-content {
    padding: var(--w-space-5) var(--w-space-6);
    padding-bottom: var(--w-space-5);
  }
}

.settings-loading {
  text-align: center;
  padding: 60px;
}

.settings-tabs {
  margin-top: var(--w-space-4);
}

.settings-form {
  max-width: 480px;
  margin-top: var(--w-space-4);
}

.settings-form--wide {
  max-width: 600px;
}

.section-title {
  font-size: var(--w-text-base);
  font-weight: 600;
  color: var(--w-text);
}

.section-title--danger {
  color: var(--w-danger);
}

.quota-row {
  margin-bottom: var(--w-space-5);
}

.quota-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.invite-section {
  margin-top: var(--w-space-4);
  padding: var(--w-space-3);
  background: var(--w-bg-tertiary);
  border-radius: var(--w-radius-md);
  border: 1px solid var(--w-border-default);
}
</style>
