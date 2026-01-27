<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <h1>404</h1>
      <p>页面不存在</p>
      <router-link to="/" class="back-home">返回首页</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.not-found-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.not-found-content {
  text-align: center;
}

.not-found-content h1 {
  font-size: 6rem;
  font-weight: bold;
  color: #8b5cf6;
  margin-bottom: 1rem;
}

.not-found-content p {
  font-size: 1.25rem;
  color: #6b7280;
  margin-bottom: 2rem;
}

.back-home {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(to right, #8b5cf6, #a78bfa);
  color: white;
  border-radius: 0.5rem;
  text-decoration: none;
  transition: opacity 0.2s;
}

.back-home:hover {
  opacity: 0.9;
}
</style>
