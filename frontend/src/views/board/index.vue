<template>
  <div>
    <div class="text-sm breadcrumbs mb-4 text-slate-500">
      <ul>
        <li><router-link to="/">首页</router-link></li>
        <li>{{ route.name }}</li>
      </ul>
    </div>
    
    <div class="flex items-center gap-4 mb-8">
      <div class="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
         <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      </div>
      <div>
        <h1 class="text-2xl font-bold text-slate-800">{{ title }}</h1>
        <p class="text-slate-500 text-sm mt-1">浏览所有{{ title }}相关的话题</p>
      </div>
    </div>

    <!-- Placeholder Content -->
    <div class="grid gap-4">
      <div v-for="i in 5" :key="i" class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200">
        <div class="card-body">
          <h2 class="card-title text-base">{{ title }} 示例帖子 #{{ i }}</h2>
          <p class="text-slate-600 text-sm">这是一个关于{{ title }}的示例内容。这里将展示用户发布的内容详情。</p>
          <div class="card-actions justify-end mt-2 text-xs text-slate-400">
            <span>10 分钟前</span>
            <span>•</span>
            <span>23 评论</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const titleMap: Record<string, string> = {
  'Confessions': '表白墙',
  'TreeHole': '树洞',
  'Help': '求助问答',
  'Market': '跳蚤市场',
  'LostFound': '失物招领'
}

const title = computed(() => titleMap[route.name as string] || '板块')
</script>
