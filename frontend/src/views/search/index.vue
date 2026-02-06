<template>
  <div class="max-w-4xl mx-auto">
    <!-- Search Bar -->
    <div class="card bg-base-100 shadow-sm border border-base-200 mb-8">
      <div class="card-body">
        <div class="join w-full">
          <input 
            v-model="keyword" 
            class="input input-bordered join-item w-full" 
            placeholder="搜索感兴趣的话题、帖子..." 
            @keyup.enter="handleSearch"
          />
          <button 
            class="btn btn-primary join-item" 
            @click="handleSearch" 
            :disabled="loading"
          >
            <span v-if="loading" class="loading loading-spinner"></span>
            搜索
          </button>
        </div>
        
        <!-- Quick Tags -->
        <div class="mt-4 flex gap-2 flex-wrap">
          <span class="text-sm text-base-content/60 mr-2">热门搜索:</span>
          <button class="badge badge-ghost hover:badge-primary cursor-pointer" @click="quickSearch('期末考试')">期末考试</button>
          <button class="badge badge-ghost hover:badge-primary cursor-pointer" @click="quickSearch('二手教材')">二手教材</button>
          <button class="badge badge-ghost hover:badge-primary cursor-pointer" @click="quickSearch('失物招领')">失物招领</button>
          <button class="badge badge-ghost hover:badge-primary cursor-pointer" @click="quickSearch('考研资料')">考研资料</button>
        </div>
      </div>
    </div>

    <!-- Search Results -->
    <div v-if="hasSearched">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-bold">搜索结果</h2>
        <span class="text-sm text-base-content/60">找到 {{ total }} 条相关内容</span>
      </div>

      <div class="space-y-4">
        <div v-if="loading" class="text-center py-12">
          <span class="loading loading-spinner loading-lg"></span>
        </div>
        
        <div v-else-if="postList.length === 0" class="text-center py-12 bg-base-100 rounded-xl border border-base-200">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto text-base-content/20 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <p class="text-base-content/60">未找到相关内容，换个关键词试试？</p>
        </div>

        <div v-else v-for="post in postList" :key="post.id" class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer" @click="goToDetail(post.id)">
          <div class="card-body p-6">
            <div class="flex justify-between items-start gap-3">
            <h3 class="card-title text-lg font-bold text-base-content mb-2 min-w-0 flex-1">
              <span class="line-clamp-1 break-words min-w-0 block">
                <template v-for="(part, index) in highlightParts(post.title)" :key="index">
                  <span v-if="part.match" class="bg-warning/30 text-warning-content font-bold px-1 rounded">{{ part.text }}</span>
                  <span v-else>{{ part.text }}</span>
                </template>
              </span>
            </h3>
              <div class="flex flex-wrap gap-2">
                <div v-for="board in getPostBoards(post)" :key="board" class="badge badge-ghost badge-sm">
                  {{ getBoardLabel(board) }}
                </div>
              </div>
            </div>
            <p class="text-base-content/70 text-sm line-clamp-2 mb-2 break-words">
              <template v-for="(part, index) in highlightParts(post.content)" :key="index">
                <span v-if="part.match" class="bg-warning/30 text-warning-content font-bold px-1 rounded">{{ part.text }}</span>
                <span v-else>{{ part.text }}</span>
              </template>
            </p>
            
            <div class="flex items-center justify-between text-xs text-base-content/50 mt-2">
              <span>{{ formatDate(post.createdAt) }}</span>
              <div class="flex gap-3">
                <span>{{ post.viewCount }} 阅读</span>
                <button :class="post.isLiked ? 'text-pink-500' : ''" @click.stop="toggleLike(post)">{{ post.likeCount }} 点赞</button>
                <span>{{ post.commentCount }} 评论</span>
              </div>
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPostList, likePost, unlikePost, type PostVO, type PostQueryDTO } from '@/api/post'
import { getBoardLabel, getPostBoards } from '@/utils/boards'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const dialog = useDialog()

const keyword = ref('')
const loading = ref(false)
const hasSearched = ref(false)
const postList = ref<PostVO[]>([])
const total = ref(0)

const queryParams = reactive<PostQueryDTO>({
  page: 1,
  size: 10,
  keyword: ''
})

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

onMounted(() => {
  if (route.query.q) {
    keyword.value = route.query.q as string
    handleSearch()
  }
})

const handleSearch = async () => {
  if (!keyword.value.trim()) return
  
  // Update URL query
  router.replace({ query: { ...route.query, q: keyword.value } })
  
  loading.value = true
  hasSearched.value = true
  queryParams.keyword = keyword.value
  queryParams.page = 1
  
  try {
    const res = await getPostList(queryParams)
    postList.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const quickSearch = (kw: string) => {
  keyword.value = kw
  handleSearch()
}

const changePage = (page: number) => {
  queryParams.page = page
  void handleSearch()
}

const goToDetail = (id: number) => {
  router.push(`/posts/${id}`)
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

type HighlightPart = { text: string; match: boolean }

const highlightParts = (text?: string): HighlightPart[] => {
  const raw = text ?? ''
  const kw = keyword.value.trim()
  if (!kw) return [{ text: raw, match: false }]

  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(escaped, 'gi')
  const parts: HighlightPart[] = []
  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = regex.exec(raw)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ text: raw.slice(lastIndex, match.index), match: false })
    }
    parts.push({ text: match[0], match: true })
    lastIndex = match.index + match[0].length
    if (match[0].length === 0) {
      regex.lastIndex++
    }
  }

  if (lastIndex < raw.length) {
    parts.push({ text: raw.slice(lastIndex), match: false })
  }

  return parts.length ? parts : [{ text: raw, match: false }]
}
</script>
