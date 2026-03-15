import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/patients',
      name: 'patients',
      component: () => import('../views/PatientsView.vue'),
    },
    {
      path: '/appointments',
      name: 'appointments',
      component: () => import('../views/AppointmentsView.vue'),
    },
    {
      path: '/doctors',
      name: 'doctors',
      component: () => import('../views/DoctorsView.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
    },
    {
      path: '/admin/users',
      name: 'users',
      component: () => import('../views/UsersView.vue'),
      meta: { adminOnly: true },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const isPublic = to.meta.public === true
  const isAdminOnly = to.meta.adminOnly === true

  if (isPublic && authStore.isAuthenticated) {
    next({ name: 'home' })
    return
  }
  if (!isPublic && !authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }
  if (isAdminOnly && !authStore.isAdmin) {
    next({ name: 'home' })
    return
  }
  next()
})

export default router
