<script setup lang="ts">
/**
 * 订阅升级引导弹窗（P8-19）。
 *
 * 三个场景分别展示不同的升级说明：
 * - quota-chars：AI 字符配额用尽
 * - quota-adaptations：改编/审查次数用尽
 * - pro-feature：专业版功能入口
 *
 * 点击"立即升级"调用 POST /subscriptions/checkout 创建支付订单，
 * 返回 payUrl 后跳转（COS 未配置时展示占位 URL）。
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NModal, NButton, NTag, NText, NDivider,
  NSpin, useMessage,
} from 'naive-ui'
import { useUpgradeModal } from '@/composables/useUpgradeModal'
import type { UpgradeScenario } from '@/composables/useUpgradeModal'
import { createCheckout, getSubscriptionPlans, getCurrentSubscription } from '@/api/subscription'
import type { PlanInfo } from '@/api/subscription'

const router = useRouter()
const notify = useMessage()
const { visible, scenario, closeUpgrade } = useUpgradeModal()

const plans = ref<PlanInfo[]>([])
const currentPlanKey = ref('free')
const loadingPlans = ref(false)
const checkoutLoading = ref(false)

/** 场景标题 */
const scenarioTitle = computed<string>(() => {
  const titles: Record<UpgradeScenario, string> = {
    'quota-chars': '🚀 AI 字符配额已用尽',
    'quota-adaptations': '🎬 改编/审查次数已用尽',
    'pro-feature': '✨ 升级到专业版',
  }
  return titles[scenario.value]
})

/** 场景描述 */
const scenarioDesc = computed<string>(() => {
  const descs: Record<UpgradeScenario, string> = {
    'quota-chars': '本月 AI 字符配额已耗尽。升级专业版获得 200 万字/月配额，继续你的创作。',
    'quota-adaptations': '本月改编/审查次数已耗尽（免费版 5 次/月）。升级专业版获得 50 次/月。',
    'pro-feature': '该功能需要专业版或更高套餐。升级后解锁全部 AI 功能，享受更强大的创作体验。',
  }
  return descs[scenario.value]
})

/** 过滤显示的套餐（仅展示非免费套餐，当前已是高级套餐则不展示该套餐以下的） */
const displayPlans = computed(() => plans.value.filter(p => p.planKey !== 'free'))

/** 专业版功能列表 */
const proFeatures = [
  '✅ 每月 200 万字 AI 字符额度',
  '✅ 每月 50 次改编/一致性审查',
  '✅ 优先响应，更快的 AI 生成',
  '✅ 无限制快照版本历史',
  '✅ 高级文风分析与定制',
]

/** 加载套餐列表和当前订阅 */
async function loadData() {
  loadingPlans.value = true
  try {
    const [plansRes, subRes] = await Promise.allSettled([
      getSubscriptionPlans(),
      getCurrentSubscription(),
    ])
    if (plansRes.status === 'fulfilled') {
      plans.value = plansRes.value.data.data
    }
    if (subRes.status === 'fulfilled') {
      currentPlanKey.value = subRes.value.data.data.planKey
    }
  } catch {
    // 静默失败
  } finally {
    loadingPlans.value = false
  }
}

/** 点击"立即升级"发起结账（P9-03 / P8-19）*/
async function handleCheckout(planKey: string) {
  checkoutLoading.value = true
  try {
    const res = await createCheckout(planKey)
    const payUrl = res.data.data.payUrl

    if (!payUrl || payUrl.startsWith('PAYMENT_NOT_CONFIGURED')) {
      notify.warning('支付渠道尚未配置，请联系管理员或稍后重试')
      closeUpgrade()
      return
    }
    // 在新标签打开支付页
    window.open(payUrl, '_blank')
    closeUpgrade()
    notify.info('已在新窗口打开支付页面，完成支付后请刷新页面')
  } catch {
    notify.error('创建支付订单失败，请稍后重试')
  } finally {
    checkoutLoading.value = false
  }
}

/** 弹窗打开时加载数据 */
function onShow() {
  loadData()
}
</script>

<template>
  <Teleport to="body">
    <NModal
      v-model:show="visible"
      preset="card"
      :title="scenarioTitle"
      style="width: min(560px, 96vw)"
      :segmented="{ content: true }"
      @after-enter="onShow"
    >
      <!-- 场景说明 -->
      <NText style="font-size: 14px; display: block; margin-bottom: 16px">
        {{ scenarioDesc }}
      </NText>

      <!-- 加载中 -->
      <div v-if="loadingPlans" style="text-align: center; padding: 24px">
        <NSpin />
      </div>

      <template v-else>
        <!-- 专业版特性列表 -->
        <div class="upgrade-features">
          <div v-for="f in proFeatures" :key="f" class="upgrade-feature-item">
            {{ f }}
          </div>
        </div>

        <NDivider style="margin: 16px 0" />

        <!-- 套餐选择 -->
        <div class="upgrade-plans">
          <div
            v-for="plan in displayPlans"
            :key="plan.planKey"
            class="upgrade-plan-card"
            :class="{ 'upgrade-plan-current': plan.planKey === currentPlanKey }"
          >
            <!-- 套餐头部 -->
            <div class="upgrade-plan-header">
              <NText strong style="font-size: 16px">{{ plan.name }}</NText>
              <NTag
                v-if="plan.planKey === currentPlanKey"
                type="success" size="small" style="margin-left: 8px"
              >当前套餐</NTag>
              <div style="flex: 1" />
              <div style="text-align: right">
                <NText strong style="font-size: 22px; color: #18a058">
                  ¥{{ plan.pricePerMonth }}
                </NText>
                <NText depth="3" style="font-size: 12px">/月</NText>
              </div>
            </div>

            <!-- 套餐描述 -->
            <NText depth="3" style="font-size: 13px; display: block; margin: 8px 0">
              {{ plan.description }}
            </NText>

            <!-- 配额信息 -->
            <div style="margin-bottom: 12px">
              <NText style="font-size: 12px">
                字符额度：{{ (plan.monthlyCharLimit / 10000).toFixed(0) }} 万字/月 |
                改编次数：{{ plan.monthlyAdaptationLimit }} 次/月
              </NText>
            </div>

            <!-- 升级按钮 -->
            <NButton
              v-if="plan.planKey !== currentPlanKey"
              type="primary"
              block
              :loading="checkoutLoading"
              @click="handleCheckout(plan.planKey)"
            >
              立即升级到{{ plan.name }}
            </NButton>
            <NButton v-else block disabled>已是当前套餐</NButton>
          </div>
        </div>

        <!-- 跳转账户设置 -->
        <div style="margin-top: 12px; text-align: center">
          <NButton
            text
            size="small"
            @click="router.push('/settings?tab=sub'); closeUpgrade()"
          >
            查看用量详情和账单
          </NButton>
        </div>
      </template>
    </NModal>
  </Teleport>
</template>

<style scoped>
/* ─── 功能列表 ─── */
.upgrade-features {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin-bottom: 4px;
}
.upgrade-feature-item {
  font-size: 13px;
  color: #555;
}

/* ─── 套餐卡片 ─── */
.upgrade-plans {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.upgrade-plan-card {
  flex: 1;
  min-width: 180px;
  border: 1px solid rgba(128, 128, 128, 0.2);
  border-radius: 10px;
  padding: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.upgrade-plan-card:hover {
  border-color: #18a058;
  box-shadow: 0 2px 12px rgba(24, 160, 88, 0.15);
}
.upgrade-plan-current {
  border-color: #18a058;
  background: rgba(24, 160, 88, 0.04);
}
.upgrade-plan-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

/* ─── 移动端 ─── */
@media (max-width: 767px) {
  .upgrade-features { grid-template-columns: 1fr; }
  .upgrade-plans { flex-direction: column; }
}
</style>
