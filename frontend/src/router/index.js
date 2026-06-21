import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import SignupView from '../views/SignupView.vue'
import FavoriteView from '../views/FavoriteView.vue'
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
