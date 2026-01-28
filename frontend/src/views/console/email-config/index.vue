<template>
  <div class="h-full flex flex-col gap-4 p-6">
    <div>
      <h1 class="text-2xl font-bold text-slate-800">邮箱管理</h1>
      <p class="text-slate-500 mt-1">配置系统邮箱和允许认证的邮箱域名</p>
    </div>

    <!-- Tabs -->
    <div class="tabs tabs-boxed w-fit">
      <a class="tab" :class="{ 'tab-active': activeTab === 'smtp' }" @click="activeTab = 'smtp'">邮箱配置</a>
      <a class="tab" :class="{ 'tab-active': activeTab === 'templates' }" @click="activeTab = 'templates'">邮件模板</a>
      <a class="tab" :class="{ 'tab-active': activeTab === 'domains' }" @click="activeTab = 'domains'">域名白名单</a>
      <a class="tab" :class="{ 'tab-active': activeTab === 'studentIds' }" @click="activeTab = 'studentIds'">学号白名单</a>
    </div>

    <!-- SMTP Config Tab -->
    <div v-show="activeTab === 'smtp'" class="flex-1 overflow-auto">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title text-lg">SMTP 邮箱配置</h2>
            <button class="btn btn-sm btn-primary" @click="saveSmtpConfig" :disabled="savingSmtp">
              <span v-if="savingSmtp" class="loading loading-spinner loading-xs"></span>
              保存配置
            </button>
          </div>
          <p class="text-sm text-slate-500 mb-4">配置用于发送验证邮件、通知邮件的系统邮箱</p>
          
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="form-control">
              <label class="label"><span class="label-text">SMTP 服务器</span></label>
              <input v-model="smtpConfig.host" type="text" class="input input-bordered" placeholder="smtp.example.com" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">端口</span></label>
              <input v-model="smtpConfig.port" type="number" class="input input-bordered" placeholder="465" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">发件邮箱</span></label>
              <input v-model="smtpConfig.username" type="email" class="input input-bordered" placeholder="noreply@example.com" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">邮箱密码/授权码</span></label>
              <input v-model="smtpConfig.password" type="password" class="input input-bordered" placeholder="••••••••" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">发件人名称</span></label>
              <input v-model="smtpConfig.fromName" type="text" class="input input-bordered" placeholder="校园墙" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">安全连接</span></label>
              <label class="label cursor-pointer justify-start gap-3 h-12 border border-base-300 rounded-lg px-4">
                <input v-model="smtpConfig.ssl" type="checkbox" class="checkbox checkbox-primary" />
                <span class="label-text">启用 SSL/TLS</span>
              </label>
            </div>
          </div>

          <div class="divider"></div>
          <div class="flex items-center gap-2">
            <input v-model="testEmail" type="email" class="input input-bordered input-sm flex-1 max-w-xs" placeholder="测试邮箱地址" />
            <button class="btn btn-sm btn-outline" @click="sendTestEmail" :disabled="sendingTest">
              <span v-if="sendingTest" class="loading loading-spinner loading-xs"></span>
              发送测试邮件
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Templates Tab -->
    <div v-show="activeTab === 'templates'" class="flex-1 overflow-auto space-y-4">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title text-lg">邮件模板配置</h2>
            <button class="btn btn-sm btn-primary" @click="saveTemplates" :disabled="savingTemplates">
              <span v-if="savingTemplates" class="loading loading-spinner loading-xs"></span>
              保存模板
            </button>
          </div>
          <p class="text-sm text-slate-500 mb-4">
            配置系统发送的邮件内容，支持以下变量：
            <code class="bg-base-200 px-1 rounded" v-pre>{{code}}</code> 验证码、
            <code class="bg-base-200 px-1 rounded" v-pre>{{username}}</code> 用户名、
            <code class="bg-base-200 px-1 rounded" v-pre>{{email}}</code> 邮箱、
            <code class="bg-base-200 px-1 rounded" v-pre>{{expireMinutes}}</code> 过期时间、
            <code class="bg-base-200 px-1 rounded" v-pre>{{loginTime}}</code> 登录时间、
            <code class="bg-base-200 px-1 rounded" v-pre>{{ip}}</code> IP地址、
            <code class="bg-base-200 px-1 rounded" v-pre>{{device}}</code> 设备、
            <code class="bg-base-200 px-1 rounded" v-pre>{{location}}</code> 地点 等
          </p>

          <!-- 动态渲染所有模板 -->
          <div v-for="(config, index) in templateConfig" :key="config.key" 
               class="collapse collapse-arrow bg-base-200" :class="{ 'mb-4': index < templateConfig.length - 1 }">
            <input type="checkbox" :checked="index === 0" />
            <div class="collapse-title font-medium">
              {{ config.name }}
              <span class="text-xs text-slate-500 ml-2">{{ config.desc }}</span>
            </div>
            <div class="collapse-content" v-if="templates[config.key]">
              <div class="form-control mb-3">
                <label class="label"><span class="label-text">邮件标题</span></label>
                <input v-model="templates[config.key].subject" type="text" class="input input-bordered" />
              </div>
              <div class="form-control">
                <label class="label"><span class="label-text">邮件内容</span></label>
                <textarea v-model="templates[config.key].body" class="textarea textarea-bordered h-32"></textarea>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Domains Tab -->
    <div v-show="activeTab === 'domains'" class="flex-1 overflow-auto space-y-4">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title text-lg">允许的邮箱域名</h2>
            <div class="flex gap-2">
              <button class="btn btn-sm btn-outline" @click="showBatchModal = true">批量添加</button>
              <button class="btn btn-sm btn-error btn-outline" @click="batchDelete" :disabled="selectedDomains.length === 0">
                批量删除 ({{ selectedDomains.length }})
              </button>
              <button class="btn btn-sm btn-primary" @click="saveDomains" :disabled="saving">
                <span v-if="saving" class="loading loading-spinner loading-xs"></span>
                保存
              </button>
            </div>
          </div>
          
          <div class="overflow-x-auto">
            <table class="table table-sm">
              <thead>
                <tr>
                  <th><input type="checkbox" class="checkbox checkbox-sm" v-model="selectAll" /></th>
                  <th>域名</th>
                  <th class="w-20">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(domain, index) in domains" :key="index">
                  <td><input type="checkbox" class="checkbox checkbox-sm" :value="index" v-model="selectedDomains" /></td>
                  <td>
                    <input v-model="domains[index]" type="text" class="input input-bordered input-sm w-full max-w-xs" placeholder="edu.cn" />
                  </td>
                  <td>
                    <button class="btn btn-ghost btn-xs text-error" @click="removeDomain(index)">删除</button>
                  </td>
                </tr>
                <tr v-if="domains.length === 0">
                  <td colspan="3" class="text-center text-slate-400">暂无域名配置</td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <button class="btn btn-outline btn-sm w-fit mt-2" @click="addDomain">+ 添加域名</button>
        </div>
      </div>
    </div>

    <!-- Student ID Whitelist Tab -->
    <div v-show="activeTab === 'studentIds'" class="flex-1 overflow-auto space-y-4">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title text-lg">学号白名单</h2>
            <div class="flex gap-2">
              <button class="btn btn-sm btn-outline" @click="showStudentBatchModal = true">批量添加</button>
              <button class="btn btn-sm btn-error btn-outline" @click="batchDeleteStudentIds" :disabled="selectedStudentIds.length === 0">
                批量删除 ({{ selectedStudentIds.length }})
              </button>
              <button class="btn btn-sm btn-primary" @click="saveStudentIds" :disabled="savingStudentIds">
                <span v-if="savingStudentIds" class="loading loading-spinner loading-xs"></span>
                保存
              </button>
            </div>
          </div>

          <div class="overflow-x-auto">
            <table class="table table-sm">
              <thead>
                <tr>
                  <th><input type="checkbox" class="checkbox checkbox-sm" v-model="selectAllStudentIds" /></th>
                  <th>学号</th>
                  <th class="w-20">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(studentId, index) in studentIds" :key="index">
                  <td><input type="checkbox" class="checkbox checkbox-sm" :value="index" v-model="selectedStudentIds" /></td>
                  <td>
                    <input v-model="studentIds[index]" type="text" class="input input-bordered input-sm w-full max-w-xs" placeholder="2024000001" />
                  </td>
                  <td>
                    <button class="btn btn-ghost btn-xs text-error" @click="removeStudentId(index)">删除</button>
                  </td>
                </tr>
                <tr v-if="studentIds.length === 0">
                  <td colspan="3" class="text-center text-slate-400">暂无学号配置</td>
                </tr>
              </tbody>
            </table>
          </div>

          <button class="btn btn-outline btn-sm w-fit mt-2" @click="addStudentId">+ 添加学号</button>
        </div>
      </div>
    </div>

    <!-- Batch Add Modal -->
    <dialog :class="{ 'modal modal-open': showBatchModal, 'modal': !showBatchModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">批量添加域名</h3>
        <p class="text-sm text-slate-500 mb-2">每行一个域名</p>
        <textarea v-model="batchInput" class="textarea textarea-bordered w-full h-40" placeholder="edu.cn&#10;edu.com&#10;ac.cn"></textarea>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showBatchModal = false">取消</button>
          <button class="btn btn-primary" @click="batchAdd">添加</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop" @click="showBatchModal = false"></form>
    </dialog>

    <dialog :class="{ 'modal modal-open': showStudentBatchModal, 'modal': !showStudentBatchModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">批量添加学号</h3>
        <p class="text-sm text-slate-500 mb-2">每行一个学号</p>
        <textarea v-model="studentBatchInput" class="textarea textarea-bordered w-full h-40" placeholder="2024000001&#10;2024000002"></textarea>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showStudentBatchModal = false">取消</button>
          <button class="btn btn-primary" @click="batchAddStudentIds">添加</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop" @click="showStudentBatchModal = false"></form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getEmailDomains, updateEmailDomains, getSmtpConfig, updateSmtpConfig, sendTestSmtpEmail, getEmailTemplates, updateEmailTemplates, getStudentIdWhitelist, updateStudentIdWhitelist } from '@/api/system'
import { useDialog } from '@/composables/useDialog'

const dialog = useDialog()
const activeTab = ref<'smtp' | 'templates' | 'domains' | 'studentIds'>('smtp')

// Email templates
const savingTemplates = ref(false)
const templates = ref<Record<string, { subject: string; body: string }>>({
  verification: {
    subject: '【校园墙】您的验证码',
    body: '您好，\n\n您的验证码是：{{code}}\n\n验证码有效期为 {{expireMinutes}} 分钟，请尽快使用。\n\n如非本人操作，请忽略此邮件。'
  },
  welcome: {
    subject: '【校园墙】欢迎注册',
    body: '亲爱的 {{username}}，\n\n欢迎加入校园墙！\n\n您的账号已注册成功，现在可以开始使用我们的服务了。\n\n祝您使用愉快！'
  },
  resetPassword: {
    subject: '【校园墙】密码重置',
    body: '您好，\n\n您正在重置密码，验证码是：{{code}}\n\n验证码有效期为 {{expireMinutes}} 分钟。\n\n如非本人操作，请立即修改密码。'
  },
  loginAlert: {
    subject: '【校园墙】异地登录提醒',
    body: '亲爱的 {{username}}，\n\n您的账号于 {{loginTime}} 在新设备上登录：\n\nIP地址：{{ip}}\n设备信息：{{device}}\n登录地点：{{location}}\n\n如果这不是您本人的操作，请立即修改密码并检查账号安全。\n\n校园墙安全中心'
  },
  passwordChanged: {
    subject: '【校园墙】密码修改成功',
    body: '亲爱的 {{username}}，\n\n您的密码已于 {{changeTime}} 修改成功。\n\n如果这不是您本人的操作，请立即联系客服。\n\n校园墙安全中心'
  },
  securityAlert: {
    subject: '【校园墙】账号安全警告',
    body: '亲爱的 {{username}}，\n\n我们检测到您的账号存在安全风险：\n\n{{alertContent}}\n\n请及时处理以保护您的账号安全。\n\n校园墙安全中心'
  },
  bindEmail: {
    subject: '【校园墙】邮箱绑定验证',
    body: '您好，\n\n您正在绑定邮箱 {{email}}，验证码是：{{code}}\n\n验证码有效期为 {{expireMinutes}} 分钟。\n\n如非本人操作，请忽略此邮件。'
  },
  verifyApproved: {
    subject: '【校园墙】身份认证已通过',
    body: '亲爱的 {{username}}，\n\n恭喜！您的身份认证已通过审核。\n\n现在您可以享受完整的校园墙服务了。\n\n校园墙团队'
  },
  verifyRejected: {
    subject: '【校园墙】身份认证未通过',
    body: '亲爱的 {{username}}，\n\n很抱歉，您的身份认证未通过审核。\n\n拒绝原因：{{rejectReason}}\n\n请修改后重新提交认证申请。\n\n校园墙团队'
  },
  notification: {
    subject: '【校园墙】系统通知',
    body: '亲爱的 {{username}}，\n\n{{content}}\n\n校园墙团队'
  }
})

// 模板配置
const templateConfig = [
  { key: 'verification', name: '验证码邮件', desc: '发送验证码时使用' },
  { key: 'welcome', name: '注册欢迎邮件', desc: '用户注册成功时发送' },
  { key: 'resetPassword', name: '密码重置邮件', desc: '用户重置密码时发送' },
  { key: 'loginAlert', name: '异地登录提醒', desc: '检测到异地登录时发送' },
  { key: 'passwordChanged', name: '密码修改成功', desc: '密码修改成功后发送' },
  { key: 'securityAlert', name: '账号安全警告', desc: '账号存在安全风险时发送' },
  { key: 'bindEmail', name: '邮箱绑定验证', desc: '绑定新邮箱时发送' },
  { key: 'verifyApproved', name: '身份认证通过', desc: '身份认证审核通过时发送' },
  { key: 'verifyRejected', name: '身份认证拒绝', desc: '身份认证审核拒绝时发送' },
  { key: 'notification', name: '系统通知', desc: '通用系统通知' }
]

// Domain management
const loading = ref(false)
const saving = ref(false)
const domains = ref<string[]>([])
const selectedDomains = ref<number[]>([])
const showBatchModal = ref(false)
const batchInput = ref('')
const studentIds = ref<string[]>([])
const selectedStudentIds = ref<number[]>([])
const showStudentBatchModal = ref(false)
const studentBatchInput = ref('')
const savingStudentIds = ref(false)

const selectAllStudentIds = computed({
  get: () => selectedStudentIds.value.length === studentIds.value.length && studentIds.value.length > 0,
  set: (val: boolean) => {
    selectedStudentIds.value = val ? studentIds.value.map((_, i) => i) : []
  }
})

const selectAll = computed({
  get: () => selectedDomains.value.length === domains.value.length && domains.value.length > 0,
  set: (val: boolean) => {
    selectedDomains.value = val ? domains.value.map((_, i) => i) : []
  }
})

// SMTP config
const savingSmtp = ref(false)
const sendingTest = ref(false)
const testEmail = ref('')
const smtpConfig = ref({
  host: '',
  port: 465,
  username: '',
  password: '',
  fromName: '',
  ssl: true
})

const loadDomains = async () => {
  loading.value = true
  try {
    const res: any = await getEmailDomains()
    domains.value = Array.isArray(res) ? res : ['edu.cn']
  } catch (e: any) {
    console.error('Failed to load domains', e)
    domains.value = ['edu.cn']
  } finally {
    loading.value = false
  }
}

const loadStudentIds = async () => {
  try {
    const res: any = await getStudentIdWhitelist()
    studentIds.value = Array.isArray(res) ? res : []
  } catch (e: any) {
    console.error('Failed to load student ids', e)
    studentIds.value = []
  }
}

const loadSmtpConfig = async () => {
  try {
    const res: any = await getSmtpConfig()
    if (res) {
      smtpConfig.value = { ...smtpConfig.value, ...res }
    }
  } catch (e: any) {
    console.error('Failed to load SMTP config', e)
  }
}

const addDomain = () => {
  domains.value.push('')
}

const removeDomain = (index: number) => {
  domains.value.splice(index, 1)
  selectedDomains.value = selectedDomains.value.filter(i => i !== index).map(i => i > index ? i - 1 : i)
}

const batchAdd = () => {
  const newDomains = batchInput.value.split('\n').map(d => d.trim()).filter(d => d)
  domains.value.push(...newDomains)
  batchInput.value = ''
  showBatchModal.value = false
}

const batchDelete = () => {
  const toDelete = new Set(selectedDomains.value)
  domains.value = domains.value.filter((_, i) => !toDelete.has(i))
  selectedDomains.value = []
}

const saveDomains = async () => {
  const validDomains = domains.value.filter(d => d.trim())
  if (validDomains.length === 0) {
    await dialog.alert('请至少添加一个域名')
    return
  }
  saving.value = true
  try {
    await updateEmailDomains(validDomains)
    await dialog.alert('保存成功')
    domains.value = validDomains
    selectedDomains.value = []
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const addStudentId = () => {
  studentIds.value.push('')
}

const removeStudentId = (index: number) => {
  studentIds.value.splice(index, 1)
  selectedStudentIds.value = selectedStudentIds.value.filter(i => i !== index).map(i => i > index ? i - 1 : i)
}

const batchAddStudentIds = () => {
  const list = studentBatchInput.value.split('\n').map(item => item.trim()).filter(item => item)
  studentIds.value.push(...list)
  studentBatchInput.value = ''
  showStudentBatchModal.value = false
}

const batchDeleteStudentIds = () => {
  const toDelete = new Set(selectedStudentIds.value)
  studentIds.value = studentIds.value.filter((_, i) => !toDelete.has(i))
  selectedStudentIds.value = []
}

const saveStudentIds = async () => {
  const valid = studentIds.value.map(item => item.trim()).filter(item => item)
  savingStudentIds.value = true
  try {
    await updateStudentIdWhitelist(valid)
    studentIds.value = valid
    selectedStudentIds.value = []
    await dialog.alert('保存成功')
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '保存失败')
  } finally {
    savingStudentIds.value = false
  }
}

const saveSmtpConfig = async () => {
  savingSmtp.value = true
  try {
    await updateSmtpConfig(smtpConfig.value)
    await dialog.alert('保存成功')
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '保存失败')
  } finally {
    savingSmtp.value = false
  }
}

const sendTestEmail = async () => {
  if (!testEmail.value) {
    await dialog.alert('请输入测试邮箱地址')
    return
  }
  sendingTest.value = true
  try {
    await sendTestSmtpEmail(testEmail.value)
    await dialog.alert('测试邮件已发送，请检查收件箱')
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '发送失败')
  } finally {
    sendingTest.value = false
  }
}

const loadTemplates = async () => {
  try {
    const res: any = await getEmailTemplates()
    if (res) {
      templates.value = { ...templates.value, ...res }
    }
  } catch (e: any) {
    console.error('Failed to load templates', e)
  }
}

const saveTemplates = async () => {
  savingTemplates.value = true
  try {
    await updateEmailTemplates(templates.value)
    await dialog.alert('保存成功')
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '保存失败')
  } finally {
    savingTemplates.value = false
  }
}

onMounted(() => {
  loadDomains()
  loadStudentIds()
  loadSmtpConfig()
  loadTemplates()
})
</script>
