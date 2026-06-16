<script setup lang="ts">
import { onMounted } from 'vue'
import { NLayout, NLayoutSider, NLayoutContent, NSpace, NText } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useQuotaStore } from '@/stores/quota'

const auth = useAuthStore()
const quota = useQuotaStore()

onMounted(async () => {
  // 页面刷新后恢复用户信息
  if (!auth.user) await auth.fetchUser()
  // 预加载配额
  await quota.refresh()
})
</script>

<template>
  <NLayout has-sider style="height: 100vh">
    <NLayoutSider
      bordered
      :width="60"
      :collapsed-width="60"
      style="background: var(--n-color)"
    >
      <!-- P8-07 侧栏图标面板占位 -->
      <NSpace vertical align="center" style="padding-top: 16px; gap: 8px">
        <NText depth="3" style="font-size: 12px">文枢</NText>
      </NSpace>
    </NLayoutSider>
    <NLayoutContent>
      <RouterView />
    </NLayoutContent>
  </NLayout>
</template>
