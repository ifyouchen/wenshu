<script setup lang="ts">
import { onMounted } from 'vue'
import { NLayout } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useQuotaStore } from '@/stores/quota'

const auth = useAuthStore()
const quota = useQuotaStore()

onMounted(async () => {
  if (!auth.user) await auth.fetchUser()
  await quota.refresh()
})
</script>

<template>
  <NLayout style="height: 100vh; overflow: hidden">
    <RouterView />
  </NLayout>
</template>
