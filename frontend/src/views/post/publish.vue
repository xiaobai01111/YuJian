<template>
  <div class="max-w-3xl mx-auto py-8">
    <div class="card bg-base-100 shadow-lg border border-base-200">
      <div class="card-body">
        <h2 class="card-title text-2xl mb-6 flex justify-between items-center">
          发布新帖子
          <button class="btn btn-ghost btn-sm" @click="$router.back()">取消</button>
        </h2>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <!-- Board Selection -->
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-bold">选择板块</span></label>
            <select class="select select-bordered w-full" v-model="form.board" required>
              <option disabled value="">请选择板块</option>
              <option value="Confessions">表白墙 (Confessions)</option>
              <option value="TreeHole">树洞 (TreeHole)</option>
              <option value="Help">求助问答 (Help)</option>
              <option value="Market">跳蚤市场 (Market)</option>
              <option value="LostFound">失物招领 (LostFound)</option>
            </select>
            <label class="label" v-if="form.board === 'TreeHole'">
              <span class="label-text-alt text-info">提示：树洞板块默认强制匿名发布</span>
            </label>
          </div>

          <!-- Title -->
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-bold">标题</span></label>
            <input type="text" v-model="form.title" placeholder="请输入标题 (可选，最多200字)" class="input input-bordered w-full" maxlength="200" />
          </div>

          <!-- Content -->
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-bold">内容</span></label>
            <textarea v-model="form.content" class="textarea textarea-bordered h-40 text-base" placeholder="分享你的新鲜事..." required maxlength="10000"></textarea>
          </div>

          <!-- Options based on Board -->
          <!-- Market: Price -->
          <div v-if="form.board === 'Market'" class="form-control w-full">
            <label class="label"><span class="label-text font-bold">价格</span></label>
            <label class="input-group">
              <span>￥</span>
              <input type="number" v-model="form.price" placeholder="0.00" class="input input-bordered w-full" min="0" step="0.01" />
            </label>
          </div>

          <!-- LostFound: Type, Location, Time -->
          <template v-if="form.board === 'LostFound'">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="form-control w-full">
                <label class="label"><span class="label-text font-bold">地点</span></label>
                <input type="text" v-model="form.location" placeholder="丢失/拾获地点" class="input input-bordered w-full" />
              </div>
              <div class="form-control w-full">
                <label class="label"><span class="label-text font-bold">时间</span></label>
                <input type="datetime-local" v-model="form.lostTime" class="input input-bordered w-full" />
              </div>
            </div>
          </template>

          <!-- Anonymous Option -->
          <div class="form-control" v-if="form.board !== 'TreeHole'">
            <label class="label cursor-pointer justify-start gap-4">
              <input type="checkbox" class="checkbox" v-model="form.isAnonymous" />
              <span class="label-text">匿名发布</span>
            </label>
          </div>

          <!-- Image Upload (Placeholder) -->
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-bold">图片上传</span></label>
            <div class="border-2 border-dashed border-base-300 rounded-xl p-8 text-center hover:bg-base-200 transition-colors cursor-pointer relative">
              <input type="file" multiple accept="image/*" class="absolute inset-0 opacity-0 cursor-pointer" @change="handleFileChange" />
              <div v-if="files.length === 0">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mx-auto text-base-content/40 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                <p class="text-sm text-base-content/60">点击或拖拽上传图片</p>
              </div>
              <div v-else class="grid grid-cols-4 gap-4">
                <div v-for="(file, index) in files" :key="index" class="relative group">
                  <img :src="getObjectURL(file)" class="w-full h-24 object-cover rounded-lg" />
                  <button type="button" class="btn btn-xs btn-circle btn-error absolute -top-2 -right-2 opacity-0 group-hover:opacity-100 transition-opacity" @click.stop="removeFile(index)">✕</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Submit -->
          <div class="pt-4">
            <button type="submit" class="btn btn-primary w-full" :class="{ 'loading': submitting }" :disabled="submitting">
              {{ submitting ? '发布中...' : '确认发布' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createPost, type PostCreateDTO } from '@/api/post'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const submitting = ref(false)
const files = ref<File[]>([])

const form = reactive<PostCreateDTO>({
  board: (route.query.board as string) || '',
  title: '',
  content: '',
  isAnonymous: false,
  price: undefined,
  location: '',
  lostTime: ''
})

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files) {
    const newFiles = Array.from(target.files)
    if (files.value.length + newFiles.length > 9) {
      alert('最多上传9张图片')
      return
    }
    files.value.push(...newFiles)
  }
}

const removeFile = (index: number) => {
  files.value.splice(index, 1)
}

const getObjectURL = (file: File) => {
  return URL.createObjectURL(file)
}

const handleSubmit = async () => {
  if (!userStore.token) {
    alert('请先登录')
    return
  }
  
  submitting.value = true
  try {
    // Upload files first if any (Not implemented yet, need file upload API)
    // For now we mock it or skip
    // const uploadedFileIds = await uploadFiles(files.value)
    // form.fileIds = uploadedFileIds
    
    // Convert date string to ISO if needed, or backend handles it
    if (form.lostTime) {
        // Backend expects LocalDateTime usually, adjust format if needed
    }

    const postId = await createPost(form)
    // Redirect to detail
    router.push(`/posts/${postId}`)
  } catch (error: any) {
    console.error(error)
    alert(error.message || '发布失败')
  } finally {
    submitting.value = false
  }
}
</script>
