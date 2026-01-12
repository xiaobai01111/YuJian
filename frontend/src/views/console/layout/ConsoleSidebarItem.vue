<template>
  <li v-if="!route.meta?.hidden">
    <template v-if="hasChildren(route)">
      <details :open="false">
        <summary>
          <svg v-if="route.meta?.icon" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
             <path v-if="route.meta.icon === 'user'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
             <path v-else-if="route.meta.icon === 'dashboard'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
             <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
          {{ route.meta?.title }}
        </summary>
        <ul>
          <ConsoleSidebarItem 
            v-for="child in route.children" 
            :key="child.path" 
            :route="child" 
            :base-path="resolvePath(route.path)" 
          />
        </ul>
      </details>
    </template>
    
    <template v-else>
      <router-link :to="resolvePath(route.path)" active-class="active">
        <svg v-if="route.meta?.icon" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
           <path v-if="route.meta.icon === 'user'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
           <path v-else-if="route.meta.icon === 'dashboard'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
           <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
        {{ route.meta?.title }}
      </router-link>
    </template>
  </li>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  route: any,
  basePath: string
}>()

const hasChildren = (item: any) => {
  return item.children && item.children.some((child: any) => !child.meta?.hidden)
}

const resolvePath = (routePath: string) => {
  if (routePath.startsWith('/')) {
    return routePath
  }
  if (props.basePath.endsWith('/')) {
    return `${props.basePath}${routePath}`
  }
  return `${props.basePath}/${routePath}`
}
</script>

<script lang="ts">
export default {
  name: 'ConsoleSidebarItem'
}
</script>
