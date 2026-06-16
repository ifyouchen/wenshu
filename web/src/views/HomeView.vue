<script setup lang="ts">
import { NEmpty, NButton, NH1, NSpace } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await auth.logoutAction()
  router.push('/login')
}
</script>

<template>
  <div style="padding: 32px">
    <NSpace justify="space-between" align="center" style="margin-bottom: 24px">
      <NH1>我的作品</NH1>
      <NButton @click="handleLogout">退出登录</NButton>
    </NSpace>
    <!-- P8-05 作品首页占位 -->
    <NEmpty description="暂无作品，点击右上角创建" />
  </div>
</template>
