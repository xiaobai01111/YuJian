<template>
  <Teleport to="body">
    <dialog class="modal" :class="{ 'modal-open': open }" style="position: fixed; inset: 0; z-index: 999;">
    <div class="modal-box max-w-3xl max-h-[90vh] p-0">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="text-center py-16 px-6">
        <div class="text-5xl mb-4">😢</div>
        <h3 class="text-lg font-bold text-slate-800 mb-2">加载失败</h3>
        <p class="text-slate-500 text-sm mb-4">{{ error }}</p>
        <button class="btn btn-primary btn-sm" @click="close">关闭</button>
      </div>

      <!-- Content -->
      <div v-else-if="post" class="relative">
        <!-- Close Button -->
        <button class="btn btn-sm btn-circle btn-ghost absolute right-3 top-3 z-10" @click="close">✕</button>

        <!-- Header -->
        <div class="p-6 pb-4 border-b border-base-200">
          <div class="flex items-center gap-3 mb-4 pr-8">
            <div class="avatar placeholder">
              <div class="bg-slate-200 text-slate-600 rounded-full w-10 h-10">
                <span class="text-lg">{{ post.isAnonymous ? '?' : (post.author?.nickname?.[0] || 'U') }}</span>
              </div>
            </div>
            <div class="flex-1">
              <button
                class="font-bold text-slate-800"
                :class="post.isAnonymous || !post.author?.id ? 'cursor-not-allowed opacity-70' : 'hover:text-primary'"
                :disabled="post.isAnonymous || !post.author?.id"
                @click="openUserProfile"
              >
                {{ post.isAnonymous ? '匿名用户' : (post.author?.nickname || post.author?.username || '用户') }}
              </button>
              <div class="text-xs text-slate-400">
                {{ formatDateTime(post.createdAt) }} · {{ post.viewCount || 0 }} 阅读
              </div>
            </div>
            <div class="flex flex-wrap gap-1">
              <span v-for="board in displayBoards" :key="board" class="badge badge-ghost badge-sm">
                {{ getBoardLabel(board) }}
              </span>
            </div>
          </div>

          <h2 v-if="post.title" class="text-xl font-bold text-slate-800">{{ post.title }}</h2>
        </div>

        <!-- Body -->
        <div class="p-6 max-h-[50vh] overflow-y-auto">
          <div class="prose prose-sm max-w-none text-slate-700 whitespace-pre-wrap leading-relaxed">
            {{ post.content }}
          </div>

          <!-- Images -->
          <div v-if="post.files && post.files.length > 0" class="mt-6 grid gap-3" :class="getImageGridClass(post.files.length)">
            <div v-for="file in post.files" :key="file.id" class="relative cursor-pointer" @click="openImage(file.url)">
              <img :src="file.url" class="rounded-xl w-full h-full object-cover max-h-64 hover:opacity-90 transition-opacity" loading="lazy" />
            </div>
          </div>

          <!-- Market Info -->
          <div v-if="post.price != null" class="mt-6 p-4 bg-orange-50 rounded-xl">
            <div class="text-orange-600 font-bold text-xl">￥{{ post.price }}</div>
            <div v-if="post.location" class="text-sm text-orange-500 mt-1">📍 {{ post.location }}</div>
          </div>

          <!-- LostFound Info -->
          <div v-if="post.lostTime" class="mt-6 p-4 bg-purple-50 rounded-xl">
            <div class="text-sm text-purple-600">
              <span v-if="post.location">📍 {{ post.location }}</span>
              <span v-if="post.location && post.lostTime"> · </span>
              <span>🕐 {{ formatDateTime(post.lostTime) }}</span>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="p-4 border-t border-base-200 flex items-center justify-between">
          <div class="flex gap-2">
            <button 
              class="btn btn-sm gap-2" 
              :class="post.isLiked ? 'btn-primary' : 'btn-ghost'"
              @click="handleLike"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" :fill="post.isLiked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
              {{ post.likeCount || 0 }}
            </button>
            <button class="btn btn-sm btn-ghost gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              {{ post.commentCount || 0 }}
            </button>
          </div>
          <button 
            class="btn btn-sm btn-ghost gap-2" 
            :class="post.isBookmarked ? 'text-warning' : ''"
            @click="handleBookmark"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" :fill="post.isBookmarked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
            </svg>
            收藏
          </button>
        </div>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop" @click="close"><button>close</button></form>
  </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getPostDetail, recordPostView, likePost, unlikePost, bookmarkPost, unbookmarkPost, type PostVO } from '@/api/post'
import { getBoardLabel, getPostBoards } from '@/utils/boards'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const props = defineProps<{
  modelValue: boolean
  postId: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'updated'): void
}>()

const userStore = useUserStore()
const router = useRouter()
const dialog = useDialog()
const post = ref<PostVO | null>(null)
const loading = ref(false)
const error = ref('')

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const displayBoards = computed(() => (post.value ? getPostBoards(post.value) : []))

watch(() => props.modelValue, async (value) => {
  if (value && props.postId) {
    await loadPost(props.postId)
  }
})

watch(() => props.postId, async (id) => {
  if (open.value && id) {
    await loadPost(id)
  }
})

const loadPost = async (id: number) => {
  loading.value = true
  error.value = ''
  post.value = null
  try {
    const res: any = await getPostDetail(id)
    post.value = res
    try {
      await recordPostView(id)
      if (post.value) {
        post.value.viewCount = (post.value.viewCount || 0) + 1
      }
    } catch (e) {
      console.error(e)
    }
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

const close = () => {
  open.value = false
}

const handleLike = async () => {
  if (!post.value) return
  if (!userStore.token) {
    await dialog.alert('请先登录')
    return
  }
  try {
    if (post.value.isLiked) {
      await unlikePost(post.value.id)
      post.value.likeCount = Math.max(0, (post.value.likeCount || 0) - 1)
      post.value.isLiked = false
    } else {
      await likePost(post.value.id)
      post.value.likeCount = (post.value.likeCount || 0) + 1
      post.value.isLiked = true
    }
    emit('updated')
  } catch (e) {
    console.error(e)
  }
}

const handleBookmark = async () => {
  if (!post.value) return
  if (!userStore.token) {
    await dialog.alert('请先登录')
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
    emit('updated')
  } catch (e) {
    console.error(e)
  }
}

const openUserProfile = () => {
  if (!post.value || post.value.isAnonymous || !post.value.author?.id) return
  router.push(`/user/${post.value.author.id}`)
  close()
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
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
