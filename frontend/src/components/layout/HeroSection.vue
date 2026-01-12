<template>
  <div class="hero min-h-screen bg-base-100 bg-gradient-to-br from-primary/5 via-transparent to-secondary/5 relative overflow-hidden">
    <!-- Background Elements -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-[20%] -left-[10%] w-[600px] h-[600px] bg-primary/10 rounded-full blur-[100px] animate-pulse-soft"></div>
      <div class="absolute top-[20%] right-[10%] w-[500px] h-[500px] bg-secondary/10 rounded-full blur-[100px] animate-pulse-soft" style="animation-delay: 1s;"></div>
    </div>

    <div class="container mx-auto px-4 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center z-10 py-16">
      <!-- Left Content -->
      <div class="text-left space-y-8 animate-fade-in-up">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-base-200 border border-base-300 shadow-sm">
          <span class="w-2 h-2 rounded-full bg-secondary"></span>
          <span class="text-xs font-medium text-base-content/70">New v2.0 Released</span>
        </div>
        
        <h1 class="text-6xl font-black tracking-tight leading-tight text-base-content">
          连接每一份<br>
          <span class="bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">校园心声</span>
        </h1>
        
        <p class="text-lg text-base-content/80 max-w-lg leading-relaxed">
          CampusWall 是一个连接校友、分享生活、互助成长的校园社区。在这里，每一个声音都值得被倾听。
        </p>
        
        <div class="flex flex-wrap gap-4 pt-2">
          <button class="btn btn-primary rounded-full px-8 h-12 text-base font-bold shadow-lg hover:shadow-primary/50 hover:-translate-y-1 transition-all">
            开始探索 🚀
          </button>
          <button class="btn btn-ghost rounded-full px-6 h-12 text-base font-medium text-base-content hover:bg-base-200">
            热门话题 🔥
          </button>
        </div>

        <div class="flex items-center gap-4 pt-8">
           <div class="avatar-group -space-x-4 rtl:space-x-reverse">
            <div class="avatar border-base-100">
              <div class="w-10 h-10 bg-base-300"></div>
            </div>
            <div class="avatar border-base-100">
              <div class="w-10 h-10 bg-base-300"></div>
            </div>
            <div class="avatar border-base-100">
              <div class="w-10 h-10 bg-base-300"></div>
            </div>
             <div class="avatar placeholder border-base-100">
              <div class="w-10 h-10 bg-neutral text-neutral-content">
                <span class="text-xs">+99</span>
              </div>
            </div>
          </div>
          <p class="text-sm font-semibold text-base-content/70">
            <span class="font-bold text-base-content">12,000+</span> 同学已加入
          </p>
        </div>
      </div>

      <!-- Right Content (Visual) -->
      <div class="relative hidden lg:block h-[400px]">
        <!-- Main Glass Card -->
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-80 bg-base-100/60 backdrop-blur-xl border border-base-content/10 rounded-3xl shadow-2xl animate-float z-20">
          <div class="absolute -bottom-6 -left-6 bg-base-100 rounded-2xl p-4 shadow-xl flex items-center gap-3 animate-float" style="animation-delay: 1.5s;">
             <div class="p-2 rounded-full bg-error/10 text-error">
               <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" viewBox="0 0 20 20" fill="currentColor">
                 <path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd" />
               </svg>
             </div>
             <div>
               <p class="text-xs text-base-content/60 font-medium">热门动态</p>
               <p class="text-sm font-bold text-base-content">+128 喜欢</p>
             </div>
          </div>
        </div>
        
        <!-- Decorative Elements -->
         <div class="absolute top-10 right-10 w-24 h-24 bg-primary/20 rounded-full blur-xl animate-float" style="animation-delay: 2s;"></div>
         <div class="absolute bottom-10 left-20 w-32 h-32 bg-secondary/20 rounded-full blur-xl animate-float" style="animation-delay: 1s;"></div>
      </div>
    </div>
  </div>
</template>
