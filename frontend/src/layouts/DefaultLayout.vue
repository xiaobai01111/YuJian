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
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCampusHeroByPage, type CampusHeroVO } from '@/api/system'

const route = useRoute()
const isHomePage = computed(() => route.path === '/')
const heroConfig = ref<CampusHeroVO | null>(null)

const heroKey = computed(() => {
  if (route.meta?.heroKey) {
    return String(route.meta.heroKey)
  }
  if (isHomePage.value) {
    return 'HOME'
  }
  return ''
})

const showHero = computed(() => {
  return !!heroConfig.value?.enabled
})

const fetchHeroConfig = async () => {
  if (!heroKey.value) {
    heroConfig.value = null
    return
  }
  try {
    const res = await getCampusHeroByPage(heroKey.value)
    heroConfig.value = res || null
  } catch (error) {
    console.error('Failed to fetch hero config', error)
    heroConfig.value = null
  }
}

const heroProps = computed(() => {
  if (heroConfig.value?.enabled) {
    const config = heroConfig.value
    const props: Record<string, unknown> = {}
    if (config.theme) props.theme = config.theme
    if (config.titleStart) props.titleStart = config.titleStart
    if (config.titleHighlight) props.titleHighlight = config.titleHighlight
    if (config.description) props.description = config.description
    if (config.badge) props.badge = config.badge
    if (config.primaryBtnText) props.primaryBtnText = config.primaryBtnText
    if (config.primaryBtnLink) props.primaryBtnLink = config.primaryBtnLink
    if (config.secondaryBtnText) props.secondaryBtnText = config.secondaryBtnText
    if (config.secondaryBtnLink) props.secondaryBtnLink = config.secondaryBtnLink
    if (config.showStats !== undefined) props.showStats = config.showStats
    if (config.statsNumber) props.statsNumber = config.statsNumber
    if (config.statsLabel) props.statsLabel = config.statsLabel
    if (config.avatarUrls && config.avatarUrls.length > 0) props.avatarUrls = config.avatarUrls
    if (config.avatarNames && config.avatarNames.length > 0) props.avatarNames = config.avatarNames
    if (config.floatCardLabel) props.floatCardLabel = config.floatCardLabel
    if (config.floatCardValue) props.floatCardValue = config.floatCardValue
    return props
  }
  return {}
})

watch(
  () => heroKey.value,
  () => {
    fetchHeroConfig()
  },
  { immediate: true }
)
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
