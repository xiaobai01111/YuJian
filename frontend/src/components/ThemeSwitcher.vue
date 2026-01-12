<template>
  <div class="dropdown dropdown-end">
    <div tabindex="0" role="button" class="btn btn-ghost btn-circle" title="切换主题">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
      </svg>
    </div>
    <ul tabindex="0" class="dropdown-content z-[1] menu p-2 shadow-2xl bg-base-200 rounded-box w-52 max-h-96 overflow-y-auto block">
      <li v-for="theme in themes" :key="theme">
        <button 
          class="flex justify-between items-center"
          :class="{ 'active': currentTheme === theme }"
          @click="updateTheme(theme)"
        >
          {{ theme }}
          <span v-if="currentTheme === theme" class="text-primary">✓</span>
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

// All available DaisyUI themes
const themes = [
  "light", "dark", "cupcake", "bumblebee", "emerald", "corporate", "synthwave", "retro", "cyberpunk", "valentine", "halloween", "garden", "forest", "aqua", "lofi", "pastel", "fantasy", "wireframe", "black", "luxury", "dracula", "cmyk", "autumn", "business", "acid", "lemonade", "night", "coffee", "winter", "dim", "nord", "sunset"
]

const currentTheme = ref('light')

onMounted(() => {
  const savedTheme = localStorage.getItem('theme') || 'light'
  updateTheme(savedTheme)
})

function updateTheme(theme: string) {
  currentTheme.value = theme
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem('theme', theme)
  // Close dropdown by blurring active element if needed, though DaisyUI dropdown usually handles focus
  if (document.activeElement instanceof HTMLElement) {
    document.activeElement.blur()
  }
}
</script>
