<template>
  <ul class="menu p-4 w-60 min-h-full bg-base-100 text-base-content border-r border-base-200">
    <!-- Logo area -->
    <li class="mb-4">
      <a class="text-xl font-bold px-2 py-4 gap-2 hover:bg-transparent cursor-default">
        <span class="bg-primary text-primary-content rounded-lg p-1">CW</span>
        CampusConsole
      </a>
    </li>
    
    <li class="px-2 py-2 text-xs uppercase tracking-widest text-slate-400">管理端</li>

    <ConsoleSidebarItem 
      v-for="route in adminRoutes" 
      :key="route.path" 
      :route="route" 
      :base-path="resolvePath(route.path)" 
    />

    <li v-if="showDivider" class="my-3 h-px bg-base-200"></li>

    <li v-if="userRoutes.length > 0" class="px-2 py-2 text-xs uppercase tracking-widest text-slate-400">用户端</li>

    <ConsoleSidebarItem 
      v-for="route in userRoutes" 
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
    return props.routes.filter(route => route.meta && !route.meta.hidden)
})

const adminTitles = new Set(['仪表盘', '系统管理', '系统监控', '系统工具'])
const userTitles = new Set(['内容管理'])

const adminRoutes = computed(() => {
  return menuRoutes.value.filter(route => !userTitles.has(route.meta?.title) || adminTitles.has(route.meta?.title))
})

const userRoutes = computed(() => {
  return menuRoutes.value.filter(route => userTitles.has(route.meta?.title))
})

const showDivider = computed(() => adminRoutes.value.length > 0 && userRoutes.value.length > 0)

const resolvePath = (path: string) => {
  if (path.startsWith('/')) return path
  return `/${path}`
}
</script>
