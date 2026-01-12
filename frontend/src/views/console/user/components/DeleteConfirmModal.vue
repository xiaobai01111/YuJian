<template>
  <dialog ref="modalRef" class="modal">
    <div class="modal-box">
      <h3 class="font-bold text-lg flex items-center gap-2 text-error">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        确认删除
      </h3>
      <p class="py-4">
        确定要删除以下 <span class="font-bold text-error">{{ userIds.length }}</span> 个用户吗？此操作不可恢复。
      </p>
      <div class="bg-base-200 rounded-lg p-3 max-h-32 overflow-y-auto">
        <div v-for="name in userNames" :key="name" class="text-sm py-1">• {{ name }}</div>
      </div>
      <div class="modal-action">
        <button class="btn btn-ghost" @click="close">取消</button>
        <button class="btn btn-error" @click="handleConfirm" :disabled="loading">
          <span v-if="loading" class="loading loading-spinner loading-sm"></span>
          确认删除
        </button>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop">
      <button>close</button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  (e: 'confirm', ids: number[]): void
}>()

const modalRef = ref<HTMLDialogElement>()
const loading = ref(false)
const userIds = ref<number[]>([])
const userNames = ref<string[]>([])

const open = (ids: number[], names: string[]) => {
  userIds.value = ids
  userNames.value = names
  modalRef.value?.showModal()
}

const close = () => {
  modalRef.value?.close()
}

const handleConfirm = async () => {
  loading.value = true
  try {
    emit('confirm', userIds.value)
    close()
  } finally {
    loading.value = false
  }
}

defineExpose({ open, close })
</script>
