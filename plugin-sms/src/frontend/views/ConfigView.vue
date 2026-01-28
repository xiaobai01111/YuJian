<template>
  <div class="p-6 space-y-6">
    <!-- 标题 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">短信服务配置</h1>
        <p class="text-slate-500 mt-1">配置短信验证码服务，支持阿里云、腾讯云</p>
      </div>
    </div>

    <!-- 服务商选择 -->
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body">
        <h2 class="card-title text-lg">服务商</h2>
        <div class="form-control">
          <div class="flex gap-4">
            <label class="label cursor-pointer gap-2">
              <input 
                type="radio" 
                name="provider" 
                class="radio radio-primary" 
                value="aliyun"
                v-model="config.provider"
              />
              <span class="label-text">阿里云短信</span>
            </label>
            <label class="label cursor-pointer gap-2">
              <input 
                type="radio" 
                name="provider" 
                class="radio radio-primary" 
                value="tencent"
                v-model="config.provider"
              />
              <span class="label-text">腾讯云短信</span>
            </label>
          </div>
        </div>
      </div>
    </div>

    <!-- 阿里云配置 -->
    <div v-if="config.provider === 'aliyun'" class="card bg-base-100 shadow-sm">
      <div class="card-body">
        <h2 class="card-title text-lg">阿里云配置</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="form-control">
            <label class="label"><span class="label-text">AccessKey ID</span></label>
            <input 
              v-model="config.aliyun.accessKeyId" 
              type="text" 
              class="input input-bordered" 
              placeholder="LTAI..."
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">AccessKey Secret</span></label>
            <input 
              v-model="config.aliyun.accessKeySecret" 
              type="password" 
              class="input input-bordered" 
              placeholder="留空表示不修改"
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">短信签名</span></label>
            <input 
              v-model="config.aliyun.signName" 
              type="text" 
              class="input input-bordered" 
              placeholder="如：校园墙"
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">模板Code</span></label>
            <input 
              v-model="config.aliyun.templateCode" 
              type="text" 
              class="input input-bordered" 
              placeholder="SMS_123456789"
            />
          </div>
        </div>
        <div class="text-sm text-slate-500 mt-2">
          <a href="https://dysms.console.aliyun.com" target="_blank" class="link link-primary">
            前往阿里云短信控制台 →
          </a>
        </div>
      </div>
    </div>

    <!-- 腾讯云配置 -->
    <div v-if="config.provider === 'tencent'" class="card bg-base-100 shadow-sm">
      <div class="card-body">
        <h2 class="card-title text-lg">腾讯云配置</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="form-control">
            <label class="label"><span class="label-text">SecretId</span></label>
            <input 
              v-model="config.tencent.secretId" 
              type="text" 
              class="input input-bordered" 
              placeholder="AKID..."
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">SecretKey</span></label>
            <input 
              v-model="config.tencent.secretKey" 
              type="password" 
              class="input input-bordered" 
              placeholder="留空表示不修改"
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">应用 AppId</span></label>
            <input 
              v-model="config.tencent.appId" 
              type="text" 
              class="input input-bordered" 
              placeholder="1400123456"
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">短信签名</span></label>
            <input 
              v-model="config.tencent.signName" 
              type="text" 
              class="input input-bordered" 
              placeholder="如：校园墙"
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">模板ID</span></label>
            <input 
              v-model="config.tencent.templateId" 
              type="text" 
              class="input input-bordered" 
              placeholder="123456"
            />
          </div>
        </div>
        <div class="text-sm text-slate-500 mt-2">
          <a href="https://console.cloud.tencent.com/smsv2" target="_blank" class="link link-primary">
            前往腾讯云短信控制台 →
          </a>
        </div>
      </div>
    </div>

    <!-- 通用配置 -->
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body">
        <h2 class="card-title text-lg">通用配置</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div class="form-control">
            <label class="label"><span class="label-text">验证码长度</span></label>
            <input 
              v-model.number="config.codeLength" 
              type="number" 
              class="input input-bordered" 
              min="4" max="8"
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">有效期(分钟)</span></label>
            <input 
              v-model.number="config.codeExpireMinutes" 
              type="number" 
              class="input input-bordered" 
              min="1" max="30"
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">每日单号码上限</span></label>
            <input 
              v-model.number="config.dailyLimitPerPhone" 
              type="number" 
              class="input input-bordered" 
              min="1" max="100"
            />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">每日单IP上限</span></label>
            <input 
              v-model.number="config.ipDailyLimit" 
              type="number" 
              class="input input-bordered" 
              min="10" max="500"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex gap-4">
      <button 
        class="btn btn-primary" 
        :disabled="saving"
        @click="saveConfig"
      >
        <span v-if="saving" class="loading loading-spinner loading-sm"></span>
        保存配置
      </button>
      <button 
        class="btn btn-outline" 
        @click="showTestModal = true"
      >
        发送测试短信
      </button>
    </div>

    <!-- 测试弹窗 -->
    <dialog :class="['modal', { 'modal-open': showTestModal }]">
      <div class="modal-box">
        <h3 class="font-bold text-lg">发送测试短信</h3>
        <div class="py-4">
          <div class="form-control">
            <label class="label"><span class="label-text">手机号</span></label>
            <input 
              v-model="testPhone" 
              type="tel" 
              class="input input-bordered" 
              placeholder="请输入手机号"
            />
          </div>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showTestModal = false">取消</button>
          <button 
            class="btn btn-primary" 
            :disabled="testing || !testPhone"
            @click="sendTest"
          >
            <span v-if="testing" class="loading loading-spinner loading-sm"></span>
            发送
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="showTestModal = false">close</button>
      </form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, inject } from 'vue'

const pluginApi = inject<any>('pluginApi')

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const showTestModal = ref(false)
const testPhone = ref('')

const config = reactive({
  provider: 'aliyun',
  codeLength: 6,
  codeExpireMinutes: 5,
  dailyLimitPerPhone: 10,
  ipDailyLimit: 50,
  aliyun: {
    accessKeyId: '',
    accessKeySecret: '',
    signName: '',
    templateCode: ''
  },
  tencent: {
    secretId: '',
    secretKey: '',
    appId: '',
    signName: '',
    templateId: ''
  }
})

const loadConfig = async () => {
  loading.value = true
  try {
    const res = await pluginApi?.get('/api/plugins/sms/config')
    if (res?.success && res.data) {
      Object.assign(config, res.data)
    }
  } catch (e) {
    console.error('加载配置失败', e)
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    const res = await pluginApi?.put('/api/plugins/sms/config', config)
    if (res?.success) {
      alert('保存成功')
    } else {
      alert(res?.message || '保存失败')
    }
  } catch (e: any) {
    alert(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const sendTest = async () => {
  if (!testPhone.value) return
  testing.value = true
  try {
    const res = await pluginApi?.post('/api/plugins/sms/test', { phone: testPhone.value })
    if (res?.success) {
      alert('测试短信已发送')
      showTestModal.value = false
      testPhone.value = ''
    } else {
      alert(res?.message || '发送失败')
    }
  } catch (e: any) {
    alert(e.message || '发送失败')
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>
