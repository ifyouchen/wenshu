<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CreditCard, KeyRound, ShieldCheck, UserRound } from 'lucide-vue-next'
import { changePassword, getMe, setIdentityType, setWritingGoal, updateAiConsent, updateProfile } from '@/api/user'
import { getCurrentSubscription } from '@/api/subscription'
import type { CurrentSubscriptionInfo } from '@/api/subscription'
import type { UserInfo } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()
const user = ref<UserInfo | null>(null)
const subscription = ref<CurrentSubscriptionInfo | null>(null)
const profile = reactive({ nickname: '', avatarUrl: '' })
const prefs = reactive({ dailyCharGoal: 2000, identityType: 'new_author', aiTrainConsent: false })
const password = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

const charPct = computed(() => {
  const quota = subscription.value?.quota
  if (!quota?.limitChars) return 0
  return Math.min(Math.round((quota.usedChars / quota.limitChars) * 100), 100)
})

onMounted(async () => {
  const [me, sub] = await Promise.allSettled([getMe(), getCurrentSubscription()])
  if (me.status === 'fulfilled') {
    user.value = me.value.data.data
    profile.nickname = user.value.nickname || ''
    profile.avatarUrl = user.value.avatarUrl || ''
    prefs.dailyCharGoal = user.value.dailyCharGoal || 2000
    prefs.identityType = user.value.identityType || 'new_author'
    prefs.aiTrainConsent = user.value.aiTrainConsent
  }
  if (sub.status === 'fulfilled') subscription.value = sub.value.data.data
})

async function saveProfile() {
  const res = await updateProfile(profile)
  user.value = res.data.data
  auth.user = res.data.data
  toast.success('资料已保存')
}

async function savePrefs() {
  await setWritingGoal(prefs.dailyCharGoal)
  const res = await setIdentityType(prefs.identityType)
  await updateAiConsent(prefs.aiTrainConsent)
  user.value = res.data.data
  auth.user = res.data.data
  localStorage.setItem('wenshu-identity-picked', '1')
  toast.success('偏好已保存')
}

async function savePassword() {
  if (password.newPassword !== password.confirmPassword) {
    toast.error('两次输入的新密码不一致')
    return
  }
  await changePassword({ currentPassword: password.currentPassword, newPassword: password.newPassword })
  toast.success('密码已修改，请重新登录')
  await auth.logoutAction()
  router.push('/login')
}
</script>

<template>
  <div class="ws-page">
    <section class="ws-page__head">
      <div>
        <p class="ws-eyebrow">Account</p>
        <h1>设置</h1>
        <p>账户资料、创作偏好、AI 授权和订阅用量。</p>
      </div>
    </section>

    <section class="settings-grid">
      <form class="ws-card ws-form" @submit.prevent="saveProfile">
        <h2><UserRound :size="18" />账户资料</h2>
        <label class="ws-field"><span>邮箱</span><input class="ws-input" :value="user?.email || ''" readonly></label>
        <label class="ws-field"><span>昵称</span><input v-model="profile.nickname" class="ws-input"></label>
        <label class="ws-field"><span>头像链接</span><input v-model="profile.avatarUrl" class="ws-input"></label>
        <button class="ws-button ws-button--primary">保存资料</button>
      </form>

      <form class="ws-card ws-form" @submit.prevent="savePrefs">
        <h2><ShieldCheck :size="18" />偏好与授权</h2>
        <label class="ws-field"><span>每日目标字数</span><input v-model.number="prefs.dailyCharGoal" class="ws-input" type="number" min="100"></label>
        <label class="ws-field">
          <span>身份</span>
          <select v-model="prefs.identityType" class="ws-select full">
            <option value="web_novel_author">小说家</option>
            <option value="short_drama_writer">编剧</option>
            <option value="new_author">全能创作者</option>
          </select>
        </label>
        <label class="switch-row"><input v-model="prefs.aiTrainConsent" type="checkbox">允许内容改进授权</label>
        <button class="ws-button ws-button--primary">保存偏好</button>
      </form>

      <form class="ws-card ws-form" @submit.prevent="savePassword">
        <h2><KeyRound :size="18" />账户安全</h2>
        <label class="ws-field"><span>当前密码</span><input v-model="password.currentPassword" class="ws-input" type="password"></label>
        <label class="ws-field"><span>新密码</span><input v-model="password.newPassword" class="ws-input" type="password"></label>
        <label class="ws-field"><span>确认新密码</span><input v-model="password.confirmPassword" class="ws-input" type="password"></label>
        <button class="ws-button ws-button--primary">修改密码</button>
      </form>

      <section class="ws-card ws-form">
        <h2><CreditCard :size="18" />订阅与配额</h2>
        <p>{{ subscription?.planName || '订阅信息暂不可用' }}</p>
        <div class="progress-track"><span :style="{ width: `${charPct}%` }" /></div>
        <small v-if="subscription">{{ subscription.quota.usedChars }} / {{ subscription.quota.limitChars }} 字</small>
        <button class="ws-button" type="button">订阅入口</button>
      </section>
    </section>
  </div>
</template>
