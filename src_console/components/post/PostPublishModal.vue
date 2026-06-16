<template>
  <Teleport to="body">
    <dialog class="modal" :class="{ 'modal-open': open }" style="position: fixed; inset: 0; z-index: 999;">
    <div class="modal-box max-w-3xl">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-bold text-lg">发布新帖子</h3>
        <button class="btn btn-sm btn-circle btn-ghost" @click="close">✕</button>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-5">
        <!-- Board Selection -->
        <div class="form-control w-full">
          <label class="label"><span class="label-text font-bold">选择板块</span></label>
          <div class="flex flex-wrap gap-4">
            <label v-for="option in boardOptions" :key="option.key" class="label cursor-pointer gap-2">
              <input type="checkbox" class="checkbox" v-model="form.boards" :value="option.key" />
              <span class="label-text">{{ option.label }}</span>
            </label>
          </div>
          <label class="label" v-if="hasTreeHole">
            <span class="label-text-alt text-info">提示：树洞板块默认强制匿名发布</span>
          </label>
        </div>
        <div class="form-control">
          <label class="label cursor-pointer justify-start gap-4">
            <input type="checkbox" class="checkbox" v-model="form.showOnHome" />
            <span class="label-text">同步到首页展示</span>
          </label>
          <label class="label">
            <span class="label-text-alt text-slate-400">默认开启，可手动关闭</span>
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
          <textarea v-model="form.content" class="textarea textarea-bordered h-32 text-base" placeholder="分享你的新鲜事..." required maxlength="10000"></textarea>
        </div>

        <!-- Market: Price -->
        <div v-if="hasMarket" class="form-control w-full">
          <label class="label"><span class="label-text font-bold">价格</span></label>
          <label class="input-group">
            <span>￥</span>
            <input type="number" v-model="form.price" placeholder="0.00" class="input input-bordered w-full" min="0" step="0.01" />
          </label>
        </div>

        <!-- LostFound: Location & Time -->
        <template v-if="hasLostFound">
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
        <div class="form-control" v-if="!hasTreeHole">
          <label class="label cursor-pointer justify-start gap-4">
            <input type="checkbox" class="checkbox" v-model="form.isAnonymous" />
            <span class="label-text">匿名发布</span>
          </label>
        </div>

        <!-- Image Upload (Placeholder) -->
        <div class="form-control w-full">
          <label class="label"><span class="label-text font-bold">图片上传</span></label>
          <div class="border-2 border-dashed border-base-300 rounded-xl p-6 text-center hover:bg-base-200 transition-colors cursor-pointer relative">
            <input type="file" multiple accept="image/*" class="absolute inset-0 opacity-0 cursor-pointer" @change="handleFileChange" />
            <div v-if="files.length === 0">
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

        <div class="modal-action">
          <button class="btn btn-ghost" type="button" @click="close">取消</button>
          <button type="submit" class="btn btn-primary" :class="{ 'loading': submitting }" :disabled="submitting">
            {{ submitting ? '发布中...' : '确认发布' }}
          </button>
        </div>
      </form>
    </div>
    <form method="dialog" class="modal-backdrop" @click="close"><button>close</button></form>
  </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { createPost, createConsolePost, type PostCreateDTO } from '@/api/post'
import { uploadPostImage } from '@/api/file'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'
import { BOARD_OPTIONS, normalizeBoardKeys } from '@/utils/boards'

const props = defineProps<{
  modelValue: boolean
  defaultBoards?: string[]
  useConsoleApi?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', id: number): void
}>()

const userStore = useUserStore()
const dialog = useDialog()
const submitting = ref(false)
const files = ref<File[]>([])

const form = reactive<PostCreateDTO>({
  boards: [],
  title: '',
  content: '',
  isAnonymous: false,
  price: undefined,
  location: '',
  lostTime: '',
  fileIds: [],
  showOnHome: true
})

const boardOptions = BOARD_OPTIONS
const hasTreeHole = computed(() => form.boards.includes('treehole'))
const hasMarket = computed(() => form.boards.includes('market'))
const hasLostFound = computed(() => form.boards.includes('lost-found'))

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const resetForm = () => {
  form.boards = normalizeBoardKeys(props.defaultBoards || [])
  form.title = ''
  form.content = ''
  form.isAnonymous = false
  form.price = undefined
  form.location = ''
  form.lostTime = ''
  form.fileIds = []
  form.showOnHome = true
  files.value = []
}

watch(() => props.modelValue, (value) => {
  if (value) {
    resetForm()
  }
})

watch(hasTreeHole, (value) => {
  if (value) {
    form.isAnonymous = true
  }
})

const close = () => {
  open.value = false
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files) {
    const newFiles = Array.from(target.files)
    if (files.value.length + newFiles.length > 9) {
      void dialog.alert('最多上传9张图片')
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
    await dialog.alert('请先登录')
    return
  }
  if (!form.boards || form.boards.length === 0) {
    await dialog.alert('请选择至少一个板块')
    return
  }
  if (hasTreeHole.value) {
    form.isAnonymous = true
  }

  submitting.value = true
  try {
    if (files.value.length > 0) {
      const uploadedIds: number[] = []
      for (const file of files.value) {
        const res = await uploadPostImage(file)
        const fileId = res?.id
        if (!fileId) {
          throw new Error('图片上传失败')
        }
        uploadedIds.push(Number(fileId))
      }
      form.fileIds = uploadedIds
    } else {
      form.fileIds = []
    }
    const res = props.useConsoleApi ? await createConsolePost(form) : await createPost(form)
    const postId = Number(res || 0)
    emit('success', postId)
    close()
  } catch (error: unknown) {
    console.error(error)
    await dialog.alert((error as ApiErrorLike)?.message || '发布失败')
  } finally {
    submitting.value = false
  }
}
</script>
