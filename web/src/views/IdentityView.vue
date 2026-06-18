<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpenText, Clapperboard, Sparkles } from 'lucide-vue-next'
import { setIdentityType } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()
const selected = ref('web_novel_author')
const loading = ref(false)

const identities = [
  {
    key: 'web_novel_author',
    title: '小说家',
    desc: '长篇作品、章节大纲、角色库和世界观优先。',
    icon: BookOpenText,
  },
  {
    key: 'short_drama_writer',
    title: '编剧',
    desc: '短剧改编、场景拆分、台词和分集工作台优先。',
    icon: Clapperboard,
  },
  {
    key: 'new_author',
    title: '全能创作者',
    desc: '保留双入口，按项目自由切换小说与剧本流程。',
    icon: Sparkles,
  },
]

async function submit() {
  loading.value = true
  try {
    const res = await setIdentityType(selected.value)
    auth.user = res.data.data
    localStorage.setItem('wenshu-identity-picked', '1')
    router.push('/')
  } catch (error) {
    toast.error((error as { response?: { data?: { message?: string } } }).response?.data?.message || '身份保存失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="ws-auth ws-auth--wide">
    <section class="ws-auth__card">
      <div class="ws-auth__brand">
        <span class="ws-brand__mark">文</span>
        <div>
          <h1>选择你的创作身份</h1>
          <p>文枢会按身份调整默认入口和工作台重点。</p>
        </div>
      </div>

      <div class="identity-grid">
        <button
          v-for="item in identities"
          :key="item.key"
          type="button"
          class="identity-card"
          :class="{ 'is-active': selected === item.key }"
          @click="selected = item.key"
        >
          <component :is="item.icon" :size="24" />
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </button>
      </div>

      <button class="ws-button ws-button--primary" type="button" :disabled="loading" @click="submit">
        {{ loading ? '保存中' : '进入工作台' }}
      </button>
    </section>
  </main>
</template>
