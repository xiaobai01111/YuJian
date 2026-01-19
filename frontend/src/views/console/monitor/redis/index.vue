<template>
  <div class="flex flex-col gap-4 h-full overflow-y-auto pr-2">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">Redis监控</h2>
      <button class="btn btn-sm btn-ghost" @click="fetchData">刷新</button>
    </div>

    <div class="card bg-base-100 shadow-xl">
      <div class="card-body">
        <h3 class="font-semibold">基本信息</h3>
        <div class="overflow-x-auto">
          <table class="table">
            <tbody>
              <tr>
                <td>Redis版本</td>
                <td>{{ data?.basic.version ?? '-' }}</td>
                <td>运行模式</td>
                <td>{{ data?.basic.runMode ?? '-' }}</td>
                <td>端口</td>
                <td>{{ data?.basic.port ?? '-' }}</td>
                <td>客户端数</td>
                <td>{{ data?.basic.connectedClients ?? '-' }}</td>
              </tr>
              <tr>
                <td>运行时间(天)</td>
                <td>{{ data?.basic.uptimeDays ?? '-' }}</td>
                <td>使用内存</td>
                <td>{{ data?.basic.usedMemory ?? '-' }}</td>
                <td>使用CPU</td>
                <td>{{ data?.basic.usedCpu ?? '-' }}</td>
                <td>内存配置</td>
                <td>{{ data?.basic.maxMemory ?? '-' }}</td>
              </tr>
              <tr>
                <td>AOF是否开启</td>
                <td>{{ data?.basic.aofEnabled ?? '-' }}</td>
                <td>RDB是否成功</td>
                <td>{{ data?.basic.rdbStatus ?? '-' }}</td>
                <td>键数量</td>
                <td>{{ data?.basic.keyCount ?? '-' }}</td>
                <td>网络入口/出口</td>
                <td>{{ formatNetwork }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="font-semibold">内存信息</h3>
          <div class="overflow-x-auto max-h-[320px] overflow-y-auto">
            <table class="table">
              <tbody>
                <tr>
                  <td>使用内存</td>
                  <td>{{ data?.memory.used ?? '-' }}</td>
                </tr>
                <tr>
                  <td>峰值内存</td>
                  <td>{{ data?.memory.usedPeak ?? '-' }}</td>
                </tr>
                <tr>
                  <td>常驻内存</td>
                  <td>{{ data?.memory.usedRss ?? '-' }}</td>
                </tr>
                <tr>
                  <td>Lua内存</td>
                  <td>{{ data?.memory.usedLua ?? '-' }}</td>
                </tr>
                <tr>
                  <td>碎片率</td>
                  <td>{{ data?.memory.fragmentationRatio ?? '-' }}</td>
                </tr>
                <tr>
                  <td>最大内存</td>
                  <td>{{ data?.memory.max ?? '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h3 class="font-semibold">命令统计</h3>
          <div class="overflow-x-auto max-h-[320px] overflow-y-auto">
            <table class="table">
              <thead>
                <tr>
                  <th>命令</th>
                  <th>调用次数</th>
                  <th>总耗时(微秒)</th>
                  <th>平均耗时(微秒)</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="4" class="text-center py-4">加载中...</td>
                </tr>
                <tr v-else-if="!data?.commandStats?.length">
                  <td colspan="4" class="text-center py-4">暂无数据</td>
                </tr>
                <tr v-else v-for="stat in data.commandStats" :key="stat.command">
                  <td>{{ stat.command }}</td>
                  <td>{{ stat.calls ?? '-' }}</td>
                  <td>{{ stat.usec ?? '-' }}</td>
                  <td>{{ stat.usecPerCall?.toFixed(2) ?? '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getRedisMonitor, type RedisMonitorVO } from '@/api/system'

const loading = ref(false)
const data = ref<RedisMonitorVO | null>(null)
let refreshTimer: number | null = null

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
    const res = await getRedisMonitor()
    data.value = res || null
  } catch (error: any) {
    data.value = null
    alert(error?.message || error?.response?.data?.message || '获取Redis监控失败')
  } finally {
    loading.value = false
  }
}

const formatNetwork = computed(() => {
  if (!data.value?.basic) return '-'
  const input = data.value.basic.networkInput || '-'
  const output = data.value.basic.networkOutput || '-'
  return `${input}/${output}`
})
</script>
