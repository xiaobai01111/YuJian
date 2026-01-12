<template>
  <div class="max-w-4xl mx-auto">
    <div class="card bg-base-100 shadow-lg border border-base-200 mb-8">
      <div class="card-body">
        <div class="flex flex-col md:flex-row items-center gap-6">
          <div class="avatar placeholder">
            <div class="bg-neutral-focus text-neutral-content rounded-full w-24 h-24 text-3xl">
              <span>{{ userInfo?.nickname?.[0] || userInfo?.username?.[0] || 'U' }}</span>
            </div>
          </div>
          <div class="text-center md:text-left flex-1">
            <h1 class="text-2xl font-bold">{{ userInfo?.nickname || userInfo?.username }}</h1>
            <p class="text-base-content/60 mt-1">@{{ userInfo?.username }}</p>
            <div class="flex gap-4 mt-4 justify-center md:justify-start">
              <div class="text-center">
                <div class="font-bold text-lg">12</div>
                <div class="text-xs text-base-content/60">帖子</div>
              </div>
              <div class="text-center">
                <div class="font-bold text-lg">48</div>
                <div class="text-xs text-base-content/60">获赞</div>
              </div>
              <div class="text-center">
                <div class="font-bold text-lg">5</div>
                <div class="text-xs text-base-content/60">收藏</div>
              </div>
            </div>
          </div>
          <div>
            <button class="btn btn-outline btn-sm">编辑资料</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs tabs-boxed bg-base-100 p-1 mb-6 w-fit">
      <a class="tab" :class="{ 'tab-active': activeTab === 'posts' }" @click="activeTab = 'posts'">我的帖子</a>
      <a class="tab" :class="{ 'tab-active': activeTab === 'bookmarks' }" @click="activeTab = 'bookmarks'">我的收藏</a>
    </div>

    <!-- Content -->
    <div class="space-y-4">
      <div v-if="loading" class="text-center py-12">
        <span class="loading loading-spinner loading-lg"></span>
      </div>
      
      <div v-else-if="currentList.length === 0" class="text-center py-12 bg-base-100 rounded-xl border border-base-200">
        <p class="text-base-content/60">暂无内容</p>
      </div>

      <div v-else v-for="post in currentList" :key="post.id" class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer" @click="goToDetail(post.id)">
        <div class="card-body p-6">
          <div class="flex justify-between items-start">
            <h3 class="card-title text-lg font-bold text-base-content mb-2">{{ post.title }}</h3>
            <div class="badge badge-ghost badge-sm">{{ getBoardName(post.board) }}</div>
          </div>
          <p class="text-base-content/70 text-sm line-clamp-2 mb-2">{{ post.content }}</p>
          
          <div class="flex items-center justify-between text-xs text-base-content/50 mt-2">
            <span>{{ formatDate(post.createdAt) }}</span>
            <div class="flex gap-3">
              <span>{{ post.viewCount }} 阅读</span>
              <span>{{ post.likeCount }} 点赞</span>
              <span>{{ post.commentCount }} 评论</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getPostList, type PostVO } from '@/api/post'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const activeTab = ref('posts')
const loading = ref(false)
const myPosts = ref<PostVO[]>([])
const myBookmarks = ref<PostVO[]>([])

const currentList = computed(() => activeTab.value === 'posts' ? myPosts.value : myBookmarks.value)

const boardNameMap: Record<string, string> = {
  'Confessions': '表白墙',
  'TreeHole': '树洞',
  'Help': '求助问答',
  'Market': '跳蚤市场',
  'LostFound': '失物招领'
}

const getBoardName = (board: string) => boardNameMap[board] || board

onMounted(() => {
  if (!userStore.token) {
    router.push('/')
    return
  }
  fetchMyPosts()
  fetchMyBookmarks()
})

const fetchMyPosts = async () => {
  loading.value = true
  try {
    // API needs to support query by userId. Current implementation might need adjustment or new endpoint.
    // For now assuming getPostList supports filtering by current user (maybe implicit or explicit param)
    const res: any = await getPostList({ page: 1, size: 20, userId: userInfo.value?.id })
    myPosts.value = res.rows || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchMyBookmarks = async () => {
  // Need a specific API for user bookmarks. `getPostList` might not cover it unless specific filter.
  // Assuming placeholder empty for now or same API if backend updated.
  // Backend `PostService` has `getUserBookmarks`, need frontend API for it.
  myBookmarks.value = [] 
}

const goToDetail = (id: number) => {
  router.push(`/posts/${id}`)
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}
</script>
