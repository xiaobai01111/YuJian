<template>
  <div class="flex flex-col space-y-6">
    <!-- Section Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-1.5 h-6 bg-blue-500 rounded-full"></div>
        <h2 class="text-xl font-bold text-slate-800">求助问答</h2>
      </div>
      
      <div class="flex items-center gap-3">
        <!-- Tabs -->
        <div class="flex bg-slate-100 rounded-lg p-1">
          <button 
            v-for="tab in tabs" 
            :key="tab.value" 
            @click="activeTab = tab.value"
            :class="[
              'px-3 py-1 text-xs font-medium rounded-md transition-all duration-200',
              activeTab === tab.value 
                ? 'bg-white text-blue-500 shadow-sm' 
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            {{ tab.label }}
          </button>
        </div>

        <router-link to="/publish" class="btn btn-primary btn-sm btn-circle h-8 w-8 min-h-0 bg-blue-500 hover:bg-blue-600 border-none text-white shadow-sm shadow-blue-200 tooltip tooltip-bottom" data-tip="发起求助">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
        </router-link>
      </div>
    </div>

    <!-- Post List -->
    <div class="flex flex-col space-y-4">
      <div v-if="loading && postList.length === 0" class="text-center py-8">
        <span class="loading loading-spinner loading-lg text-blue-500"></span>
      </div>
      
      <div v-else-if="postList.length === 0" class="bg-base-100 border border-dashed border-blue-200 rounded-2xl min-h-[300px] flex flex-col items-center justify-center text-center p-8">
        <div class="w-20 h-20 bg-blue-50 rounded-full flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-blue-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h3 class="text-slate-900 font-medium mb-1">暂无求助</h3>
        <p class="text-slate-500 text-sm mb-4">遇到问题？在这里寻求帮助吧</p>
        <router-link to="/publish" class="btn btn-primary btn-sm">发起求助</router-link>
      </div>

      <div v-else v-for="post in postList" :key="post.id" 
        class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer"
        @click="goToDetail(post.id)">
        <div class="card-body p-5">
          <!-- Status Badge -->
          <div class="flex items-start justify-between mb-2">
            <div class="badge" :class="post.status === 1 ? 'badge-success' : 'badge-warning'">
              {{ post.status === 1 ? '已解决' : '待解答' }}
            </div>
            <span class="text-xs text-slate-400">{{ formatDate(post.createdAt) }}</span>
          </div>
          
          <!-- Title -->
          <h3 class="font-bold text-slate-800 text-lg mb-2 line-clamp-1">{{ post.title || '求助问题' }}</h3>
          
          <!-- Content -->
          <p class="text-slate-600 text-sm leading-relaxed line-clamp-2 mb-3">{{ post.content }}</p>
          
          <!-- Tags -->
          <div class="flex flex-wrap gap-2 mb-3">
            <span class="badge badge-ghost badge-sm">学习</span>
          </div>
          
          <!-- Footer -->
          <div class="flex items-center justify-between text-xs text-slate-400 pt-3 border-t border-base-200">
            <div class="flex items-center gap-2">
              <div class="avatar placeholder">
                <div class="bg-blue-100 text-blue-600 rounded-full w-6 h-6">
                  <span class="text-xs">{{ post.isAnonymous ? '?' : (post.author?.nickname?.[0] || 'U') }}</span>
                </div>
              </div>
              <span>{{ post.isAnonymous ? '匿名用户' : (post.author?.nickname || '用户') }}</span>
            </div>
            <div class="flex items-center gap-4">
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
                {{ post.commentCount || 0 }} 回答
              </span>
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                {{ post.viewCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="postList.length > 0" class="flex justify-center mt-6">
        <div class="join">
          <button class="join-item btn btn-sm" :disabled="queryParams.page <= 1" @click="changePage(queryParams.page - 1)">«</button>
          <button class="join-item btn btn-sm btn-active">{{ queryParams.page }}</button>
          <button class="join-item btn btn-sm" :disabled="postList.length < queryParams.size" @click="changePage(queryParams.page + 1)">»</button>
        </div>
      </div>
    </div>
  </div>

  <!-- Sidebar -->
  <teleport to="#sidebar-slot-target" v-if="mounted">
    <!-- Quick Categories -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
          </svg>
          <h3 class="font-bold text-slate-800">问题分类</h3>
        </div>
        <div class="flex flex-wrap gap-2">
          <button class="btn btn-ghost btn-xs">📚 学习</button>
          <button class="btn btn-ghost btn-xs">🏠 生活</button>
          <button class="btn btn-ghost btn-xs">💼 就业</button>
          <button class="btn btn-ghost btn-xs">💻 技术</button>
          <button class="btn btn-ghost btn-xs">🎯 考证</button>
          <button class="btn btn-ghost btn-xs">🤝 社交</button>
        </div>
      </div>
    </div>

    <!-- Active Helpers -->
    <div class="card bg-base-100 shadow-sm border border-base-200 mt-6">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
          </svg>
          <h3 class="font-bold text-slate-800">热心帮主</h3>
        </div>
        <div class="text-sm text-slate-500 text-center py-4">
          暂无数据
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPostList, type PostVO, type PostQueryDTO } from '@/api/post'

const router = useRouter()
const mounted = ref(false)

const tabs = [
  { label: '最新', value: 'latest' },
  { label: '待解答', value: 'pending' },
  { label: '已解决', value: 'solved' },
  { label: '悬赏', value: 'reward' }
]
const activeTab = ref('latest')

const postList = ref<PostVO[]>([])
const loading = ref(false)

const queryParams = reactive<PostQueryDTO>({
  page: 1,
  size: 10,
  board: 'help'
})

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getPostList(queryParams)
    postList.value = res.rows || []
  } catch (error) {
    console.error('Failed to fetch posts', error)
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  queryParams.page = page
  fetchData()
}

const goToDetail = (id: number) => {
  router.push(`/posts/${id}`)
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  
  return date.toLocaleDateString()
}

onMounted(() => {
  mounted.value = true
  fetchData()
})
</script>
