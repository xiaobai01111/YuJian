<template>
  <ul class="menu p-4 w-60 min-h-full bg-base-100 text-base-content border-r border-base-200">
    <!-- Logo area -->
    <li class="mb-4">
      <a class="text-xl font-bold px-2 py-4 gap-2 hover:bg-transparent cursor-default">
        <span class="bg-primary text-primary-content rounded-lg p-1">CW</span>
        CampusConsole
      </a>
    </li>
    
    <template v-for="section in menuSections" :key="section.code">
      <li class="px-2 py-2 text-xs uppercase tracking-widest text-slate-400">{{ section.label }}</li>
      <ConsoleSidebarItem
        v-for="route in section.routes"
        :key="route.path"
        :route="route"
        :base-path="resolvePath(route.path)"
      />
      <li v-if="section.code !== menuSections[menuSections.length - 1]?.code" class="my-2 h-px bg-base-200"></li>
    </template>
  </ul>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ConsoleSidebarItem from './ConsoleSidebarItem.vue'

interface SidebarRouteMeta {
  title?: string
  icon?: string
  hidden?: boolean
  groupCode?: string
  featureCode?: string
  routeType?: string
}

interface SidebarRoute {
  path: string
  meta?: SidebarRouteMeta
  children?: SidebarRoute[]
}

interface MenuSection {
  code: string
  label: string
  routes: SidebarRoute[]
}

// Props
const props = defineProps<{
  routes: SidebarRoute[]
}>()

const menuRoutes = computed(() => {
  return props.routes.filter(route => route.meta && !route.meta.hidden)
})

const GROUP_ORDER = ['WORKBENCH', 'SYSTEM', 'CONTENT', 'ASSET', 'MONITOR', 'CAMPUS', 'AUDIT', 'GENERAL'] as const

const GROUP_LABELS: Record<string, string> = {
  WORKBENCH: '工作台',
  SYSTEM: '系统管理',
  CONTENT: '内容治理',
  ASSET: '资产管理',
  MONITOR: '系统运维',
  CAMPUS: '校园管理',
  AUDIT: '审计日志',
  GENERAL: '其他'
}

function inferGroupCode(route: SidebarRoute): string {
  const title = route.meta?.title
  if (title === '仪表盘') return 'WORKBENCH'
  if (title === '系统管理') return 'SYSTEM'
  if (title === '内容管理') return 'CONTENT'
  if (title === '资产管理' || title === '系统工具') return 'ASSET'
  if (title === '系统监控') return 'MONITOR'
  if (title === '校园管理') return 'CAMPUS'
  if (title === '审计日志') return 'AUDIT'
  return 'GENERAL'
}

const menuSections = computed(() => {
  const buckets = new Map<string, SidebarRoute[]>()
  for (const route of menuRoutes.value) {
    const code = route.meta?.groupCode || inferGroupCode(route)
    const list = buckets.get(code) || []
    list.push(route)
    buckets.set(code, list)
  }

  const sections: MenuSection[] = GROUP_ORDER
    .filter(code => (buckets.get(code) || []).length > 0)
    .map(code => ({
      code,
      label: GROUP_LABELS[code] || '其他',
      routes: buckets.get(code) || []
    }))

  for (const [code, routes] of buckets) {
    if (!sections.some(section => section.code === code)) {
      sections.push({
        code,
        label: GROUP_LABELS[code] || '其他',
        routes
      })
    }
  }
  return sections
})

const resolvePath = (path: string) => {
  if (path.startsWith('/')) return path
  return `/${path}`
}
</script>
