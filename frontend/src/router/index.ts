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
      path: '/admin',
      name: 'admin-dashboard',
      component: () => import('../views/AdminDashboardView.vue'),
      meta: { adminOnly: true },
    },
    {
      path: '/admin/users',
      name: 'users',
      component: () => import('../views/UsersView.vue'),
      meta: { adminOnly: true, permission: 'users.read_all' },
    },
    {
      path: '/admin/roles',
      name: 'roles',
      component: () => import('../views/RolesView.vue'),
      meta: { adminOnly: true, permission: 'users.manage_roles' },
    },
    {
      path: '/admin/role-permissions',
      name: 'role-permissions',
      component: () => import('../views/RolePermissionsView.vue'),
      meta: { adminOnly: true, permission: 'users.manage_roles' },
    },
    {
      path: '/admin/rbac-audit',
      name: 'rbac-audit',
      component: () => import('../views/RbacAuditView.vue'),
      meta: { adminOnly: true, permission: 'users.manage_roles' },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const isPublic = to.meta.public === true
  const isAdminOnly = to.meta.adminOnly === true
  const requiredPermission = typeof to.meta.permission === 'string' ? to.meta.permission : null

  if (isPublic && authStore.isAuthenticated) {
    next({ name: 'home' })
    return
  }
  if (!isPublic && !authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }
  if (isAdminOnly && !authStore.canAccessAdmin) {
    next({ name: 'home' })
    return
  }
  if (requiredPermission && !authStore.hasPermission(requiredPermission)) {
    next({ name: 'home' })
    return
  }
  next()
})

export default router
