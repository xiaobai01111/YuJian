<template>
  <div class="flex flex-col gap-4 h-full overflow-y-auto pr-2">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">服务监控</h2>
      <button class="btn btn-sm btn-ghost" @click="fetchData">刷新</button>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="font-semibold">中央处理器</h3>
          <div class="overflow-x-auto">
            <table class="table">
              <tbody>
                <tr>
                  <td>核心数</td>
                  <td>{{ data?.cpu.coreCount ?? '-' }}</td>
                </tr>
                <tr>
                  <td>用户使用率</td>
                  <td>{{ formatPercent(data?.cpu.userUsage) }}</td>
                </tr>
                <tr>
                  <td>系统使用率</td>
                  <td>{{ formatPercent(data?.cpu.systemUsage) }}</td>
                </tr>
                <tr>
                  <td>当前空闲率</td>
                  <td>{{ formatPercent(data?.cpu.idleUsage) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="font-semibold">内存</h3>
          <div class="overflow-x-auto">
            <table class="table">
              <thead>
                <tr>
                  <th>属性</th>
                  <th>内存</th>
                  <th>JVM</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>总内存</td>
                  <td>{{ data?.memory.total ?? '-' }}</td>
                  <td>{{ data?.jvm.total ?? '-' }}</td>
                </tr>
                <tr>
                  <td>已用内存</td>
                  <td>{{ data?.memory.used ?? '-' }}</td>
                  <td>{{ data?.jvm.used ?? '-' }}</td>
                </tr>
                <tr>
                  <td>剩余内存</td>
                  <td>{{ data?.memory.free ?? '-' }}</td>
                  <td>{{ data?.jvm.free ?? '-' }}</td>
                </tr>
                <tr>
                  <td>使用率</td>
                  <td :class="usageClass(data?.memory.usage)">{{ formatPercent(data?.memory.usage) }}</td>
                  <td :class="usageClass(data?.jvm.usage)">{{ formatPercent(data?.jvm.usage) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="card bg-base-100 shadow-xl">
      <div class="card-body">
        <h3 class="font-semibold">服务器信息</h3>
        <div class="overflow-x-auto max-h-[320px] overflow-y-auto">
          <table class="table">
            <tbody>
              <tr>
                <td>服务器名称</td>
                <td>{{ data?.server.hostName ?? '-' }}</td>
                <td>操作系统</td>
                <td>{{ data?.server.osName ?? '-' }}</td>
              </tr>
              <tr>
                <td>服务器IP</td>
                <td>{{ data?.server.hostIp ?? '-' }}</td>
                <td>系统架构</td>
                <td>{{ data?.server.osArch ?? '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="card bg-base-100 shadow-xl">
      <div class="card-body">
        <h3 class="font-semibold">Java虚拟机信息</h3>
        <div class="overflow-x-auto max-h-[320px] overflow-y-auto">
          <table class="table">
            <tbody>
              <tr>
                <td>Java名称</td>
                <td>{{ data?.javaInfo.javaName ?? '-' }}</td>
                <td>Java版本</td>
                <td>{{ data?.javaInfo.javaVersion ?? '-' }}</td>
              </tr>
              <tr>
                <td>启动时间</td>
                <td>{{ data?.javaInfo.startTime ?? '-' }}</td>
                <td>运行时长</td>
                <td>{{ data?.javaInfo.runTime ?? '-' }}</td>
              </tr>
              <tr>
                <td>安装路径</td>
                <td colspan="3">{{ data?.javaInfo.javaHome ?? '-' }}</td>
              </tr>
              <tr>
                <td>项目路径</td>
                <td colspan="3">{{ data?.javaInfo.projectDir ?? '-' }}</td>
              </tr>
              <tr>
                <td>运行参数</td>
                <td colspan="3" class="break-all">{{ data?.javaInfo.inputArgs || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="card bg-base-100 shadow-xl">
      <div class="card-body">
        <h3 class="font-semibold">磁盘状态</h3>
        <div class="overflow-x-auto max-h-[320px] overflow-y-auto">
          <table class="table">
            <thead>
              <tr>
                <th>盘符路径</th>
                <th>文件系统</th>
                <th>盘符类型</th>
                <th>总尺寸</th>
                <th>可用尺寸</th>
                <th>已用尺寸</th>
                <th>已用百分比</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="text-center py-4">加载中...</td>
              </tr>
              <tr v-else-if="!data?.disks?.length">
                <td colspan="7" class="text-center py-4">暂无数据</td>
              </tr>
              <tr v-else v-for="disk in data.disks" :key="disk.mount">
                <td>{{ disk.mount }}</td>
                <td>{{ disk.fileSystem }}</td>
                <td>{{ disk.diskType || '-' }}</td>
                <td>{{ disk.total }}</td>
                <td>{{ disk.free }}</td>
                <td>{{ disk.used }}</td>
                <td :class="usageClass(disk.usage)">{{ formatPercent(disk.usage) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { getServerMonitor, type ServerMonitorVO } from '@/api/system'
import { useDialog } from '@/composables/useDialog'

const loading = ref(false)
const data = ref<ServerMonitorVO | null>(null)
let refreshTimer: number | null = null
const dialog = useDialog()

onMounted(() => {
  fetchData()
  refreshTimer = window.setInterval(fetchData, 5000)
})

onUnmounted(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
})

const fetchData = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getServerMonitor()
    data.value = res || null
  } catch (error: any) {
    data.value = null
    await dialog.alert(error?.message || error?.response?.data?.message || '获取服务监控失败')
  } finally {
    loading.value = false
  }
}

const formatPercent = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `${value.toFixed(2)}%`
}

const usageClass = (value?: number) => {
  if (value === undefined || value === null) return ''
  if (value >= 90) return 'text-error'
  if (value >= 70) return 'text-warning'
  return 'text-success'
}
</script>
