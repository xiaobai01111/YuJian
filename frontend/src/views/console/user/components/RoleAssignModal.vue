<template>
  <dialog ref="modalRef" class="modal">
    <div class="modal-box">
      <form method="dialog">
        <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">✕</button>
      </form>
      <h3 class="font-bold text-lg mb-2">{{ isBatch ? '批量分配角色' : '分配角色' }}</h3>
      <p class="text-sm text-base-content/60 mb-4">
        <template v-if="isBatch">
          为 <span class="font-semibold text-primary">{{ batchUserNames.join('、') }}</span> 等 {{ batchUserIds.length }} 个用户分配角色
        </template>
        <template v-else>
          为用户 <span class="font-semibold text-primary">{{ currentUser?.nickname || currentUser?.username }}</span> 分配角色
        </template>
      </p>
      
      <div class="border border-base-200 rounded-lg p-3 max-h-60 overflow-y-auto">
        <div v-if="enabledRoles.length === 0" class="text-center text-base-content/60 py-4">
          暂无可用角色
        </div>
        <label v-else class="flex items-center gap-3 p-2 hover:bg-base-200 rounded cursor-pointer" v-for="role in enabledRoles" :key="role.id">
          <input type="checkbox" class="checkbox checkbox-primary checkbox-sm" :value="role.id" v-model="selectedRoleIds" />
          <div class="flex-1">
            <div class="font-medium text-sm">{{ role.roleName }}</div>
            <div class="text-xs text-base-content/60">{{ role.roleKey }}</div>
          </div>
        </label>
      </div>

      <div class="modal-action">
        <button class="btn btn-ghost" @click="close">取消</button>
        <button class="btn btn-primary" @click="handleSubmit" :disabled="loading">
          <span v-if="loading" class="loading loading-spinner loading-sm"></span>
          确定
        </button>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop">
      <button>close</button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { UserVO, RoleVO } from '@/api/system'
import { updateUserRole, batchUpdateUserRole } from '@/api/system'

const props = defineProps<{
  roleList: RoleVO[]
}>()

// 只显示启用状态的角色 (status=0)
const enabledRoles = computed(() => props.roleList.filter(r => r.status === 0))

const emit = defineEmits<{
  (e: 'success'): void
}>()

const modalRef = ref<HTMLDialogElement>()
const loading = ref(false)
const currentUser = ref<UserVO | null>(null)
const selectedRoleIds = ref<number[]>([])
const isBatch = ref(false)
const batchUserIds = ref<number[]>([])
const batchUserNames = ref<string[]>([])

const open = (user: UserVO) => {
  isBatch.value = false
  currentUser.value = user
  batchUserIds.value = []
  batchUserNames.value = []
  // 匹配用户现有角色（按角色名匹配，移除可能的"(已禁用)"后缀）
  selectedRoleIds.value = enabledRoles.value
    .filter(r => user.roles?.some(ur => ur.replace('(已禁用)', '') === r.roleName))
    .map(r => r.id)
  modalRef.value?.showModal()
}

const openBatch = (userIds: number[], userNames: string[]) => {
  isBatch.value = true
  currentUser.value = null
  batchUserIds.value = userIds
  batchUserNames.value = userNames.slice(0, 3) // 只显示前3个名字
  selectedRoleIds.value = []
  modalRef.value?.showModal()
}

const close = () => {
  modalRef.value?.close()
  currentUser.value = null
  isBatch.value = false
  batchUserIds.value = []
  batchUserNames.value = []
}

const handleSubmit = async () => {
  loading.value = true
  try {
    if (isBatch.value) {
      await batchUpdateUserRole(batchUserIds.value, selectedRoleIds.value)
    } else if (currentUser.value) {
      await updateUserRole(currentUser.value.id, selectedRoleIds.value)
    }
    emit('success')
    close()
  } catch (error) {
    console.error(error)
    alert('分配角色失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open, openBatch, close })
</script>
