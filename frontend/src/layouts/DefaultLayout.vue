<template>
  <div class="min-h-screen bg-base-200 font-sans text-base-content transition-colors duration-300">
    <!-- Navbar -->
    <Navbar />

    <!-- Hero Section -->
    <transition name="fade" mode="out-in">
      <HeroSection 
        v-if="showHero" 
        v-bind="heroProps"
        :key="route.path"
      />
    </transition>

    <!-- Main Content -->
    <div class="container mx-auto px-4 py-8 grid grid-cols-1 lg:grid-cols-4 gap-8 min-h-[80vh]">
      <!-- Main Column -->
      <main class="lg:col-span-3 space-y-6">
        <slot />
      </main>

      <!-- Sidebar Column (Desktop Only) -->
      <aside class="hidden lg:block lg:col-span-1">
        <div class="sticky top-24 space-y-6">
          <!-- Target for Teleport from Views -->
          <div id="sidebar-slot-target" class="space-y-6"></div>
          
          <slot name="sidebar">
            <!-- Fallback content if no sidebar teleport/slot provided -->
          </slot>
        </div>
      </aside>
    </div>

    <!-- Footer -->
    <Footer />
  </div>
</template>

<script setup lang="ts">
import Navbar from '@/components/layout/Navbar.vue'
import HeroSection from '@/components/layout/HeroSection.vue'
import Footer from '@/components/layout/Footer.vue'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isHomePage = computed(() => route.path === '/')

const showHero = computed(() => {
  return isHomePage.value || !!route.meta.hero
})

const heroProps = computed(() => {
  if (isHomePage.value) {
    return {
      theme: 'blue',
      titleStart: '连接每一份',
      titleHighlight: '校园心声',
      description: 'CampusWall 是一个连接校友、分享生活、互助成长的校园社区。在这里，每一个声音都值得被倾听。',
      badge: 'New v2.0 Released',
      primaryBtnText: '开始探索 🚀',
      secondaryBtnText: '热门话题 🔥'
    }
  }
  return route.meta.hero || {}
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
