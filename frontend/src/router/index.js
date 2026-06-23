import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import AdminBatchView from '../views/admin/AdminBatchView.vue'
import SignupView from '../views/SignupView.vue'
import FavoriteView from '../views/FavoriteView.vue'
import MyPageView from '../views/MyPageView.vue'
import ComparisonView from '../views/ComparisonView.vue'
import PreferenceSettingView from '../views/PreferenceSettingView.vue'
import RecommendationScoreView from '../views/RecommendationScoreView.vue'
import { isLoggedIn } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/admin/batch',
      name: 'adminBatch',
      component: AdminBatchView
    },
    {
      path: '/signup',
      name: 'signup',
      component: SignupView
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: FavoriteView,
      meta: { requiresAuth: true }
    },
    {
      path: '/mypage',
      name: 'mypage',
      component: MyPageView,
      meta: { requiresAuth: true }
    },
    {
      path: '/comparison',
      name: 'comparison',
      component: ComparisonView,
      meta: { requiresAuth: true }
    },
    {
      path: '/preferences',
      name: 'preferences',
      component: PreferenceSettingView,
      meta: { requiresAuth: true }
    },
    {
      path: '/recommendation-score/properties/:propertyId',
      name: 'property-recommendation-score',
      component: RecommendationScoreView,
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn.value) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
        reason: 'login-required'
      }
    }
  }

  if ((to.name === 'login' || to.name === 'signup') && isLoggedIn.value) {
    return { name: 'home' }
  }
})

export default router
