<template>
  <div class="max-w-4xl mx-auto">
    <div class="card bg-base-100 shadow-lg border border-base-200 mb-8">
      <div class="card-body">
        <div v-if="loadingUser" class="text-center py-12">
          <span class="loading loading-spinner loading-lg"></span>
        </div>
        <div v-else-if="!userInfo" class="text-center py-12 text-base-content/60">
          用户不存在或无法访问
        </div>
        <div v-else class="flex flex-col md:flex-row items-center gap-6">
          <div class="avatar placeholder">
            <div class="bg-neutral-focus text-neutral-content rounded-full w-24 h-24 text-3xl">
              <span>{{ userInfo?.nickname?.[0] || userInfo?.username?.[0] || 'U' }}</span>
            </div>
          </div>
          <div class="text-center md:text-left flex-1">
            <h1 class="text-2xl font-bold">{{ userInfo?.nickname || userInfo?.username }}</h1>
            <p class="text-base-content/60 mt-1">@{{ userInfo?.username }}</p>
            <div class="flex gap-6 mt-4 justify-center md:justify-start">
              <div class="text-center">
                <div class="font-bold text-lg">{{ postTotal }}</div>
                <div class="text-xs text-base-content/60">帖子</div>
              </div>
              <div v-if="isSelf" class="text-center">
                <div class="font-bold text-lg">{{ bookmarkTotal }}</div>
                <div class="text-xs text-base-content/60">收藏</div>
              </div>
            </div>
          </div>
          <div v-if="isSelf">
            <button class="btn btn-outline btn-sm">编辑资料</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs tabs-boxed bg-base-100 p-1 mb-6 w-fit">
      <a class="tab" :class="{ 'tab-active': activeTab === 'posts' }" @click="activeTab = 'posts'">{{ isSelf ? '我的帖子' : 'TA的帖子' }}</a>
      <a v-if="isSelf" class="tab" :class="{ 'tab-active': activeTab === 'bookmarks' }" @click="activeTab = 'bookmarks'">我的收藏</a>
    </div>

    <div v-if="showBatchActions" class="flex flex-wrap items-center gap-2 mb-4">
      <button class="btn btn-xs btn-ghost" @click="toggleSelectAll">{{ selectedCount === myPosts.length ? '清空选择' : '全选' }}</button>
      <span class="text-xs text-base-content/60">已选 {{ selectedCount }} 条</span>
      <button class="btn btn-xs btn-outline" :disabled="selectedCount === 0" @click="handleBatchBookmark">批量收藏</button>
      <button class="btn btn-xs btn-outline btn-error" :disabled="selectedCount === 0" @click="handleBatchReport">批量举报</button>
    </div>

    <!-- Content -->
    <div class="space-y-4">
      <div v-if="loading" class="text-center py-12">
        <span class="loading loading-spinner loading-lg"></span>
      </div>

      <div v-else-if="currentList.length === 0" class="text-center py-12 bg-base-100 rounded-xl border border-base-200">
        <p class="text-base-content/60">暂无内容</p>
      </div>

      <div
        v-else
        v-for="post in currentList"
        :key="post.id"
        class="card bg-base-100 shadow-sm hover:shadow-md transition-all border border-base-200 cursor-pointer"
        @click="goToDetail(post.id)"
      >
        <div class="card-body p-6">
          <div class="flex items-start gap-3">
            <input
              v-if="showBatchActions"
              type="checkbox"
              class="checkbox checkbox-sm mt-1"
              :checked="selectedPostIds.has(post.id)"
              @click.stop
              @change="toggleSelect(post.id)"
            />
            <div class="flex-1">
              <div class="flex justify-between items-start">
                <h3 class="card-title text-lg font-bold text-base-content mb-2 min-w-0">
                  <span class="line-clamp-1 break-words min-w-0">{{ post.title }}</span>
                </h3>
                <div class="flex flex-wrap gap-2">
                  <div v-for="board in getPostBoards(post)" :key="board" class="badge badge-ghost badge-sm">
                    {{ getBoardLabel(board) }}
                  </div>
                </div>
              </div>
              <p class="text-base-content/70 text-sm line-clamp-2 mb-2 break-words">{{ post.content }}</p>

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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserDetail, getUserPosts, getMyBookmarks, type UserDetailVO } from '@/api/user'
import { batchBookmarkPosts, batchReportPosts, likePost, unlikePost, type PostVO } from '@/api/post'
import { getBoardLabel, getPostBoards } from '@/utils/boards'
import { useDialog } from '@/composables/useDialog'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const dialog = useDialog()

const activeTab = ref<'posts' | 'bookmarks'>('posts')
const loading = ref(false)
const loadingUser = ref(false)
const profileUser = ref<UserDetailVO | null>(null)

const myPosts = ref<PostVO[]>([])
const myBookmarks = ref<PostVO[]>([])
const postTotal = ref(0)
const bookmarkTotal = ref(0)

const selectedPostIds = ref<Set<number>>(new Set())

const targetUserId = computed(() => Number(route.params.id))
const isSelf = computed(() => userStore.userInfo?.id === targetUserId.value)
const userInfo = computed(() => (isSelf.value ? userStore.userInfo : profileUser.value))

const currentList = computed(() => (activeTab.value === 'posts' ? myPosts.value : myBookmarks.value))
const selectedCount = computed(() => selectedPostIds.value.size)
const showBatchActions = computed(() => !isSelf.value && activeTab.value === 'posts' && myPosts.value.length > 0)

const loadProfile = async () => {
  if (!userStore.token) {
    router.push('/')
    return
  }

  if (!Number.isFinite(targetUserId.value) || targetUserId.value <= 0) {
    router.push('/')
    return
  }

  loadingUser.value = !isSelf.value
  profileUser.value = null

  try {
    if (!isSelf.value) {
      profileUser.value = await getUserDetail(targetUserId.value)
    }
  } catch (error) {
    console.error(error)
  } finally {
    loadingUser.value = false
  }

  await fetchPosts()

  if (isSelf.value) {
    await fetchMyBookmarks()
  } else {
    myBookmarks.value = []
    bookmarkTotal.value = 0
  }
}

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await getUserPosts(targetUserId.value, 1, 20)
    myPosts.value = res.records || []
    postTotal.value = res.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
    clearSelection()
  }
}

const fetchMyBookmarks = async () => {
  try {
    const res = await getMyBookmarks(1, 20)
    myBookmarks.value = res.records || []
    bookmarkTotal.value = res.total || 0
  } catch (error) {
    console.error(error)
  }
}

const toggleSelect = (id: number) => {
  const next = new Set(selectedPostIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  selectedPostIds.value = next
}

const toggleSelectAll = () => {
  if (selectedPostIds.value.size === myPosts.value.length) {
    clearSelection()
    return
  }
  selectedPostIds.value = new Set(myPosts.value.map(post => post.id))
}

const clearSelection = () => {
  selectedPostIds.value = new Set()
}

const handleBatchBookmark = async () => {
  if (selectedCount.value === 0) return
  const confirmed = await dialog.confirm(`确定要收藏已选择的 ${selectedCount.value} 条帖子吗？`)
  if (!confirmed) return

  try {
    const res = await batchBookmarkPosts(Array.from(selectedPostIds.value))
    const success = res?.success ?? 0
    const skipped = res?.skipped ?? 0
    myPosts.value.forEach(post => {
      if (selectedPostIds.value.has(post.id)) {
        post.isBookmarked = true
      }
    })
    await dialog.alert(`已收藏 ${success} 条，跳过 ${skipped} 条`) 
    clearSelection()
  } catch (error) {
    console.error(error)
    await dialog.alert('批量收藏失败')
  }
}

const handleBatchReport = async () => {
  if (selectedCount.value === 0) return
  const reason = await dialog.prompt('请输入举报原因', { required: true, multiline: true, placeholder: '请描述举报原因' })
  if (!reason) return

  try {
    const res = await batchReportPosts(Array.from(selectedPostIds.value), reason)
    const success = res?.success ?? 0
    const skipped = res?.skipped ?? 0
    await dialog.alert(`已提交 ${success} 条举报，跳过 ${skipped} 条`)
    clearSelection()
  } catch (error) {
    console.error(error)
    await dialog.alert('批量举报失败')
  }
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
  } catch (error) {
    console.error(error)
  }
}

const goToDetail = (id: number) => {
  router.push(`/posts/${id}`)
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

watch(
  () => targetUserId.value,
  () => {
    activeTab.value = 'posts'
    clearSelection()
    void loadProfile()
  },
  { immediate: true }
)

watch(
  () => activeTab.value,
  () => {
    clearSelection()
  }
)
</script>
