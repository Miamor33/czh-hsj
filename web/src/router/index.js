import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/',
    name: 'cover',
    component: () => import('../views/CoverPage.vue'),
    meta: { public: true },
  },
  {
    path: '/app',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('../views/HomePage.vue'),
      },
      {
        path: 'album',
        name: 'album',
        component: () => import('../views/AlbumPage.vue'),
      },
      {
        path: 'qa',
        name: 'qa',
        component: () => import('../views/QaPage.vue'),
      },
      {
        path: 'challenges',
        name: 'challenges',
        component: () => import('../views/ChallengePage.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.path.startsWith('/app')) {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      return { path: '/' }
    }
  }
})

export default router
