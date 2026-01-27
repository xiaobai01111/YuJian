<template>
  <div class="flex flex-col space-y-6">
    <!-- Section Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-1.5 h-6 bg-pink-500 rounded-full"></div>
        <h2 class="text-xl font-bold text-slate-800">表白墙</h2>
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
                ? 'bg-white text-pink-500 shadow-sm' 
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            {{ tab.label }}
          </button>
        </div>

        <button class="btn btn-primary btn-sm btn-circle h-8 w-8 min-h-0 bg-pink-500 hover:bg-pink-600 border-none text-white shadow-sm shadow-pink-200 tooltip tooltip-bottom" data-tip="发布表白" @click="openPublish">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Post List -->
    <div class="flex flex-col space-y-4">
      <div v-if="loading && postList.length === 0" class="text-center py-8">
        <span class="loading loading-spinner loading-lg text-pink-500"></span>
      </div>
      
      <div v-else-if="postList.length === 0" class="bg-base-100 border border-dashed border-pink-200 rounded-2xl min-h-[300px] flex flex-col items-center justify-center text-center p-8">
        <div class="w-20 h-20 bg-pink-50 rounded-full flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-pink-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
        </div>
        <h3 class="text-slate-900 font-medium mb-1">还没有表白</h3>
        <p class="text-slate-500 text-sm mb-4">勇敢迈出第一步，发布你的心意吧！</p>
        <button class="btn btn-primary btn-sm" @click="openPublish">发布表白</button>
      </div>

      <div v-else v-for="post in postList" :key="post.id" 
        class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer group"
        @click="openPostDetail(post.id)">
        <div class="card-body p-5">
          <div class="flex gap-4">
            <!-- Avatar -->
            <div class="avatar placeholder flex-shrink-0">
              <div class="bg-pink-100 text-pink-600 rounded-full w-11 h-11">
                <span class="text-lg">{{ post.isAnonymous ? '?' : (post.author?.nickname?.[0] || 'U') }}</span>
              </div>
            </div>
            
            <div class="flex-1 min-w-0">
              <!-- Author & Time -->
              <div class="flex items-center gap-2 mb-2">
                <span class="font-medium text-slate-800">{{ post.isAnonymous ? '匿名用户' : (post.author?.nickname || '用户') }}</span>
                <span class="text-xs text-slate-400">{{ formatDate(post.createdAt) }}</span>
                <span v-if="post.isAnonymous" class="badge badge-ghost badge-xs">匿名</span>
                <span v-if="post.status === 1" class="badge badge-success badge-sm">已解决</span>
              </div>
              
              <!-- Title -->
              <h3 v-if="post.title" class="font-bold text-slate-800 text-base mb-2 truncate">{{ truncateText(post.title, 20) }}</h3>
              
              <!-- Content -->
              <p class="text-slate-700 text-sm leading-relaxed line-clamp-3 mb-3 break-words">{{ truncateText(post.content, 100) }}</p>
              
              <!-- Images -->
              <div v-if="post.files && post.files.length > 0" class="flex gap-2 mb-3">
                <img v-for="file in post.files.slice(0, 3)" :key="file.id" :src="resolveFileUrl(file.url)" 
                  class="w-20 h-20 object-cover rounded-lg border border-base-200" alt="" />
                <div v-if="post.files.length > 3" class="w-20 h-20 bg-base-200 rounded-lg flex items-center justify-center text-slate-400 text-sm">
                  +{{ post.files.length - 3 }}
                </div>
              </div>
              
              <!-- Stats -->
              <div class="flex items-center gap-5 text-xs text-slate-400">
                <button class="flex items-center gap-1 hover:text-pink-500 transition-colors" :class="post.isLiked ? 'text-pink-500' : ''" @click.stop="toggleLike(post)" :aria-pressed="post.isLiked">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" :fill="post.isLiked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                  {{ post.likeCount || 0 }}
                </button>
                <span class="flex items-center gap-1 hover:text-blue-500 transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  {{ post.commentCount || 0 }}
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
    <!-- Love Tips -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-pink-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          <h3 class="font-bold text-slate-800">表白小贴士</h3>
        </div>
        <ul class="text-sm text-slate-600 space-y-2">
          <li class="flex items-start gap-2">
            <span class="text-pink-400">♥</span>
            <span>真诚是最好的告白方式</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-pink-400">♥</span>
            <span>尊重对方的选择和感受</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-pink-400">♥</span>
            <span>匿名表白也要注意分寸</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- Hot Confessions -->
    <div class="card bg-base-100 shadow-sm border border-base-200 mt-6">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
          </svg>
          <h3 class="font-bold text-slate-800">热门表白</h3>
        </div>
        <div class="text-sm text-slate-500 text-center py-4">
          暂无热门表白
        </div>
      </div>
    </div>
  </teleport>

  <PostPublishModal v-model="showPublish" :default-boards="['confessions']" @success="fetchData" />
  <PostDetailModal v-model="showPostDetail" :postId="selectedPostId" @updated="fetchData" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getPostList, likePost, unlikePost, type PostVO, type PostQueryDTO } from '@/api/post'
import { resolveFileUrl } from '@/utils/file'
import PostPublishModal from '@/components/post/PostPublishModal.vue'
import PostDetailModal from '@/components/post/PostDetailModal.vue'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const mounted = ref(false)
const showPublish = ref(false)
const showPostDetail = ref(false)
const selectedPostId = ref<number | null>(null)
const userStore = useUserStore()
const dialog = useDialog()

const tabs = [
  { label: '最新', value: 'latest' },
  { label: '最热', value: 'hot' },
  { label: '精华', value: 'featured' }
]
const activeTab = ref('latest')

const postList = ref<PostVO[]>([])
const loading = ref(false)

const queryParams = reactive<PostQueryDTO>({
  page: 1,
  size: 10,
  board: 'confessions'
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

const toggleLike = async (post: PostVO) => {
  if (!userStore.token) {
    await dialog.alert('请先登录')
    return
  }
  try {
    if (post.isLiked) {
      await unlikePost(post.id)
      post.likeCount = Math.max(0, (post.likeCount || 0) - 1)
      post.isLiked = false
    } else {
      await likePost(post.id)
      post.likeCount = (post.likeCount || 0) + 1
      post.isLiked = true
    }
  } catch (e) {
    console.error(e)
  }
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

const truncateText = (text: string | undefined, maxLength: number) => {
  if (!text) return ''
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}

onMounted(() => {
  mounted.value = true
  fetchData()
})
</script>
