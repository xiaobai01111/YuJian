<template>
  <div class="flex flex-col space-y-6">
    <!-- Section Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-1.5 h-6 bg-emerald-500 rounded-full"></div>
        <h2 class="text-xl font-bold text-slate-800">树洞</h2>
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
                ? 'bg-white text-emerald-500 shadow-sm' 
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            {{ tab.label }}
          </button>
        </div>

        <button class="btn btn-primary btn-sm btn-circle h-8 w-8 min-h-0 bg-emerald-500 hover:bg-emerald-600 border-none text-white shadow-sm shadow-emerald-200 tooltip tooltip-bottom" data-tip="发布心声" @click="openPublish">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Post List -->
    <div class="flex flex-col space-y-4">
      <div v-if="loading && postList.length === 0" class="text-center py-8">
        <span class="loading loading-spinner loading-lg text-emerald-500"></span>
      </div>
      
      <div v-else-if="postList.length === 0" class="bg-base-100 border border-dashed border-emerald-200 rounded-2xl min-h-[300px] flex flex-col items-center justify-center text-center p-8">
        <div class="w-20 h-20 bg-emerald-50 rounded-full flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-emerald-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
        </div>
        <h3 class="text-slate-900 font-medium mb-1">树洞空空如也</h3>
        <p class="text-slate-500 text-sm mb-4">有什么想说的？在这里倾诉吧</p>
        <button class="btn btn-primary btn-sm" @click="openPublish">发布心声</button>
      </div>

      <div v-else v-for="post in postList" :key="post.id" 
        class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer"
        @click="openPostDetail(post.id)">
        <div class="card-body p-5">
          <div class="flex gap-4">
            <!-- Anonymous Avatar -->
            <div class="avatar placeholder flex-shrink-0">
              <div class="bg-emerald-100 text-emerald-600 rounded-full w-11 h-11">
                <span class="text-lg">🌳</span>
              </div>
            </div>
            
            <div class="flex-1 min-w-0">
              <!-- Author & Time -->
              <div class="flex items-center gap-2 mb-2">
                <span class="font-medium text-slate-800">树洞{{ post.id }}</span>
                <span class="text-xs text-slate-400">{{ formatDate(post.createdAt) }}</span>
              </div>
              
              <!-- Content -->
              <p class="text-slate-700 text-sm leading-relaxed line-clamp-4 mb-3">{{ post.content }}</p>
              
              <!-- Stats -->
              <div class="flex items-center gap-5 text-xs text-slate-400">
                <span class="flex items-center gap-1 hover:text-emerald-500 transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
                  </svg>
                  {{ post.likeCount || 0 }} 抱抱
                </span>
                <span class="flex items-center gap-1 hover:text-blue-500 transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  {{ post.commentCount || 0 }} 回应
                </span>
              </div>
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
    <!-- Community Rules -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
          <h3 class="font-bold text-slate-800">树洞守则</h3>
        </div>
        <ul class="text-sm text-slate-600 space-y-2">
          <li class="flex items-start gap-2">
            <span class="text-emerald-400">✓</span>
            <span>所有发言均为匿名</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-emerald-400">✓</span>
            <span>尊重他人，友善评论</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-emerald-400">✓</span>
            <span>禁止人身攻击和恶意言论</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-emerald-400">✓</span>
            <span>保护隐私，不泄露他人信息</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- Mood Tags -->
    <div class="card bg-base-100 shadow-sm border border-base-200 mt-6">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <h3 class="font-bold text-slate-800">心情标签</h3>
        </div>
        <div class="flex flex-wrap gap-2">
          <span class="badge badge-ghost">😊 开心</span>
          <span class="badge badge-ghost">😢 难过</span>
          <span class="badge badge-ghost">😤 吐槽</span>
          <span class="badge badge-ghost">🤔 困惑</span>
          <span class="badge badge-ghost">💪 励志</span>
          <span class="badge badge-ghost">💕 暗恋</span>
        </div>
      </div>
    </div>
  </teleport>

  <PostPublishModal v-model="showPublish" :default-boards="['treehole']" @success="fetchData" />
  <PostDetailModal v-model="showPostDetail" :postId="selectedPostId" @updated="fetchData" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getPostList, type PostVO, type PostQueryDTO } from '@/api/post'
import PostPublishModal from '@/components/post/PostPublishModal.vue'
import PostDetailModal from '@/components/post/PostDetailModal.vue'

const mounted = ref(false)
const showPublish = ref(false)
const showPostDetail = ref(false)
const selectedPostId = ref<number | null>(null)

const tabs = [
  { label: '最新', value: 'latest' },
  { label: '最热', value: 'hot' },
  { label: '待回应', value: 'pending' }
]
const activeTab = ref('latest')

const postList = ref<PostVO[]>([])
const loading = ref(false)

const queryParams = reactive<PostQueryDTO>({
  page: 1,
  size: 10,
  board: 'treehole'
})

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getPostList(queryParams)
    postList.value = res.records || []
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

const openPublish = () => {
  showPublish.value = true
}

const openPostDetail = (id: number) => {
  selectedPostId.value = id
  showPostDetail.value = true
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
