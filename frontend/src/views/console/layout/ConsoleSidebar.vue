<template>
  <ul class="menu p-4 w-60 min-h-full bg-base-100 text-base-content border-r border-base-200">
    <!-- Logo area -->
    <li class="mb-4">
      <a class="text-xl font-bold px-2 py-4 gap-2 hover:bg-transparent cursor-default">
        <span class="bg-primary text-primary-content rounded-lg p-1">CW</span>
        CampusConsole
      </a>
    </li>
    
    <ConsoleSidebarItem 
      v-for="route in menuRoutes" 
      :key="route.path" 
      :route="route" 
      :base-path="resolvePath(route.path)" 
    />
  </ul>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ConsoleSidebarItem from './ConsoleSidebarItem.vue'

// Props
const props = defineProps<{
  routes: any[]
}>()

const menuRoutes = computed(() => {
    return props.routes.filter(route => !route.hidden && route.meta)
})

const resolvePath = (path: string) => {
  if (path.startsWith('/')) return path
  return `/${path}`
}
</script>
