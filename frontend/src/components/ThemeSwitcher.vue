<template>
  <details class="dropdown dropdown-end" ref="detailsRef">
    <summary tabindex="0" role="button" class="btn btn-ghost btn-circle btn-sm" title="切换主题">
      <slot>
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
        </svg>
      </slot>
    </summary>
    <ul tabindex="0" class="dropdown-content z-[999] menu p-2 shadow-2xl bg-base-200 rounded-box w-52 max-h-96 overflow-y-auto flex flex-col flex-nowrap">
      <li v-for="theme in themes" :key="theme">
        <button 
          class="flex justify-between items-center"
          :class="{ 'active': currentTheme === theme }"
          @click="updateTheme(theme)"
        >
          {{ themeMap[theme] || theme }}
          <span v-if="currentTheme === theme" class="text-primary">✓</span>
        </button>
      </li>
    </ul>
  </details>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

// All available DaisyUI themes
const themes = [
  "light", "dark", "cupcake", "bumblebee", "emerald", "corporate", "synthwave", "retro", "cyberpunk", "valentine", "halloween", "garden", "forest", "aqua", "lofi", "pastel", "fantasy", "wireframe", "black", "luxury", "dracula", "cmyk", "autumn", "business", "acid", "lemonade", "night", "coffee", "winter", "dim", "nord", "sunset"
]

const themeMap: Record<string, string> = {
  "light": "明亮 (Light)",
  "dark": "暗黑 (Dark)",
  "cupcake": "纸杯蛋糕 (Cupcake)",
  "bumblebee": "大黄蜂 (Bumblebee)",
  "emerald": "翡翠 (Emerald)",
  "corporate": "企业 (Corporate)",
  "synthwave": "合成波 (Synthwave)",
  "retro": "复古 (Retro)",
  "cyberpunk": "赛博朋克 (Cyberpunk)",
  "valentine": "情人节 (Valentine)",
  "halloween": "万圣节 (Halloween)",
  "garden": "花园 (Garden)",
  "forest": "森林 (Forest)",
  "aqua": "水蓝 (Aqua)",
  "lofi": "低保真 (Lo-Fi)",
  "pastel": "粉彩 (Pastel)",
  "fantasy": "幻想 (Fantasy)",
  "wireframe": "线框 (Wireframe)",
  "black": "纯黑 (Black)",
  "luxury": "奢华 (Luxury)",
  "dracula": "吸血鬼 (Dracula)",
  "cmyk": "印刷色 (CMYK)",
  "autumn": "秋天 (Autumn)",
  "business": "商务 (Business)",
  "acid": "酸性 (Acid)",
  "lemonade": "柠檬水 (Lemonade)",
  "night": "黑夜 (Night)",
  "coffee": "咖啡 (Coffee)",
  "winter": "冬天 (Winter)",
  "dim": "暗淡 (Dim)",
  "nord": "诺德 (Nord)",
  "sunset": "日落 (Sunset)"
}

const currentTheme = ref('light')
const detailsRef = ref<HTMLDetailsElement | null>(null)

onMounted(() => {
  const savedTheme = localStorage.getItem('theme') || 'light'
  updateTheme(savedTheme)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

function updateTheme(theme: string) {
  currentTheme.value = theme
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem('theme', theme)
}

function handleClickOutside(event: MouseEvent) {
  if (detailsRef.value && !detailsRef.value.contains(event.target as Node)) {
    detailsRef.value.removeAttribute('open')
  }
}
</script>
