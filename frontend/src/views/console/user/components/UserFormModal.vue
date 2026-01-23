<template>
  <dialog ref="modalRef" class="modal">
    <div class="modal-box w-11/12 max-w-3xl">
      <form method="dialog">
        <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">✕</button>
      </form>
      <h3 class="font-bold text-lg mb-6">{{ isEdit ? '编辑用户' : '添加用户' }}</h3>
      
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <!-- 登录账号 -->
          <div class="form-control">
            <label class="label"><span class="label-text">登录账号 <span class="text-error">*</span></span></label>
            <input type="text" v-model="form.username" class="input input-bordered input-sm" :disabled="isEdit" placeholder="请输入登录账号" />
          </div>
          
          <!-- 用户名称 -->
          <div class="form-control">
            <label class="label"><span class="label-text">用户名称 <span class="text-error">*</span></span></label>
            <input type="text" v-model="form.nickname" class="input input-bordered input-sm" placeholder="请输入用户名称" />
          </div>
          
          <!-- 密码 -->
          <div class="form-control" v-if="!isEdit">
            <label class="label"><span class="label-text">密码 <span class="text-error">*</span></span></label>
            <input type="password" v-model="form.password" class="input input-bordered input-sm" placeholder="请输入密码" />
          </div>
          
          <!-- 确认密码 -->
          <div class="form-control" v-if="!isEdit">
            <label class="label"><span class="label-text">确认密码 <span class="text-error">*</span></span></label>
            <input type="password" v-model="form.confirmPassword" class="input input-bordered input-sm" placeholder="请确认密码" />
          </div>
          
          <!-- 手机号 -->
          <div class="form-control">
            <label class="label"><span class="label-text">手机号</span></label>
            <input type="tel" v-model="form.phone" class="input input-bordered input-sm" placeholder="请输入手机号" />
          </div>
          
          <!-- 邮箱 -->
          <div class="form-control">
            <label class="label"><span class="label-text">邮箱</span></label>
            <input type="email" v-model="form.email" class="input input-bordered input-sm" placeholder="请输入邮箱" />
          </div>
          
          <!-- 分配角色（仅新增时可用，编辑请使用角色分配功能） -->
          <div class="form-control" v-if="!isEdit">
            <label class="label"><span class="label-text">分配角色</span></label>
            <select v-model="form.roleId" class="select select-bordered select-sm">
              <option :value="undefined">请选择角色</option>
              <option v-for="role in roleList" :key="role.id" :value="role.id">{{ role.roleName }}</option>
            </select>
          </div>
          
          <!-- 用户性别 -->
          <div class="form-control">
            <label class="label"><span class="label-text">用户性别</span></label>
            <div class="flex gap-4 h-8 items-center">
              <label class="label cursor-pointer gap-2">
                <input type="radio" name="sex" class="radio radio-sm radio-primary" :value="1" v-model="form.sex" />
                <span class="label-text">男</span>
              </label>
              <label class="label cursor-pointer gap-2">
                <input type="radio" name="sex" class="radio radio-sm radio-primary" :value="2" v-model="form.sex" />
                <span class="label-text">女</span>
              </label>
              <label class="label cursor-pointer gap-2">
                <input type="radio" name="sex" class="radio radio-sm radio-primary" :value="0" v-model="form.sex" />
                <span class="label-text">未知</span>
              </label>
            </div>
          </div>
          
          <!-- 用户状态（仅新增时可用，编辑状态请使用封禁功能） -->
          <div class="form-control" v-if="!isEdit">
            <label class="label"><span class="label-text">用户状态</span></label>
            <div class="flex gap-4 h-8 items-center">
              <label class="label cursor-pointer gap-2">
                <input type="radio" name="status" class="radio radio-sm radio-primary" :value="0" v-model="form.status" />
                <span class="label-text">正常</span>
              </label>
              <label class="label cursor-pointer gap-2">
                <input type="radio" name="status" class="radio radio-sm radio-primary" :value="1" v-model="form.status" />
                <span class="label-text">停用</span>
              </label>
            </div>
          </div>
          
          <!-- 用户头像 -->
          <div class="form-control">
            <label class="label"><span class="label-text">用户头像</span></label>
            <div class="flex items-center gap-4">
              <div class="avatar">
                <div class="w-16 h-16 rounded-lg ring-1 ring-base-300">
                  <img :src="form.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + form.username" alt="Avatar" />
                </div>
              </div>
              <input type="text" v-model="form.avatar" class="input input-bordered input-sm flex-1" placeholder="头像URL" />
            </div>
          </div>
          
          <!-- 用户备注 -->
          <div class="form-control col-span-2">
            <label class="label"><span class="label-text">用户备注</span></label>
            <textarea v-model="form.remark" class="textarea textarea-bordered textarea-sm" rows="2" placeholder="请输入备注信息"></textarea>
          </div>
        </div>
        
        <div class="modal-action">
          <button type="button" class="btn btn-ghost" @click="close">取消</button>
          <button type="submit" class="btn btn-primary" :disabled="loading">
            <span v-if="loading" class="loading loading-spinner loading-sm"></span>
            确定
          </button>
        </div>
      </form>
    </div>
    <form method="dialog" class="modal-backdrop">
      <button>close</button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { UserVO, RoleVO } from '@/api/system'
import { createUser, editUser } from '@/api/system'
import { useDialog } from '@/composables/useDialog'

interface UserForm {
  id?: number
  username: string
  nickname: string
  password: string
  confirmPassword: string
  email: string
  phone: string
  roleId?: number
  sex: number
  status: number
  avatar: string
  remark: string
}

const props = defineProps<{
  roleList: RoleVO[]
}>()

const emit = defineEmits<{
  (e: 'success'): void
}>()

const modalRef = ref<HTMLDialogElement>()
const loading = ref(false)
const isEdit = ref(false)

const initialForm: UserForm = {
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  roleId: undefined,
  sex: 0,
  status: 0,
  avatar: '',
  remark: ''
}

const form = reactive<UserForm>({ ...initialForm })
const dialog = useDialog()

const open = (user?: UserVO) => {
  isEdit.value = !!user
  if (user) {
    Object.assign(form, {
      id: user.id,
      username: user.username,
      nickname: user.nickname,
      password: '',
      confirmPassword: '',
      email: user.email || '',
      phone: user.phone || '',
      roleId: user.roleIds?.[0],
      sex: user.sex || 0,
      status: user.status,
      avatar: user.avatar || '',
      remark: user.remark || ''
    })
  } else {
    Object.assign(form, { ...initialForm })
  }
  modalRef.value?.showModal()
}

const close = () => {
  modalRef.value?.close()
}

const handleSubmit = async () => {
  if (!form.username || !form.nickname) {
    await dialog.alert('请填写必填字段')
    return
  }
  if (!isEdit.value && (!form.password || form.password !== form.confirmPassword)) {
    await dialog.alert('请正确填写密码')
    return
  }
  
  loading.value = true
  try {
    if (isEdit.value && form.id) {
      await editUser(form.id, {
        nickname: form.nickname,
        email: form.email || undefined,
        phone: form.phone || undefined,
        sex: form.sex,
        avatar: form.avatar || undefined,
        remark: form.remark || undefined
      })
    } else {
      await createUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        email: form.email || undefined,
        phone: form.phone || undefined,
        roleId: form.roleId,
        sex: form.sex,
        status: form.status,
        avatar: form.avatar || undefined,
        remark: form.remark || undefined
      })
    }
    emit('success')
    close()
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.msg || '操作失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open, close })
</script>
