<script setup lang="ts">
/**
 * 订阅升级引导弹窗（P8-19）。
 *
 * 三个场景分别展示不同的升级说明：
 * - quota-chars：创作辅助字数用尽
 * - quota-adaptations：改编/审查次数用尽
 * - pro-feature：专业版功能入口
 *
 * 点击"立即升级"调用 POST /subscriptions/checkout 创建支付订单，
 * 返回 payUrl 后跳转（COS 未配置时展示占位 URL）。
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NModal, NButton, NTag, NText, NDivider, NIcon,
  NSpin, useMessage,
} from 'naive-ui'
import {
  Rocket,
  Check,
} from 'lucide-vue-next'
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

const scenarioIcon = computed(() => {
  return Rocket
})

const scenarioTitle = computed<string>(() => {
  const titles: Record<UpgradeScenario, string> = {
    'quota-chars': '创作辅助字数已用尽',
    'quota-adaptations': '改编/审查次数已用尽',
    'pro-feature': '升级到专业版',
  }
  return titles[scenario.value]
})

const scenarioDesc = computed<string>(() => {
  const descs: Record<UpgradeScenario, string> = {
    'quota-chars': '本月创作辅助字数已耗尽。升级专业版获得 200 万字/月配额，继续你的创作。',
    'quota-adaptations': '本月改编/审查次数已耗尽（免费版 5 次/月）。升级专业版获得 50 次/月。',
    'pro-feature': '该功能需要专业版或更高套餐。升级后解锁完整创作辅助能力，获得更稳定的工作流。',
  }
  return descs[scenario.value]
})

const displayPlans = computed(() => plans.value.filter(p => p.planKey !== 'free'))

const proFeatures = [
  '每月 200 万字创作辅助额度',
  '每月 50 次改编/一致性审查',
  '优先响应，更快的生成速度',
  '无限制快照版本历史',
  '高级文风分析与定制',
]

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
    window.open(payUrl, '_blank')
    closeUpgrade()
    notify.info('已在新窗口打开支付页面，完成支付后请刷新页面')
  } catch {
    notify.error('创建支付订单失败，请稍后重试')
  } finally {
    checkoutLoading.value = false
  }
}

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
      <template #header>
        <div class="modal-title">
          <NIcon :component="scenarioIcon" :size="18" class="modal-title-icon" />
          <span>{{ scenarioTitle }}</span>
        </div>
      </template>

      <NText style="font-size: 14px; display: block; margin-bottom: 16px">
        {{ scenarioDesc }}
      </NText>

      <div v-if="loadingPlans" class="plans-loading">
        <NSpin />
      </div>

      <template v-else>
        <div class="upgrade-features">
          <div v-for="f in proFeatures" :key="f" class="upgrade-feature-item">
            <NIcon :component="Check" :size="14" class="feature-check" />
            <span>{{ f }}</span>
          </div>
        </div>

        <NDivider style="margin: 16px 0" />

        <div class="upgrade-plans">
          <div
            v-for="plan in displayPlans"
            :key="plan.planKey"
            class="upgrade-plan-card"
            :class="{ 'upgrade-plan-current': plan.planKey === currentPlanKey }"
          >
            <div class="upgrade-plan-header">
              <NText strong style="font-size: 16px">{{ plan.name }}</NText>
              <NTag
                v-if="plan.planKey === currentPlanKey"
                type="success"
                size="small"
                style="margin-left: 8px"
              >
                当前套餐
              </NTag>
              <div style="flex: 1" />
              <div style="text-align: right">
                <NText strong class="plan-price">
                  ¥{{ plan.pricePerMonth }}
                </NText>
                <NText depth="3" style="font-size: 12px">/月</NText>
              </div>
            </div>

            <NText depth="3" style="font-size: 13px; display: block; margin: 8px 0">
              {{ plan.description }}
            </NText>

            <div style="margin-bottom: 12px">
              <NText style="font-size: 12px">
                字符额度：{{ (plan.monthlyCharLimit / 10000).toFixed(0) }} 万字/月 |
                改编次数：{{ plan.monthlyAdaptationLimit }} 次/月
              </NText>
            </div>

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

        <div class="upgrade-footer">
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
.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-title-icon {
  color: var(--w-brand);
}

.plans-loading {
  text-align: center;
  padding: 24px;
}

.upgrade-features {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 4px;
}

.upgrade-feature-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--w-text-secondary);
}

.feature-check {
  color: var(--w-success);
  flex-shrink: 0;
}

.upgrade-plans {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.upgrade-plan-card {
  flex: 1;
  min-width: 180px;
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  padding: 14px;
  transition: border-color var(--w-transition-base), box-shadow var(--w-transition-base);
  background: var(--w-bg-secondary);
}

.upgrade-plan-card:hover {
  border-color: var(--w-brand);
  box-shadow: 0 2px 12px var(--w-brand-glow);
}

.upgrade-plan-current {
  border-color: var(--w-success);
  background: var(--w-success-soft);
}

.upgrade-plan-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.plan-price {
  font-size: 22px;
  color: var(--w-success);
}

.upgrade-footer {
  margin-top: 12px;
  text-align: center;
}

@media (max-width: 767px) {
  .upgrade-features { grid-template-columns: 1fr; }
  .upgrade-plans { flex-direction: column; }
}
</style>
