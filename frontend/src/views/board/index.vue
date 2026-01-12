<template>
  <div>
    <div class="text-sm breadcrumbs mb-4 text-base-content/60">
      <ul>
        <li><router-link to="/">首页</router-link></li>
        <li>{{ title }}</li>
      </ul>
    </div>
    
    <div class="flex items-center gap-4 mb-8">
      <div class="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
         <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      </div>
      <div>
        <h1 class="text-2xl font-bold text-base-content">{{ title }}</h1>
        <p class="text-base-content/60 text-sm mt-1">浏览所有{{ title }}相关的话题</p>
      </div>
    </div>

    <!-- Post List -->
    <div class="flex flex-col space-y-4">
      <div v-if="loading && postList.length === 0" class="text-center py-8 text-base-content/60">
        <span class="loading loading-spinner loading-lg"></span>
      </div>
      
      <div v-else-if="postList.length === 0" class="text-center py-12 border border-dashed border-base-300 rounded-2xl bg-base-100">
        <p class="text-base-content/60">暂无内容，快来发布第一条帖子吧！</p>
        <button class="btn btn-primary btn-sm mt-4">发布帖子</button>
      </div>

      <div v-else v-for="post in postList" :key="post.id" class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer" @click="goToDetail(post.id)">
        <div class="card-body p-6">
          <div class="flex justify-between items-start">
            <h2 class="card-title text-lg font-bold text-base-content mb-2 line-clamp-1">{{ post.title }}</h2>
            <div class="badge badge-ghost badge-sm" v-if="post.category">{{ post.category }}</div>
          </div>
          
          <p class="text-base-content/70 text-sm line-clamp-2 mb-4">{{ post.content }}</p>
          
          <!-- Image Preview (if any) -->
          <div v-if="post.files && post.files.length > 0" class="flex gap-2 mb-4 overflow-x-auto pb-2">
            <img 
              v-for="file in post.files.slice(0, 3)" 
              :key="file.id" 
              :src="file.url" 
              class="w-24 h-24 object-cover rounded-lg border border-base-200" 
              alt="attachment" 
            />
            <div v-if="post.files.length > 3" class="w-24 h-24 bg-base-200 rounded-lg flex items-center justify-center text-base-content/50 text-xs">
              +{{ post.files.length - 3 }}
            </div>
          </div>

          <div class="flex items-center justify-between text-xs text-base-content/50">
            <div class="flex items-center gap-3">
              <div class="flex items-center gap-1">
                <div class="avatar placeholder">
                  <div class="bg-neutral-focus text-neutral-content rounded-full w-5 h-5">
                    <span class="text-[10px]">{{ post.isAnonymous ? '?' : (post.author?.nickname?.[0] || 'U') }}</span>
                  </div>
                </div>
                <span>{{ post.isAnonymous ? '匿名用户' : (post.author?.nickname || post.author?.username || '用户') }}</span>
              </div>
              <span>{{ formatDate(post.createdAt) }}</span>
            </div>
            
            <div class="flex items-center gap-4">
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                {{ post.viewCount || 0 }}
              </span>
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                {{ post.likeCount || 0 }}
              </span>
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
                {{ post.commentCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="postList.length > 0" class="flex justify-center mt-8">
        <div class="join">
          <button class="join-item btn btn-sm" :disabled="queryParams.page <= 1" @click="changePage(queryParams.page - 1)">«</button>
          <button class="join-item btn btn-sm">Page {{ queryParams.page }}</button>
          <button class="join-item btn btn-sm" :disabled="postList.length < queryParams.size" @click="changePage(queryParams.page + 1)">»</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPostList, type PostVO, type PostQueryDTO } from '@/api/post'

const route = useRoute()
const router = useRouter()

const titleMap: Record<string, string> = {
  'Confessions': '表白墙',
  'TreeHole': '树洞',
  'Help': '求助问答',
  'Market': '跳蚤市场',
  'LostFound': '失物招领'
}

const title = computed(() => titleMap[route.name as string] || '板块')

const postList = ref<PostVO[]>([])
const loading = ref(false)

const queryParams = reactive<PostQueryDTO>({
  page: 1,
  size: 10,
  board: route.name as string // Initial board
})

const fetchData = async () => {
  loading.value = true
  try {
    // Map route name to board key if needed, assuming backend uses same keys or we map them
    // Assuming backend accepts 'Confessions', 'TreeHole', etc. directly.
    queryParams.board = route.name as string
    
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
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// Watch for route changes to re-fetch data (e.g. switching from Confessions to TreeHole)
watch(() => route.name, () => {
  queryParams.page = 1
  postList.value = [] // clear old data
  fetchData()
})

onMounted(() => {
  fetchData()
})
</script>
