<script setup lang="ts">
import { NConfigProvider, NMessageProvider, NNotificationProvider, NGlobalStyle, zhCN, dateZhCN } from 'naive-ui'
</script>

<template>
  <NConfigProvider :locale="zhCN" :date-locale="dateZhCN">
    <NGlobalStyle />
    <NMessageProvider>
      <NNotificationProvider>
        <RouterView />
      </NNotificationProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>
