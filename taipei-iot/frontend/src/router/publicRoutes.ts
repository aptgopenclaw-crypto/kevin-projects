import type { RouteRecordRaw } from 'vue-router'

export const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { isPublic: true },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/user/ProfileView.vue'),
    meta: { isPublic: true },
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/user/ChangePasswordView.vue'),
    meta: { isPublic: true },
  },
  {
    path: '/my/activity',
    name: 'MyActivity',
    component: () => import('@/views/user/MyActivityView.vue'),
    meta: { isPublic: true, breadcrumb: '我的操作記錄' },
  },
  {
    path: '/announcements',
    name: 'Announcements',
    component: () => import('@/views/announcement/AnnouncementListView.vue'),
    meta: { isPublic: true, breadcrumb: '公告欄' },
  },
]
