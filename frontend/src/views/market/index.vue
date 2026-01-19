<template>
  <div class="flex flex-col space-y-6">
    <!-- Section Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-1.5 h-6 bg-orange-500 rounded-full"></div>
        <h2 class="text-xl font-bold text-slate-800">跳蚤市场</h2>
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
                ? 'bg-white text-orange-500 shadow-sm' 
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            {{ tab.label }}
          </button>
        </div>

        <button class="btn btn-primary btn-sm btn-circle h-8 w-8 min-h-0 bg-orange-500 hover:bg-orange-600 border-none text-white shadow-sm shadow-orange-200 tooltip tooltip-bottom" data-tip="发布闲置" @click="openPublish">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Item Grid -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-if="loading && postList.length === 0" class="col-span-full text-center py-8">
        <span class="loading loading-spinner loading-lg text-orange-500"></span>
      </div>
      
      <div v-else-if="postList.length === 0" class="col-span-full bg-base-100 border border-dashed border-orange-200 rounded-2xl min-h-[300px] flex flex-col items-center justify-center text-center p-8">
        <div class="w-20 h-20 bg-orange-50 rounded-full flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-orange-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
          </svg>
        </div>
        <h3 class="text-slate-900 font-medium mb-1">暂无闲置</h3>
        <p class="text-slate-500 text-sm mb-4">有闲置物品？发布出来让它找到新主人</p>
        <button class="btn btn-primary btn-sm" @click="openPublish">发布闲置</button>
      </div>

      <!-- Item Cards -->
      <div v-else v-for="post in postList" :key="post.id" 
        class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer group"
        @click="openPostDetail(post.id)">
        <!-- Image -->
        <figure class="relative aspect-square bg-base-200">
          <img v-if="post.files && post.files.length > 0" :src="resolveFileUrl(post.files[0]?.url)" 
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" alt="" />
          <div v-else class="w-full h-full flex items-center justify-center text-slate-300">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
          <!-- Status Badge -->
          <div class="absolute top-2 left-2">
            <span class="badge badge-sm" :class="post.status === 1 ? 'badge-error' : 'badge-success'">
              {{ post.status === 1 ? '已售' : '在售' }}
            </span>
          </div>
        </figure>
        
        <div class="card-body p-3">
          <!-- Title -->
          <h3 class="font-medium text-slate-800 text-sm line-clamp-2 leading-snug">{{ post.title || post.content }}</h3>
          
          <!-- Price -->
          <div class="flex items-baseline gap-1 mt-1">
            <span class="text-orange-500 font-bold text-lg">¥{{ post.price || '面议' }}</span>
            <span v-if="post.originalPrice" class="text-xs text-slate-400 line-through">¥{{ post.originalPrice }}</span>
          </div>
          
          <!-- Footer -->
          <div class="flex items-center justify-between text-xs text-slate-400 mt-2 pt-2 border-t border-base-200">
            <span>{{ post.author?.nickname || '卖家' }}</span>
            <span class="flex items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
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
    <div v-if="postList.length > 0" class="flex justify-center mt-8">
      <div class="join">
        <button class="join-item btn btn-sm" :disabled="queryParams.page <= 1" @click="changePage(queryParams.page - 1)">«</button>
        <button class="join-item btn btn-sm btn-active">{{ queryParams.page }}</button>
        <button class="join-item btn btn-sm" :disabled="postList.length < queryParams.size" @click="changePage(queryParams.page + 1)">»</button>
      </div>
    </div>
  </div>

  <!-- Sidebar -->
  <teleport to="#sidebar-slot-target" v-if="mounted">
    <!-- Categories -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-orange-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
          </svg>
          <h3 class="font-bold text-slate-800">商品分类</h3>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <button class="btn btn-ghost btn-sm justify-start">📚 书籍教材</button>
          <button class="btn btn-ghost btn-sm justify-start">💻 数码电子</button>
          <button class="btn btn-ghost btn-sm justify-start">👕 服饰鞋包</button>
          <button class="btn btn-ghost btn-sm justify-start">🎮 游戏娱乐</button>
          <button class="btn btn-ghost btn-sm justify-start">🏠 生活用品</button>
          <button class="btn btn-ghost btn-sm justify-start">🎁 其他</button>
        </div>
      </div>
    </div>

    <!-- Trading Tips -->
    <div class="card bg-base-100 shadow-sm border border-base-200 mt-6">
      <div class="card-body p-5">
        <div class="flex items-center gap-2 mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <h3 class="font-bold text-slate-800">交易提醒</h3>
        </div>
        <ul class="text-sm text-slate-600 space-y-2">
          <li class="flex items-start gap-2">
            <span class="text-orange-400">•</span>
            <span>建议当面交易，验货后付款</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-orange-400">•</span>
            <span>谨防诈骗，不轻信先付款</span>
          </li>
          <li class="flex items-start gap-2">
            <span class="text-orange-400">•</span>
            <span>贵重物品建议校内交易</span>
          </li>
        </ul>
      </div>
    </div>
  </teleport>

  <PostPublishModal v-model="showPublish" :default-boards="['market']" @success="fetchData" />
  <PostDetailModal v-model="showPostDetail" :postId="selectedPostId" @updated="fetchData" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getPostList, type PostVO, type PostQueryDTO } from '@/api/post'
import { resolveFileUrl } from '@/utils/file'
import PostPublishModal from '@/components/post/PostPublishModal.vue'
import PostDetailModal from '@/components/post/PostDetailModal.vue'

const mounted = ref(false)
const showPublish = ref(false)
const showPostDetail = ref(false)
const selectedPostId = ref<number | null>(null)

const tabs = [
  { label: '全部', value: 'all' },
  { label: '书籍', value: 'books' },
  { label: '数码', value: 'digital' },
  { label: '服饰', value: 'clothes' },
  { label: '生活', value: 'life' }
]
const activeTab = ref('all')

const postList = ref<PostVO[]>([])
const loading = ref(false)

const queryParams = reactive<PostQueryDTO>({
  page: 1,
  size: 12,
  board: 'market'
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

onMounted(() => {
  mounted.value = true
  fetchData()
})
</script>
