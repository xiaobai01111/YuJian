<template>
  <div class="p-6">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-slate-800">示例插件</h1>
      <p class="text-slate-500 mt-1">这是插件的主页面</p>
    </div>

    <div class="card bg-base-100 shadow-sm">
      <div class="card-body">
        <h2 class="card-title">插件功能</h2>
        
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">输入内容</span>
          </label>
          <input 
            v-model="inputValue" 
            type="text" 
            placeholder="请输入..." 
            class="input input-bordered w-full max-w-xs"
          />
        </div>

        <div class="mt-4">
          <button class="btn btn-primary" @click="handleSubmit">
            提交
          </button>
        </div>

        <div v-if="result" class="mt-4 p-4 bg-base-200 rounded-lg">
          <pre>{{ result }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'

const inputValue = ref('')
const result = ref<any>(null)

// 注入插件上下文
const pluginApi = inject<any>('pluginApi')

const handleSubmit = async () => {
  try {
    const res = await pluginApi?.post('/api/plugins/my-plugin/process', {
      input: inputValue.value
    })
    result.value = res
  } catch (e: any) {
    console.error('请求失败', e)
  }
}
</script>
