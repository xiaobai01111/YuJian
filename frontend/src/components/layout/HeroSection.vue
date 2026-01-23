<template>
  <div class="hero min-h-[calc(100vh-4rem)] bg-base-100 relative overflow-hidden transition-colors duration-500" :class="bgGradientClass">
    <!-- Background Elements -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-[20%] -left-[10%] w-[600px] h-[600px] rounded-full blur-[100px] animate-pulse-soft opacity-20" :class="blob1Class"></div>
      <div class="absolute top-[20%] right-[10%] w-[500px] h-[500px] rounded-full blur-[100px] animate-pulse-soft opacity-20" :class="blob2Class" style="animation-delay: 1s;"></div>
    </div>

    <div class="container mx-auto px-4 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center z-10 py-16">
      <!-- Left Content -->
      <div class="text-left space-y-8 animate-fade-in-up">
        <div v-if="badge" class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-base-200/50 backdrop-blur border border-base-300 shadow-sm">
          <span class="w-2 h-2 rounded-full" :class="badgeColorClass"></span>
          <span class="text-xs font-medium text-base-content/70">{{ badge }}</span>
        </div>
        
        <h1 class="text-5xl lg:text-6xl font-black tracking-tight leading-tight text-base-content">
          {{ titleStart }}<br>
          <span class="bg-clip-text text-transparent bg-gradient-to-r" :class="titleGradientClass">{{ titleHighlight }}</span>
        </h1>
        
        <p class="text-lg text-base-content/80 max-w-lg leading-relaxed">
          {{ description }}
        </p>
        
        <div class="flex flex-wrap gap-4 pt-2">
          <component
            :is="primaryBtnLink ? 'a' : 'button'"
            :href="primaryBtnLink || undefined"
            :type="primaryBtnLink ? undefined : 'button'"
            class="btn rounded-full px-8 h-12 text-base font-bold shadow-lg hover:-translate-y-1 transition-all border-none text-white"
            :class="primaryBtnClass"
          >
            {{ primaryBtnText }}
          </component>
          <component
            v-if="secondaryBtnText"
            :is="secondaryBtnLink ? 'a' : 'button'"
            :href="secondaryBtnLink || undefined"
            :type="secondaryBtnLink ? undefined : 'button'"
            class="btn btn-ghost rounded-full px-6 h-12 text-base font-medium text-base-content hover:bg-base-200/50"
          >
            {{ secondaryBtnText }}
          </component>
        </div>

        <div v-if="showStats" class="flex items-center gap-4 pt-8">
           <div class="avatar-group -space-x-4 rtl:space-x-reverse">
            <template v-if="avatarUrls.length > 0">
              <div v-for="(url, index) in visibleAvatars" :key="`${url}-${index}`" class="avatar border-base-100">
                <div class="w-10 h-10">
                  <img :src="url" alt="avatar" />
                </div>
              </div>
              <div v-if="extraAvatarCount > 0" class="avatar placeholder border-base-100">
                <div class="w-10 h-10 bg-neutral text-neutral-content">
                  <span class="text-xs">+{{ extraAvatarCount }}</span>
                </div>
              </div>
            </template>
            <template v-else>
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
            </template>
          </div>
          <p class="text-sm font-semibold text-base-content/70">
            <span class="font-bold text-base-content">{{ statsNumber }}</span> {{ statsLabel }}
          </p>
        </div>
      </div>

      <!-- Right Content (Visual) -->
      <div class="relative hidden lg:block h-[400px]">
        <!-- Main Glass Card -->
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-80 bg-base-100/60 backdrop-blur-xl border border-base-content/10 rounded-3xl shadow-2xl animate-float z-20">
          <div class="absolute -bottom-6 -left-6 bg-base-100 rounded-2xl p-4 shadow-xl flex items-center gap-3 animate-float" style="animation-delay: 1.5s;">
             <div class="p-2 rounded-full bg-base-200">
               <component :is="iconComponent" class="w-6 h-6" :class="iconColorClass" />
             </div>
             <div>
               <p class="text-xs text-base-content/60 font-medium">{{ floatCardLabel }}</p>
               <p class="text-sm font-bold text-base-content">{{ floatCardValue }}</p>
             </div>
          </div>
        </div>
        
        <!-- Decorative Elements -->
         <div class="absolute top-10 right-10 w-24 h-24 rounded-full blur-xl animate-float opacity-30" :class="blob1Class" style="animation-delay: 2s;"></div>
         <div class="absolute bottom-10 left-20 w-32 h-32 rounded-full blur-xl animate-float opacity-30" :class="blob2Class" style="animation-delay: 1s;"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'

const props = defineProps({
  theme: { type: String, default: 'blue' }, // blue, pink, emerald, orange, purple
  titleStart: { type: String, default: '连接每一份' },
  titleHighlight: { type: String, default: '校园心声' },
  description: { type: String, default: 'CampusWall 是一个连接校友、分享生活、互助成长的校园社区。在这里，每一个声音都值得被倾听。' },
  badge: { type: String, default: 'New v2.0 Released' },
  primaryBtnText: { type: String, default: '开始探索 ' },
  primaryBtnLink: { type: String, default: '' },
  secondaryBtnText: { type: String, default: '热门话题 ' },
  secondaryBtnLink: { type: String, default: '' },
  showStats: { type: Boolean, default: true },
  statsNumber: { type: String, default: '12,000+' },
  statsLabel: { type: String, default: '同学已加入' },
  avatarUrls: { type: Array as () => string[], default: () => [] },
  floatCardLabel: { type: String, default: '热门动态' },
  floatCardValue: { type: String, default: '+128 ' }
})

const bgGradientClass = computed(() => {
  const map: Record<string, string> = {
    blue: 'bg-gradient-to-br from-blue-500/5 via-transparent to-indigo-500/5',
    pink: 'bg-gradient-to-br from-pink-500/5 via-transparent to-rose-500/5',
    emerald: 'bg-gradient-to-br from-emerald-500/5 via-transparent to-teal-500/5',
    orange: 'bg-gradient-to-br from-orange-500/5 via-transparent to-amber-500/5',
    purple: 'bg-gradient-to-br from-purple-500/5 via-transparent to-violet-500/5',
    sky: 'bg-gradient-to-br from-sky-500/5 via-transparent to-cyan-500/5',
    rose: 'bg-gradient-to-br from-rose-500/5 via-transparent to-pink-500/5',
    teal: 'bg-gradient-to-br from-teal-500/5 via-transparent to-emerald-500/5',
    amber: 'bg-gradient-to-br from-amber-500/5 via-transparent to-orange-500/5'
  }
  return map[props.theme] || map.blue
})

const blob1Class = computed(() => {
  const map: Record<string, string> = {
    blue: 'bg-blue-500',
    pink: 'bg-pink-500',
    emerald: 'bg-emerald-500',
    orange: 'bg-orange-500',
    purple: 'bg-purple-500',
    sky: 'bg-sky-500',
    rose: 'bg-rose-500',
    teal: 'bg-teal-500',
    amber: 'bg-amber-500'
  }
  return map[props.theme] || map.blue
})

const blob2Class = computed(() => {
  const map: Record<string, string> = {
    blue: 'bg-indigo-500',
    pink: 'bg-rose-500',
    emerald: 'bg-teal-500',
    orange: 'bg-amber-500',
    purple: 'bg-violet-500',
    sky: 'bg-cyan-500',
    rose: 'bg-pink-500',
    teal: 'bg-emerald-500',
    amber: 'bg-orange-500'
  }
  return map[props.theme] || map.blue
})

const badgeColorClass = computed(() => {
  const map: Record<string, string> = {
    blue: 'bg-blue-500',
    pink: 'bg-pink-500',
    emerald: 'bg-emerald-500',
    orange: 'bg-orange-500',
    purple: 'bg-purple-500',
    sky: 'bg-sky-500',
    rose: 'bg-rose-500',
    teal: 'bg-teal-500',
    amber: 'bg-amber-500'
  }
  return map[props.theme] || map.blue
})

const titleGradientClass = computed(() => {
  const map: Record<string, string> = {
    blue: 'from-blue-600 to-indigo-600',
    pink: 'from-pink-600 to-rose-600',
    emerald: 'from-emerald-600 to-teal-600',
    orange: 'from-orange-600 to-amber-600',
    purple: 'from-purple-600 to-violet-600',
    sky: 'from-sky-600 to-cyan-600',
    rose: 'from-rose-600 to-pink-600',
    teal: 'from-teal-600 to-emerald-600',
    amber: 'from-amber-600 to-orange-600'
  }
  return map[props.theme] || map.blue
})

const primaryBtnClass = computed(() => {
  const map: Record<string, string> = {
    blue: 'bg-blue-600 hover:bg-blue-700 shadow-blue-200',
    pink: 'bg-pink-500 hover:bg-pink-600 shadow-pink-200',
    emerald: 'bg-emerald-500 hover:bg-emerald-600 shadow-emerald-200',
    orange: 'bg-orange-500 hover:bg-orange-600 shadow-orange-200',
    purple: 'bg-purple-500 hover:bg-purple-600 shadow-purple-200',
    sky: 'bg-sky-600 hover:bg-sky-700 shadow-sky-200',
    rose: 'bg-rose-500 hover:bg-rose-600 shadow-rose-200',
    teal: 'bg-teal-500 hover:bg-teal-600 shadow-teal-200',
    amber: 'bg-amber-500 hover:bg-amber-600 shadow-amber-200'
  }
  return map[props.theme] || map.blue
})

const iconColorClass = computed(() => {
  const map: Record<string, string> = {
    blue: 'text-blue-500',
    pink: 'text-pink-500',
    emerald: 'text-emerald-500',
    orange: 'text-orange-500',
    purple: 'text-purple-500',
    sky: 'text-sky-500',
    rose: 'text-rose-500',
    teal: 'text-teal-500',
    amber: 'text-amber-500'
  }
  return map[props.theme] || map.blue
})

const visibleAvatars = computed(() => props.avatarUrls.slice(0, 3))
const extraAvatarCount = computed(() => Math.max(props.avatarUrls.length - visibleAvatars.value.length, 0))

// Icons
const iconComponent = computed(() => {
  // Simple SVG components
  if (props.theme === 'pink') return { render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", viewBox: "0 0 24 24", fill: "currentColor" }, [ h('path', { d: "M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3A5.5 5.5 0 0112 5.052 5.5 5.5 0 0116.313 3c2.973 0 5.437 2.322 5.437 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z" }) ]) }
  
  if (props.theme === 'emerald') return { render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", viewBox: "0 0 24 24", fill: "currentColor" }, [ h('path', { d: "M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12zm14.024-.983a1.125 1.125 0 010 1.966l-5.603 3.113A1.125 1.125 0 019 15.113V8.887c0-.857.921-1.4 1.671-.983l5.603 3.113-.042-.02a.75.75 0 11-.671-1.34l.041-.022zM12 9a.75.75 0 100-1.5.75.75 0 000 1.5z" }) ]) }

  if (props.theme === 'orange') return { render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", viewBox: "0 0 24 24", fill: "currentColor" }, [ h('path', { fillRule: "evenodd", d: "M5.25 2.25a3 3 0 00-3 3v4.318a3 3 0 00.879 2.121l9.58 9.581c.92.92 2.39 .92 3.31 0l4.62-4.621a2.36 2.36 0 000-3.31l-9.58-9.58A3 3 0 0012.818 2.25H5.25zM6 8.25a2.25 2.25 0 100-4.5 2.25 2.25 0 000 4.5z", clipRule: "evenodd" }) ]) }

  if (props.theme === 'purple') return { render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", viewBox: "0 0 24 24", fill: "currentColor" }, [ h('path', { fillRule: "evenodd", d: "M10.5 3.75a6.75 6.75 0 100 13.5 6.75 6.75 0 000-13.5zM2.25 10.5a8.25 8.25 0 1114.59 5.28l4.69 4.69a.75.75 0 11-1.06 1.06l-4.69-4.69A8.25 8.25 0 012.25 10.5z", clipRule: "evenodd" }) ]) }

  // Default Blue (Question/Home)
  return { render: () => h('svg', { xmlns: "http://www.w3.org/2000/svg", viewBox: "0 0 24 24", fill: "currentColor" }, [ h('path', { fillRule: "evenodd", d: "M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12zm8.706-1.442c1.146-.573 2.437.463 2.126 1.706l-.709 2.836.042-.02a.75.75 0 01.67 1.34l-.04.022c-1.147.573-2.438-.463-2.127-1.706l.71-2.836-.042.02a.75.75 0 11-.671-1.34l.041-.022zM12 9a.75.75 0 100-1.5.75.75 0 000 1.5z", clipRule: "evenodd" }) ]) }
})
</script>
