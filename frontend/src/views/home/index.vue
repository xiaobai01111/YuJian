<template>
  <div>
    <!-- Category Cards -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
      <router-link v-for="category in categories" :key="category.id" :to="category.path"
        class="card bg-base-100 shadow-sm hover:shadow-md transition-all duration-300 hover:-translate-y-1 border border-base-200 cursor-pointer group">
        <div class="card-body p-6 items-start">
          <div :class="`p-3 rounded-xl ${category.bgClass} ${category.textClass} mb-3 group-hover:scale-110 transition-transform`">
            <component :is="category.icon" class="w-6 h-6" />
          </div>
          <h3 class="font-bold text-base text-slate-800">{{ category.name }}</h3>
          <p class="text-xs text-slate-500 mt-1">{{ category.desc }}</p>
        </div>
      </router-link>
    </div>

    <!-- Main Content Section -->
    <div class="flex flex-col space-y-6">
      <!-- Section Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-1.5 h-6 bg-blue-600 rounded-full"></div>
          <h2 class="text-xl font-bold text-slate-800">最新动态</h2>
        </div>
        <div class="flex gap-4 text-sm font-medium text-slate-500">
          <button class="hover:text-slate-900 transition-colors">全部精华</button>
        </div>
      </div>

      <!-- Content Area -->
      <div class="bg-base-100 border border-dashed border-base-300 rounded-2xl min-h-[400px] flex flex-col items-center justify-center text-center p-8">
        <div class="w-24 h-24 bg-slate-50 rounded-full flex items-center justify-center mb-4">
           <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
        </div>
        <h3 class="text-slate-900 font-medium mb-1">暂无内容</h3>
        <p class="text-slate-500 text-sm">换个筛选条件试试？</p>
      </div>
    </div>
  </div>

  <!-- Sidebar Content (Injected into Layout) -->
  <teleport to="#sidebar-slot-target" v-if="mounted">
      <!-- Bulletin -->
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body p-5">
           <div class="flex items-center justify-between mb-4">
             <div class="flex items-center gap-2">
               <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                 <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z" />
               </svg>
               <h3 class="font-bold text-slate-800">公告栏</h3>
             </div>
             <router-link to="/notices" class="text-xs text-blue-500 hover:underline">更多</router-link>
           </div>
           <div v-if="notices.length > 0" class="space-y-3">
             <router-link
               v-for="notice in notices.slice(0, 3)" 
               :key="notice.id" 
               :to="`/notices/${notice.id}`"
               class="block group"
             >
               <div class="flex items-start gap-2">
                 <span v-if="notice.isPinned" class="badge badge-error badge-xs mt-1">置顶</span>
                 <span class="text-sm text-slate-600 group-hover:text-primary transition-colors line-clamp-1 flex-1">
                   {{ notice.title }}
                 </span>
                 <span class="text-xs text-slate-400 shrink-0">{{ formatDate(notice.publishedAt) }}</span>
               </div>
             </router-link>
           </div>
           <div v-else class="text-sm text-slate-500 text-center py-2">暂无公告</div>
        </div>
      </div>

      <!-- School Calendar -->
      <div class="card bg-base-100 shadow-sm border border-base-200 mt-6">
        <div class="card-body p-5">
           <div class="flex items-center gap-2 mb-4">
             <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-purple-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
               <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
             </svg>
             <h3 class="font-bold text-slate-800">校历周次</h3>
           </div>
           <div class="text-center py-4 bg-purple-50 rounded-xl">
             <div class="text-xs text-purple-600 mb-1">2025-2026学年 秋季学期</div>
             <div class="text-3xl font-black text-purple-600">第 12 周</div>
             <div class="text-xs text-purple-400 mt-1">考试周倒计时: 4 周</div>
           </div>
        </div>
      </div>
  </teleport>

</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { getPublicNotices, type NoticeVO } from '@/api/system'

const mounted = ref(false)
const notices = ref<NoticeVO[]>([])

onMounted(async () => {
  mounted.value = true
  await loadNotices()
})

const loadNotices = async () => {
  try {
    const res = await getPublicNotices(50)
    const list = normalizeNoticeList(res)
    notices.value = list.slice(0, 3)
  } catch (e) {
    console.error('Failed to load notices', e)
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const normalizeNoticeList = (res: any): NoticeVO[] => {
  if (Array.isArray(res)) return res
  if (res?.records && Array.isArray(res.records)) return res.records
  if (res?.data && Array.isArray(res.data)) return res.data
  return []
}

// Icons as components
const HeartIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" })
  ])
}

const ChatIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" })
  ])
}

const QuestionIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" })
  ])
}

const TagIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" })
  ])
}

const SearchIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" })
  ])
}

const categories = [
  { id: 1, name: '表白墙', desc: '勇敢表达爱', icon: HeartIcon, bgClass: 'bg-pink-50', textClass: 'text-pink-500', path: '/confessions' },
  { id: 2, name: '树洞', desc: '倾诉秘密', icon: ChatIcon, bgClass: 'bg-green-50', textClass: 'text-green-500', path: '/treehole' },
  { id: 3, name: '求助问答', desc: '互助成长', icon: QuestionIcon, bgClass: 'bg-blue-50', textClass: 'text-blue-500', path: '/help' },
  { id: 4, name: '跳蚤市场', desc: '旧物新生', icon: TagIcon, bgClass: 'bg-orange-50', textClass: 'text-orange-500', path: '/market' },
  { id: 5, name: '失物招领', desc: '传递温暖', icon: SearchIcon, bgClass: 'bg-purple-50', textClass: 'text-purple-500', path: '/lost-found' },
]

</script>
