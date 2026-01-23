<template>
  <div class="modal" :class="{ 'modal-open': state.open }">
    <div class="modal-box">
      <h3 class="font-bold text-lg">{{ state.title }}</h3>
      <p class="py-4 whitespace-pre-wrap">{{ state.message }}</p>

      <div v-if="state.mode === 'prompt'" class="space-y-2">
        <textarea
          v-if="state.inputMultiline"
          ref="inputRef"
          v-model="inputValue"
          class="textarea textarea-bordered w-full"
          :placeholder="state.inputPlaceholder"
          rows="4"
          @keydown.ctrl.enter.prevent="handleConfirm"
        ></textarea>
        <input
          v-else
          ref="inputRef"
          v-model="inputValue"
          class="input input-bordered w-full"
          :placeholder="state.inputPlaceholder"
          @keyup.enter="handleConfirm"
        />
        <p v-if="errorMessage" class="text-sm text-error">{{ errorMessage }}</p>
      </div>

      <div class="modal-action">
        <button
          v-if="state.mode !== 'alert'"
          class="btn btn-ghost"
          @click="handleCancel"
        >{{ state.cancelText }}</button>
        <button class="btn btn-primary" @click="handleConfirm">{{ state.confirmText }}</button>
      </div>
    </div>
    <div class="modal-backdrop">
      <button @click="handleBackdrop">close</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useDialogState } from '@/composables/useDialog'

const { state, closeDialog } = useDialogState()

const inputRef = ref<HTMLInputElement | HTMLTextAreaElement | null>(null)
const inputValue = ref('')
const errorMessage = ref('')

const resetPrompt = () => {
  inputValue.value = state.inputDefault || ''
  errorMessage.value = ''
  nextTick(() => inputRef.value?.focus())
}

watch(
  () => state.open,
  open => {
    if (open && state.mode === 'prompt') {
      resetPrompt()
    }
  }
)

const handleConfirm = () => {
  if (state.mode === 'prompt') {
    if (state.inputRequired && !inputValue.value.trim()) {
      errorMessage.value = '请输入内容'
      return
    }
    closeDialog(inputValue.value.trim())
    return
  }
  closeDialog(true)
}

const handleCancel = () => {
  closeDialog(state.mode === 'confirm' ? false : null)
}

const handleBackdrop = () => {
  if (state.mode === 'alert') {
    closeDialog(true)
    return
  }
  handleCancel()
}
</script>
