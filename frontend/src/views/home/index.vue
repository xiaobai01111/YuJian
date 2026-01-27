<template>
  <div>
    <!-- Category Cards -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
      <router-link v-for="category in categories" :key="category.id" :to="category.path"
        class="card bg-base-100 shadow-sm hover:shadow-md transition-all duration-300 hover:-translate-y-1 border border-base-200 cursor-pointer group">
        <div class="card-body p-6 items-start">
          <div :class="`p-3 rounded-xl ${category.bgClass} ${category.textClass} mb-3 group-hover:scale-110 transition-transform`">
            <component :is="category.icon" class="w-6 h-6" />
          </div>
          <h3 class="font-bold text-base text-slate-800">{{ category.name }}</h3>
          <p class="text-xs text-slate-500 mt-1">{{ category.desc }}</p>
        </div>
      </router-link>
    </div>

    <!-- Main Content Section -->
    <div class="flex flex-col space-y-6">
      <!-- Section Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-1.5 h-6 bg-blue-600 rounded-full"></div>
          <h2 class="text-xl font-bold text-slate-800">最新动态</h2>
        </div>
        <div class="flex items-center gap-3 text-sm font-medium text-slate-500">
          <button class="hover:text-slate-900 transition-colors">全部精华</button>
          <button class="btn btn-primary btn-sm" @click="openPublish">发布</button>
        </div>
      </div>

      <!-- Content Area -->
      <div v-if="loadingPosts && latestPosts.length === 0" class="text-center py-10">
        <span class="loading loading-spinner loading-lg text-blue-500"></span>
      </div>
      <div v-else-if="latestPosts.length === 0" class="bg-base-100 border border-dashed border-base-300 rounded-2xl min-h-[360px] flex flex-col items-center justify-center text-center p-8">
        <div class="w-24 h-24 bg-slate-50 rounded-full flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
        </div>
        <h3 class="text-slate-900 font-medium mb-1">暂无内容</h3>
        <p class="text-slate-500 text-sm mb-4">快来发布第一条帖子吧</p>
        <button class="btn btn-primary btn-sm" @click="openPublish">发布帖子</button>
      </div>
      <div v-else class="grid grid-cols-1 xl:grid-cols-2 gap-4">
        <div
          v-for="post in latestPosts"
          :key="post.id"
          class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer"
          @click="openPostDetail(post.id)"
        >
          <div class="card-body p-5">
            <div class="flex items-start justify-between gap-3 mb-2">
              <div class="flex items-center gap-2 min-w-0 flex-1 overflow-hidden">
                <h3 class="font-bold text-slate-800 text-base truncate flex-1 min-w-0">{{ truncateText(post.title || '校园动态', 20) }}</h3>
                <span v-if="post.status === 1" class="badge badge-success badge-sm shrink-0">已解决</span>
              </div>
              <div class="flex flex-wrap gap-1 shrink-0">
                <span
                  v-for="board in getPostBoards(post)"
                  :key="board"
                  class="badge badge-ghost badge-xs"
                >
                  {{ getBoardLabel(board) }}
                </span>
              </div>
            </div>
            <p class="text-slate-600 text-sm line-clamp-2 mb-3 break-all">{{ truncateText(post.content, 100) }}</p>
            <div v-if="post.files && post.files.length > 0" class="flex gap-2 mb-3 overflow-x-auto pb-1">
              <img
                v-for="file in post.files.slice(0, 3)"
                :key="file.id"
                :src="resolveFileUrl(file.url)"
                class="w-20 h-20 object-cover rounded-lg border border-base-200"
                alt=""
              />
              <div v-if="post.files.length > 3" class="w-20 h-20 bg-base-200 rounded-lg flex items-center justify-center text-slate-400 text-xs">
                +{{ post.files.length - 3 }}
              </div>
            </div>
            <div class="flex items-center justify-between text-xs text-slate-400 pt-2 border-t border-base-200">
              <div class="flex items-center gap-2">
                <div class="avatar placeholder">
                  <div class="bg-slate-200 text-slate-600 rounded-full w-6 h-6">
                    <span class="text-xs">{{ post.isAnonymous ? '?' : (post.author?.nickname?.[0] || 'U') }}</span>
                  </div>
                </div>
                <span>{{ post.isAnonymous ? '匿名用户' : (post.author?.nickname || post.author?.username || '用户') }}</span>
                <span>{{ formatDateTime(post.createdAt) }}</span>
              </div>
              <div class="flex items-center gap-4">
                <span class="flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  {{ post.viewCount || 0 }}
                </span>
                <button class="flex items-center gap-1 transition-colors" :class="post.isLiked ? 'text-pink-500' : ''" @click.stop="toggleLike(post)" :aria-pressed="post.isLiked">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" :fill="post.isLiked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                  {{ post.likeCount || 0 }}
                </button>
                <span class="flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  {{ post.commentCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Sidebar Content (Injected into Layout) -->
  <teleport to="#sidebar-slot-target" v-if="mounted">
      <!-- Bulletin -->
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body p-5">
           <div class="flex items-center justify-between mb-4">
             <div class="flex items-center gap-2">
               <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                 <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z" />
               </svg>
               <h3 class="font-bold text-slate-800">公告栏</h3>
             </div>
             <button @click="showNoticeModal = true" class="text-xs text-blue-500 hover:underline">更多</button>
           </div>
           <div v-if="notices.length > 0" class="space-y-3">
             <div
               v-for="notice in notices.slice(0, 3)" 
               :key="notice.id" 
               class="block group cursor-pointer"
               @click="openNoticeDetail(notice)"
             >
               <div class="flex items-start gap-2">
                 <span v-if="notice.isPinned" class="badge badge-error badge-xs mt-1">置顶</span>
                 <span class="text-sm text-slate-600 group-hover:text-primary transition-colors line-clamp-1 flex-1">
                   {{ notice.title }}
                 </span>
                 <span class="text-xs text-slate-400 shrink-0">{{ formatDate(notice.publishedAt) }}</span>
               </div>
             </div>
           </div>
           <div v-else class="text-sm text-slate-500 text-center py-2">暂无公告</div>
        </div>
      </div>

      <!-- School Calendar -->
      <div class="card bg-base-100 shadow-sm border border-base-200 mt-6">
        <div class="card-body p-5">
           <div class="flex items-center gap-2 mb-4">
             <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-purple-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
               <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
             </svg>
             <h3 class="font-bold text-slate-800">校历周次</h3>
           </div>
           <div class="text-center py-4 bg-purple-50 rounded-xl">
             <div class="text-xs text-purple-600 mb-1">2025-2026学年 秋季学期</div>
             <div class="text-3xl font-black text-purple-600">第 12 周</div>
             <div class="text-xs text-purple-400 mt-1">考试周倒计时: 4 周</div>
           </div>
        </div>
      </div>
  </teleport>

  <!-- Notice List Modal -->
  <dialog class="modal" :class="{ 'modal-open': showNoticeModal }">
    <div class="modal-box max-w-2xl max-h-[80vh]">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-bold text-lg">全部公告</h3>
        <button class="btn btn-sm btn-circle btn-ghost" @click="showNoticeModal = false">✕</button>
      </div>
      <div v-if="allNotices.length > 0" class="space-y-3 overflow-y-auto max-h-[60vh]">
        <div 
          v-for="notice in allNotices" 
          :key="notice.id" 
          class="block p-4 rounded-lg border border-base-200 hover:bg-base-50 transition-colors cursor-pointer"
          @click="openNoticeDetail(notice)"
        >
          <div class="flex items-start gap-2">
            <span v-if="notice.isPinned" class="badge badge-error badge-sm">置顶</span>
            <div class="flex-1">
              <div class="font-medium text-slate-800 line-clamp-1">{{ notice.title }}</div>
              <div class="text-xs text-slate-400 mt-1">{{ formatDateTime(notice.publishedAt) }}</div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="text-center py-8 text-slate-500">暂无公告</div>
    </div>
    <form method="dialog" class="modal-backdrop" @click="showNoticeModal = false"><button>close</button></form>
  </dialog>

  <!-- Notice Detail Modal -->
  <dialog class="modal" :class="{ 'modal-open': showDetailModal }">
    <div class="modal-box max-w-2xl border-0 shadow-xl">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <span v-if="selectedNotice?.isPinned" class="badge badge-error">置顶</span>
          <h3 class="font-bold text-lg">{{ selectedNotice?.title }}</h3>
        </div>
        <button class="btn btn-sm btn-circle btn-ghost" @click="showDetailModal = false">✕</button>
      </div>
      <div class="text-xs text-slate-400 mb-4">
        发布时间：{{ formatDateTime(selectedNotice?.publishedAt) }}
        <span v-if="selectedNotice?.createdByName" class="ml-4">发布者：{{ selectedNotice?.createdByName }}</span>
      </div>
      <div class="prose prose-sm max-w-none whitespace-pre-wrap text-slate-700">
        {{ selectedNotice?.content }}
      </div>
      <div class="modal-action">
        <button class="btn btn-sm" @click="showDetailModal = false">关闭</button>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop" @click="showDetailModal = false"><button>close</button></form>
  </dialog>

  <PostPublishModal v-model="showPublish" @success="loadLatestPosts" />
  <PostDetailModal v-model="showPostDetail" :postId="selectedPostId" @updated="loadLatestPosts" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { getPublicNotices, type NoticeVO } from '@/api/system'
import { getPostList, likePost, unlikePost, type PostVO, type PostQueryDTO } from '@/api/post'
import { getBoardLabel, normalizeBoardKeys } from '@/utils/boards'
import { resolveFileUrl } from '@/utils/file'
import PostPublishModal from '@/components/post/PostPublishModal.vue'
import PostDetailModal from '@/components/post/PostDetailModal.vue'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const mounted = ref(false)
const notices = ref<NoticeVO[]>([])
const allNotices = ref<NoticeVO[]>([])
const showNoticeModal = ref(false)
const showDetailModal = ref(false)
const selectedNotice = ref<NoticeVO | null>(null)
const latestPosts = ref<PostVO[]>([])
const loadingPosts = ref(false)
const showPublish = ref(false)
const showPostDetail = ref(false)
const selectedPostId = ref<number | null>(null)
const userStore = useUserStore()
const dialog = useDialog()

const postQuery = reactive<PostQueryDTO>({
  page: 1,
  size: 6,
  showOnHome: true
})

onMounted(async () => {
  mounted.value = true
  await loadNotices()
  await loadLatestPosts()
})

const loadNotices = async () => {
  try {
    const res = await getPublicNotices(50)
    const list = normalizeNoticeList(res)
    allNotices.value = list
    notices.value = list.slice(0, 3)
  } catch (e) {
    console.error('Failed to load notices', e)
  }
}

const loadLatestPosts = async () => {
  loadingPosts.value = true
  try {
    const res: any = await getPostList(postQuery)
    latestPosts.value = res.records || []
  } catch (e) {
    console.error('Failed to load posts', e)
  } finally {
    loadingPosts.value = false
  }
}

const openPublish = () => {
  showPublish.value = true
}

const openPostDetail = (id: number) => {
  selectedPostId.value = id
  showPostDetail.value = true
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

const getPostBoards = (post: PostVO) => {
  const raw = post.boards && post.boards.length > 0 ? post.boards : post.board ? [post.board] : []
  return normalizeBoardKeys(raw)
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const truncateText = (text: string | undefined, maxLength: number) => {
  if (!text) return ''
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}

const openNoticeDetail = (notice: NoticeVO) => {
  selectedNotice.value = notice
  showNoticeModal.value = false
  showDetailModal.value = true
}

const normalizeNoticeList = (res: any): NoticeVO[] => {
  if (Array.isArray(res)) return res
  if (res?.records && Array.isArray(res.records)) return res.records
  if (res?.data && Array.isArray(res.data)) return res.data
  return []
}

// Icons as components
const HeartIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" })
  ])
}

const ChatIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" })
  ])
}

const QuestionIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" })
  ])
}

const TagIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" })
  ])
}

const SearchIcon = {
  render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor" }, [
    h('path', { 'stroke-linecap': "round", 'stroke-linejoin': "round", 'stroke-width': "2", d: "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" })
  ])
}

const categories = [
  { id: 1, name: '表白墙', desc: '勇敢表达爱', icon: HeartIcon, bgClass: 'bg-pink-50', textClass: 'text-pink-500', path: '/confessions' },
  { id: 2, name: '树洞', desc: '倾诉秘密', icon: ChatIcon, bgClass: 'bg-green-50', textClass: 'text-green-500', path: '/treehole' },
  { id: 3, name: '求助问答', desc: '互助成长', icon: QuestionIcon, bgClass: 'bg-blue-50', textClass: 'text-blue-500', path: '/help' },
  { id: 4, name: '跳蚤市场', desc: '旧物新生', icon: TagIcon, bgClass: 'bg-orange-50', textClass: 'text-orange-500', path: '/market' },
  { id: 5, name: '失物招领', desc: '传递温暖', icon: SearchIcon, bgClass: 'bg-purple-50', textClass: 'text-purple-500', path: '/lost-found' },
]

</script>
