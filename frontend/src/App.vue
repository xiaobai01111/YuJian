<template>
  <component :is="layout">
    <router-view v-slot="{ Component }">
      <component :is="Component" :key="route.fullPath" />
    </router-view>
  </component>
  <AppDialog />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import AppDialog from '@/components/ui/AppDialog.vue'

const route = useRoute()
const layout = computed(() => {
  return route.meta.layout || DefaultLayout
})
</script>
