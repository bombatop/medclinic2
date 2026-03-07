<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppLayout from './layouts/AppLayout.vue'

const route = useRoute()
const isLoginPage = computed(() => route.name === 'login')
</script>

<template>
  <AppLayout v-if="!isLoginPage">
    <RouterView />
  </AppLayout>
  <RouterView v-else />
</template>
