<template>
  <div class="flex flex-col space-y-6">
    <!-- Section Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-1.5 h-6 bg-purple-500 rounded-full"></div>
        <h2 class="text-xl font-bold text-slate-800">失物招领</h2>
      </div>
      
      <div class="flex items-center gap-3">
        <!-- Custom Tabs for Lost/Found -->
        <div class="flex bg-slate-100 rounded-lg p-1">
          <button 
            @click="postType = 'lost'"
            :class="[
              'px-3 py-1 text-xs font-medium rounded-md transition-all duration-200',
              postType === 'lost'
                ? 'bg-white text-purple-500 shadow-sm' 
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            寻物
          </button>
          <button 
            @click="postType = 'found'"
            :class="[
              'px-3 py-1 text-xs font-medium rounded-md transition-all duration-200',
              postType === 'found'
                ? 'bg-white text-purple-500 shadow-sm' 
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            招领
          </button>
        </div>

        <button class="btn btn-primary btn-sm btn-circle h-8 w-8 min-h-0 bg-purple-500 hover:bg-purple-600 border-none text-white shadow-sm shadow-purple-200 tooltip tooltip-bottom" :data-tip="postType === 'lost' ? '发布寻物' : '发布招领'" @click="openPublish">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Post List -->
    <div class="flex flex-col space-y-4">
      <div v-if="loading && postList.length === 0" class="text-center py-8">
        <span class="loading loading-spinner loading-lg text-purple-500"></span>
      </div>
      
      <div v-else-if="postList.length === 0" class="bg-base-100 border border-dashed border-purple-200 rounded-2xl min-h-[300px] flex flex-col items-center justify-center text-center p-8">
        <div class="w-20 h-20 bg-purple-50 rounded-full flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-purple-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        <h3 class="text-slate-900 font-medium mb-1">暂无{{ postType === 'lost' ? '寻物' : '招领' }}信息</h3>
        <p class="text-slate-500 text-sm mb-4">
          {{ postType === 'lost' ? '丢失了物品？发布寻物启事' : '捡到物品？发布招领信息' }}
        </p>
        <button class="btn btn-primary btn-sm" @click="openPublish">
          {{ postType === 'lost' ? '发布寻物' : '发布招领' }}
        </button>
      </div>

      <div v-else v-for="post in postList" :key="post.id" 
        class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer"
        @click="openPostDetail(post.id)">
        <div class="card-body p-5">
          <div class="flex gap-4">
            <!-- Image -->
            <div class="flex-shrink-0">
              <div class="w-24 h-24 rounded-xl bg-base-200 overflow-hidden">
                <img v-if="post.files && post.files.length > 0" :src="resolveFileUrl(post.files[0]?.url)" 
                  class="w-full h-full object-cover" alt="" />
                <div v-else class="w-full h-full flex items-center justify-center text-slate-300">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
              </div>
            </div>
            
            <div class="flex-1 min-w-0">
              <!-- Header -->
              <div class="flex items-start justify-between mb-2">
                <div class="flex items-center gap-2">
                  <span class="badge" :class="postType === 'lost' ? 'badge-error' : 'badge-success'">
                    {{ postType === 'lost' ? '寻物' : '招领' }}
                  </span>
                  <span v-if="post.status === POST_STATUS.RESOLVED" class="badge badge-success badge-sm">已找到</span>
                </div>
                <span class="text-xs text-slate-400">{{ formatDate(post.createdAt) }}</span>
              </div>
              
              <!-- Title -->
              <h3 class="font-bold text-slate-800 mb-2 truncate">{{ truncateText(post.title || '物品信息', 20) }}</h3>
              
              <!-- Content -->
              <p class="text-slate-600 text-sm line-clamp-2 mb-3 break-words">{{ truncateText(post.content, 100) }}</p>
              
              <!-- Info -->
              <div class="flex flex-wrap gap-3 text-xs text-slate-500">
                <span v-if="post.location" class="flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  {{ post.location }}
                </span>
                <span class="flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  {{ post.viewCount || 0 }} 浏览
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
    <!-- Quick Tips -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-purple-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <h3 class="font-bold text-slate-800">寻物小贴士</h3>
        </div>
        <ul class="text-sm text-slate-600 space-y-2">
          <li class="flex items-start gap-2">
            <span class="text-purple-400">1</span>
            <span>详细描述物品特征和丢失地点</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-purple-400">2</span>
            <span>上传物品照片增加辨识度</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-purple-400">3</span>
            <span>留下联系方式便于沟通</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-purple-400">4</span>
            <span>及时更新状态，找到后标记已解决</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- Common Locations -->
    <div class="card bg-base-100 shadow-sm border border-base-200 mt-6">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <h3 class="font-bold text-slate-800">常见地点</h3>
        </div>
        <div class="flex flex-wrap gap-2">
          <span class="badge badge-ghost cursor-pointer hover:badge-primary">图书馆</span>
          <span class="badge badge-ghost cursor-pointer hover:badge-primary">食堂</span>
          <span class="badge badge-ghost cursor-pointer hover:badge-primary">教学楼</span>
          <span class="badge badge-ghost cursor-pointer hover:badge-primary">体育馆</span>
          <span class="badge badge-ghost cursor-pointer hover:badge-primary">宿舍区</span>
          <span class="badge badge-ghost cursor-pointer hover:badge-primary">操场</span>
        </div>
      </div>
    </div>
  </teleport>

  <PostPublishModal v-model="showPublish" :default-boards="['lost-found']" @success="fetchData" />
  <PostDetailModal v-model="showPostDetail" :postId="selectedPostId" @updated="fetchData" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { getPostList, POST_STATUS, type PostVO, type PostQueryDTO } from '@/api/post'
import { resolveFileUrl } from '@/utils/file'
import PostPublishModal from '@/components/post/PostPublishModal.vue'
import PostDetailModal from '@/components/post/PostDetailModal.vue'

const mounted = ref(false)
const showPublish = ref(false)
const showPostDetail = ref(false)
const selectedPostId = ref<number | null>(null)

const postType = ref<'lost' | 'found'>('lost')

const postList = ref<PostVO[]>([])
const loading = ref(false)

const queryParams = reactive<PostQueryDTO>({
  page: 1,
  size: 10,
  board: 'lost-found'
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPostList({
      ...queryParams,
      lostFoundType: postType.value
    })
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

const openPostDetail = (id: number) => {
  selectedPostId.value = id
  showPostDetail.value = true
}

const openPublish = () => {
  showPublish.value = true
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

watch(postType, () => {
  queryParams.page = 1
  fetchData()
})

onMounted(() => {
  mounted.value = true
  fetchData()
})
</script>
