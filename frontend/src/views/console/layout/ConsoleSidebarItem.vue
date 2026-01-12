<template>
  <li v-if="!route.meta?.hidden">
    <template v-if="hasChildren(route)">
      <details :open="isOpen">
        <summary>
          <component :is="getIcon(route.meta?.icon)" class="w-5 h-5" />
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
        <component :is="getIcon(route.meta?.icon)" class="w-5 h-5" />
        {{ route.meta?.title }}
      </router-link>
    </template>
  </li>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps<{
  route: any,
  basePath: string
}>()

const currentRoute = useRoute()

const isOpen = computed(() => {
  if (!props.route.children) return false
  return props.route.children.some((child: any) => 
    currentRoute.path.startsWith(child.path)
  )
})

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

const iconPaths: Record<string, string> = {
  dashboard: 'M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z',
  user: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
  peoples: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z',
  setting: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z',
  tree: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4',
  'tree-table': 'M4 6h16M4 10h16M4 14h16M4 18h16',
  dict: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253',
  post: 'M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z',
  log: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
  logininfor: 'M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1',
  form: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01'
}

const getIcon = (icon?: string) => {
  const path = icon && iconPaths[icon] ? iconPaths[icon] : 'M4 6h16M4 12h16M4 18h16'
  return h('svg', { 
    xmlns: 'http://www.w3.org/2000/svg',
    fill: 'none', 
    viewBox: '0 0 24 24', 
    stroke: 'currentColor',
    'stroke-width': '2'
  }, [
    h('path', { 
      'stroke-linecap': 'round', 
      'stroke-linejoin': 'round', 
      d: path 
    })
  ])
}
</script>

<script lang="ts">
export default {
  name: 'ConsoleSidebarItem'
}
</script>
