<template>
  <div class="chrome-tab-bar">
    <div class="tabs-container">
      <div 
        v-for="tab in tabs" 
        :key="tab.path"
        class="chrome-tab"
        :class="{ 'active': isActive(tab.path) }"
        @click="switchTab(tab.path)"
      >
        <span class="tab-title">{{ tab.title }}</span>
        <button 
          class="tab-close"
          @click.stop="closeTab(tab.path)"
          title="关闭"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
            <path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.75.75 0 1 1 1.06 1.06L9.06 8l3.22 3.22a.75.75 0 1 1-1.06 1.06L8 9.06l-3.22 3.22a.75.75 0 0 1-1.06-1.06L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'

const route = useRoute()
const router = useRouter()
const tabsStore = useTabsStore()

const tabs = computed(() => tabsStore.tabs)

const isActive = (path: string) => route.path === path

const switchTab = (path: string) => {
  router.push(path)
}

const closeTab = (path: string) => {
  if (tabs.value.length <= 1) return
  
  const nextPath = tabsStore.getNextTab(path)
  tabsStore.removeTab(path)
  
  if (isActive(path) && nextPath) {
    router.push(nextPath)
  }
}

watch(
  () => route.path,
  (newPath) => {
    if (newPath.startsWith('/console')) {
      const title = (route.meta?.title as string) || route.name?.toString() || newPath.split('/').pop() || 'Page'
      tabsStore.addTab({ path: newPath, title, name: route.name?.toString() })
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.chrome-tab-bar {
  background: #dee1e6;
  padding: 6px 8px 0;
  min-height: 38px;
}

.tabs-container {
  display: flex;
  align-items: flex-end;
  overflow-x: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.tabs-container::-webkit-scrollbar {
  display: none;
}

.chrome-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 8px 0 12px;
  background: #c8cad0;
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  transition: background 0.15s;
  margin-right: 1px;
}

.chrome-tab:hover {
  background: #d5d7db;
}

.chrome-tab.active {
  background: #fff;
}

.tab-title {
  font-size: 12px;
  color: #5f6368;
  white-space: nowrap;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  user-select: none;
}

.chrome-tab.active .tab-title {
  color: #202124;
  font-weight: 500;
}

.tab-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #5f6368;
  cursor: pointer;
  transition: background 0.15s;
  flex-shrink: 0;
}

.tab-close:hover {
  background: rgba(0, 0, 0, 0.1);
}

.tab-close svg {
  width: 10px;
  height: 10px;
}

.chrome-tab.active .tab-close {
  color: #202124;
}

/* Dark mode */
[data-theme="dark"] .chrome-tab-bar {
  background: #35363a;
}

[data-theme="dark"] .chrome-tab {
  background: #292a2d;
}

[data-theme="dark"] .chrome-tab:hover {
  background: #3c3d41;
}

[data-theme="dark"] .chrome-tab.active {
  background: #202124;
}

[data-theme="dark"] .tab-title {
  color: #9aa0a6;
}

[data-theme="dark"] .chrome-tab.active .tab-title {
  color: #e8eaed;
}

[data-theme="dark"] .tab-close {
  color: #9aa0a6;
}

[data-theme="dark"] .tab-close:hover {
  background: rgba(255, 255, 255, 0.1);
}
</style>
