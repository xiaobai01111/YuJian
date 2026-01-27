import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface Tab {
  path: string
  title: string
  name?: string
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<Tab[]>([])
  const activeTab = ref<string>('')

  const addTab = (tab: Tab) => {
    const exists = tabs.value.find(t => t.path === tab.path)
    if (!exists) {
      tabs.value.push(tab)
    }
    activeTab.value = tab.path
  }

  const removeTab = (path: string) => {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index > -1) {
      tabs.value.splice(index, 1)
    }
  }

  const setActiveTab = (path: string) => {
    activeTab.value = path
  }

  const getNextTab = (closingPath: string): string | null => {
    const index = tabs.value.findIndex(t => t.path === closingPath)
    if (tabs.value.length <= 1) return null
    
    if (index === tabs.value.length - 1) {
      return tabs.value[index - 1]?.path || null
    }
    return tabs.value[index + 1]?.path || null
  }

  const clearTabs = () => {
    tabs.value = []
    activeTab.value = ''
  }

  return {
    tabs,
    activeTab,
    addTab,
    removeTab,
    setActiveTab,
    getNextTab,
    clearTabs
  }
})
