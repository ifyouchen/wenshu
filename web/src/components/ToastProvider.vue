<script setup lang="ts">
import { globalToast } from '@/composables/useToast'
</script>

<template>
  <slot />
  <Teleport to="body">
    <div class="toast-stack">
      <button
        v-for="toast in globalToast.toasts"
        :key="toast.id"
        class="toast-item"
        :class="`toast-item--${toast.type}`"
        @click="globalToast.dismiss(toast.id)"
      >
        <span class="toast-dot" />
        <span>{{ toast.content }}</span>
      </button>
    </div>
  </Teleport>
</template>
