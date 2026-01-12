<template>
  <div v-if="post" class="max-w-4xl mx-auto">
    <!-- Breadcrumbs -->
    <div class="text-sm breadcrumbs mb-6 text-base-content/60">
      <ul>
        <li><router-link to="/">首页</router-link></li>
        <li><router-link :to="getBoardPath(post.board)">{{ getBoardName(post.board) }}</router-link></li>
        <li>帖子详情</li>
      </ul>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Main Content -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Post Card -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body p-6 md:p-8">
            <!-- Header -->
            <div class="flex items-center gap-3 mb-6">
              <div class="avatar placeholder">
                <div class="bg-neutral-focus text-neutral-content rounded-full w-10 h-10">
                  <span class="text-lg">{{ post.isAnonymous ? '?' : (post.author?.nickname?.[0] || 'U') }}</span>
                </div>
              </div>
              <div>
                <div class="font-bold text-base-content">
                  {{ post.isAnonymous ? '匿名用户' : (post.author?.nickname || post.author?.username || '用户') }}
                </div>
                <div class="text-xs text-base-content/60">
                  {{ formatDate(post.createdAt) }} · {{ post.viewCount || 0 }} 阅读
                </div>
              </div>
              <div class="ml-auto">
                <div class="badge badge-ghost">{{ getBoardName(post.board) }}</div>
              </div>
            </div>

            <!-- Title & Content -->
            <h1 class="text-2xl font-bold mb-4 text-base-content">{{ post.title }}</h1>
            <div class="prose max-w-none text-base-content/80 whitespace-pre-wrap leading-relaxed">
              {{ post.content }}
            </div>

            <!-- Images -->
            <div v-if="post.files && post.files.length > 0" class="mt-6 grid gap-2" :class="getImageGridClass(post.files.length)">
              <div v-for="file in post.files" :key="file.id" class="relative group cursor-pointer" @click="openImage(file.url)">
                <img :src="file.url" class="rounded-xl w-full h-full object-cover max-h-96 hover:opacity-95 transition-opacity" loading="lazy" />
              </div>
            </div>

            <!-- Actions -->
            <div class="flex items-center justify-between mt-8 pt-6 border-t border-base-200">
              <div class="flex gap-4">
                <button 
                  class="btn btn-sm gap-2" 
                  :class="post.isLiked ? 'btn-primary' : 'btn-ghost'"
                  @click="handleLike"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" :fill="post.isLiked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                  {{ post.likeCount || 0 }}
                </button>
                <button class="btn btn-sm btn-ghost gap-2">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  {{ post.commentCount || 0 }}
                </button>
              </div>
              
              <button 
                class="btn btn-sm btn-ghost btn-square" 
                :class="post.isBookmarked ? 'text-warning' : ''"
                @click="handleBookmark"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" :fill="post.isBookmarked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Comments Section (Placeholder) -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body p-6">
            <h3 class="font-bold text-lg mb-4">评论 ({{ post.commentCount || 0 }})</h3>
            <div class="flex gap-4 mb-6">
              <div class="avatar placeholder">
                <div class="bg-neutral-focus text-neutral-content rounded-full w-8 h-8">
                  <span class="text-xs">我</span>
                </div>
              </div>
              <div class="flex-1">
                <textarea class="textarea textarea-bordered w-full h-24" placeholder="写下你的评论..."></textarea>
                <div class="flex justify-end mt-2">
                  <button class="btn btn-primary btn-sm">发表评论</button>
                </div>
              </div>
            </div>
            
            <div class="text-center py-8 text-base-content/60">
              暂无评论，抢沙发！
            </div>
          </div>
        </div>
      </div>

      <!-- Sidebar -->
      <div class="hidden lg:block">
        <div class="card bg-base-100 shadow-sm border border-base-200 sticky top-24">
          <div class="card-body">
            <h3 class="card-title text-base mb-4">关于作者</h3>
            <div class="flex items-center gap-3 mb-4">
              <div class="avatar placeholder">
                <div class="bg-neutral-focus text-neutral-content rounded-full w-12 h-12">
                  <span class="text-xl">{{ post.isAnonymous ? '?' : (post.author?.nickname?.[0] || 'U') }}</span>
                </div>
              </div>
              <div>
                <div class="font-bold">{{ post.isAnonymous ? '匿名用户' : (post.author?.nickname || post.author?.username || '用户') }}</div>
                <div class="text-xs text-base-content/60">加入于 {{ formatDate(post.author?.createdAt || post.createdAt) }}</div>
              </div>
            </div>
            <button class="btn btn-outline btn-sm w-full" :disabled="post.isAnonymous">查看主页</button>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <div v-else-if="loading" class="flex justify-center py-20">
    <span class="loading loading-spinner loading-lg"></span>
  </div>
  
  <div v-else class="text-center py-20 text-base-content/60">
    <h3 class="text-xl font-bold">帖子不存在或已被删除</h3>
    <button class="btn btn-primary btn-sm mt-4" @click="$router.push('/')">返回首页</button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPostDetail, likePost, unlikePost, bookmarkPost, unbookmarkPost, type PostVO } from '@/api/post'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const post = ref<PostVO | null>(null)
const loading = ref(false)

const boardNameMap: Record<string, string> = {
  'Confessions': '表白墙',
  'TreeHole': '树洞',
  'Help': '求助问答',
  'Market': '跳蚤市场',
  'LostFound': '失物招领'
}

onMounted(() => {
  fetchDetail()
})

const fetchDetail = async () => {
  const id = Number(route.params.id)
  if (!id) return
  
  loading.value = true
  try {
    const res: any = await getPostDetail(id)
    post.value = res
  } catch (error) {
    console.error('Failed to fetch post detail', error)
  } finally {
    loading.value = false
  }
}

const handleLike = async () => {
  if (!post.value) return
  if (!userStore.token) {
    // Should prompt login
    alert('请先登录')
    return
  }
  
  try {
    if (post.value.isLiked) {
      await unlikePost(post.value.id)
      post.value.likeCount--
      post.value.isLiked = false
    } else {
      await likePost(post.value.id)
      post.value.likeCount++
      post.value.isLiked = true
    }
  } catch (error) {
    console.error(error)
  }
}

const handleBookmark = async () => {
  if (!post.value) return
  if (!userStore.token) {
    alert('请先登录')
    return
  }
  
  try {
    if (post.value.isBookmarked) {
      await unbookmarkPost(post.value.id)
      post.value.isBookmarked = false
    } else {
      await bookmarkPost(post.value.id)
      post.value.isBookmarked = true
    }
  } catch (error) {
    console.error(error)
  }
}

const getBoardName = (board: string) => {
  return boardNameMap[board] || board
}

const getBoardPath = (board: string) => {
  // Simple mapping, assumes route name matches board key but lowercase or dashed
  // Our routes are like /confessions, /treehole (camelCase or just lowercase?)
  // Route definitions: /confessions, /treehole, /lost-found
  // Board keys: 'Confessions', 'TreeHole', 'LostFound'
  // Need to map correctly
  const map: Record<string, string> = {
    'Confessions': '/confessions',
    'TreeHole': '/treehole',
    'Help': '/help',
    'Market': '/market',
    'LostFound': '/lost-found'
  }
  return map[board] || '/'
}

const formatDate = (dateStr: string | undefined) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}

const getImageGridClass = (count: number) => {
  if (count === 1) return 'grid-cols-1'
  if (count === 2) return 'grid-cols-2'
  return 'grid-cols-3'
}

const openImage = (url: string) => {
  window.open(url, '_blank')
}
</script>
