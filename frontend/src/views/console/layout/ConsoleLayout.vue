<template>
  <div class="drawer lg:drawer-open font-sans bg-base-200 h-screen overflow-hidden">
    <input id="console-drawer" type="checkbox" class="drawer-toggle" />
    <div class="drawer-content flex flex-col h-full overflow-hidden">
      <!-- Navbar -->
      <div class="navbar bg-base-100 shadow-sm sticky top-0 z-30 flex-none">
        <div class="flex-none lg:hidden">
          <label for="console-drawer" aria-label="open sidebar" class="btn btn-square btn-ghost">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline-block w-6 h-6 stroke-current"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path></svg>
          </label>
        </div>
        <div class="flex-1 px-4">
          <div class="text-sm breadcrumbs">
            <ul>
              <li><a>Console</a></li>
              <!-- Dynamic breadcrumbs could be added here -->
              <li>{{ route.meta.title || route.name }}</li>
            </ul>
          </div>
        </div>
        <div class="flex-none gap-2">
           <ThemeSwitcher />
           <div class="dropdown dropdown-end">
              <label tabindex="0" class="btn btn-ghost btn-circle avatar placeholder">
                <div class="bg-neutral text-neutral-content rounded-full w-10">
                  <span>A</span>
                </div>
              </label>
              <ul tabindex="0" class="menu menu-sm dropdown-content mt-3 z-[1] p-2 shadow bg-base-100 rounded-box w-52">
                <li><router-link to="/">Back to Home</router-link></li>
                <li @click="handleLogout"><a>Logout</a></li>
              </ul>
           </div>
        </div>
      </div>
      
      <!-- Page Content -->
      <main class="flex-1 p-6 flex flex-col overflow-hidden">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
             <component :is="Component" class="h-full" />
          </transition>
        </router-view>
      </main>
    </div> 
    
    <div class="drawer-side z-40">
      <label for="console-drawer" aria-label="close sidebar" class="drawer-overlay"></label> 
      <ConsoleSidebar :routes="permissionStore.routes" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import ConsoleSidebar from './ConsoleSidebar.vue'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const handleLogout = () => {
    userStore.logout()
    router.push('/')
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
