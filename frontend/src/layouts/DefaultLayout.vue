<template>
  <div class="min-h-screen bg-base-200 font-sans text-base-content transition-colors duration-300">
    <!-- Navbar -->
    <Navbar />

    <!-- Hero Section (Only on Home Page) -->
    <transition name="fade" mode="out-in">
      <HeroSection v-if="isHomePage" />
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
