<template>
  <PostPublishModal v-model="open" :default-boards="defaultBoards" @success="handleSuccess" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostPublishModal from '@/components/post/PostPublishModal.vue'
import { normalizeBoardKeys } from '@/utils/boards'

const router = useRouter()
const route = useRoute()
const open = ref(true)
const defaultBoards = normalizeBoardKeys(route.query.board ? [String(route.query.board)] : [])

const handleSuccess = (postId: number) => {
  router.push(`/posts/${postId}`)
}

watch(open, (value) => {
  if (!value) {
    router.back()
  }
})
</script>
