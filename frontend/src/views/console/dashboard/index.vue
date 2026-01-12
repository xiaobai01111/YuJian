<template>
  <div class="space-y-6">
    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title text-sm text-slate-500">总用户数</h2>
          <div class="flex items-end justify-between">
            <span class="text-3xl font-bold">12,345</span>
            <span class="text-xs text-success flex items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" /></svg>
              +12%
            </span>
          </div>
        </div>
      </div>
      
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title text-sm text-slate-500">今日发帖</h2>
          <div class="flex items-end justify-between">
            <span class="text-3xl font-bold">148</span>
            <span class="text-xs text-success flex items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" /></svg>
              +5%
            </span>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title text-sm text-slate-500">待审核</h2>
          <div class="flex items-end justify-between">
            <span class="text-3xl font-bold">23</span>
            <span class="text-xs text-warning">需处理</span>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title text-sm text-slate-500">系统状态</h2>
          <div class="flex items-end justify-between">
            <span class="text-3xl font-bold text-success">正常</span>
            <span class="text-xs text-slate-400">运行中</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Activity & Quick Actions -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Recent Activity -->
      <div class="card bg-base-100 shadow-xl lg:col-span-2">
        <div class="card-body">
          <h2 class="card-title mb-4">最近活动</h2>
          <div class="overflow-x-auto">
            <table class="table table-zebra">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>操作</th>
                  <th>时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="i in 5" :key="i">
                  <td class="flex items-center gap-2">
                    <div class="avatar placeholder">
                      <div class="bg-neutral-focus text-neutral-content rounded-full w-8 h-8">
                        <span class="text-xs">U{{ i }}</span>
                      </div>
                    </div>
                    <span>用户 {{ i }}</span>
                  </td>
                  <td>发布了新帖子</td>
                  <td class="text-slate-500 text-sm">10分钟前</td>
                  <td><span class="badge badge-success badge-sm">成功</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">快捷操作</h2>
          <div class="grid grid-cols-2 gap-4">
            <button class="btn btn-outline btn-primary h-24 flex flex-col gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
              发布公告
            </button>
            <button class="btn btn-outline h-24 flex flex-col gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
              审核日志
            </button>
            <button class="btn btn-outline h-24 flex flex-col gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
              系统设置
            </button>
            <button class="btn btn-outline text-error h-24 flex flex-col gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
              清理缓存
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Dashboard logic here
</script>
