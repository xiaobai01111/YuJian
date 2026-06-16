<template>
  <footer class="bg-base-100 border-t border-base-200 pt-16 pb-8">
    <div class="container mx-auto px-4">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-12 mb-16">
        <!-- Brand Section -->
        <div class="col-span-1 md:col-span-1 space-y-4">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-purple-600 to-blue-500 flex items-center justify-center text-white font-bold text-lg shadow-lg">
              C
            </div>
            <span class="text-xl font-bold text-slate-900">CampusWall</span>
          </div>
          <div class="text-sm text-slate-500 space-y-1">
            <p>连接每一份校园心声</p>
            <p>Since 2024</p>
          </div>
        </div>

        <!-- Links: Discovery -->
        <div class="col-span-1">
          <h3 class="font-bold text-purple-600 mb-4">发现</h3>
          <ul class="space-y-3 text-sm text-slate-600">
            <li><a href="#" class="hover:text-primary transition-colors">表白墙</a></li>
            <li><a href="#" class="hover:text-primary transition-colors">跳蚤市场</a></li>
            <li><a href="#" class="hover:text-primary transition-colors">失物招领</a></li>
          </ul>
        </div>

        <!-- Links: Community -->
        <div class="col-span-1">
          <h3 class="font-bold text-purple-600 mb-4">社区</h3>
          <ul class="space-y-3 text-sm text-slate-600">
            <li><a href="#" class="hover:text-primary transition-colors">关于我们</a></li>
            <li><a href="#" class="hover:text-primary transition-colors">社区规范</a></li>
            <li><a href="#" class="hover:text-primary transition-colors">联系管理</a></li>
          </ul>
        </div>

        <!-- Subscription -->
        <div class="col-span-1">
          <h3 class="font-bold text-purple-600 mb-4">订阅</h3>
          <div class="form-control">
            <div class="flex gap-2">
              <input type="email" placeholder="username@site.com" class="input input-bordered input-sm w-full rounded-md focus:outline-none focus:ring-2 focus:ring-primary/20" />
              <button class="btn btn-sm btn-ghost hover:bg-transparent hover:text-primary">订阅</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Copyright / Bottom -->
      <div class="border-t border-base-200 pt-8 flex flex-col md:flex-row justify-between items-center text-xs text-slate-400 gap-4">
        <p>&copy; 2026 CampusWall. All rights reserved.</p>
        <div class="flex gap-4">
          <a href="#" class="hover:text-slate-600">隐私政策</a>
          <a href="#" class="hover:text-slate-600">服务条款</a>
        </div>
      </div>
    </div>
  </footer>
</template>
