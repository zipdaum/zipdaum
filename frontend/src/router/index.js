import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import LoginView from "../views/LoginView.vue";
import AdminBatchView from "../views/admin/AdminBatchView.vue";
import SignupView from "../views/SignupView.vue";
import FavoriteView from "../views/FavoriteView.vue";
import MyPageView from "../views/MyPageView.vue";
import ComparisonView from "../views/ComparisonView.vue";
import PreferenceSettingView from "../views/PreferenceSettingView.vue";
import RecommendationScoreView from "../views/RecommendationScoreView.vue";
import { isLoggedIn, userRole } from "../stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
    },
    {
      path: "/login",
      name: "login",
      component: LoginView,
    },
    {
      path: "/admin/batch",
      name: "adminBatch",
      component: AdminBatchView,
      meta: { requiresAuth: true, roles: ["ROLE_ADMIN"] },
    },
    {
      path: "/signup",
      name: "signup",
      component: SignupView,
    },
    {
      path: "/favorites",
      name: "favorites",
      component: FavoriteView,
      meta: { requiresAuth: true, roles: ["ROLE_USER"] },
    },
    {
      path: "/mypage",
      name: "mypage",
      component: MyPageView,
      meta: { requiresAuth: true, roles: ["ROLE_USER"] },
    },
    {
      path: "/comparison",
      name: "comparison",
      component: ComparisonView,
      meta: { requiresAuth: true, roles: ["ROLE_USER"] },
    },
    {
      path: "/preferences",
      name: "preferences",
      component: PreferenceSettingView,
      meta: { requiresAuth: true, roles: ["ROLE_USER"] },
    },
    {
      path: "/recommendation-score/properties/:propertyId",
      name: "property-recommendation-score",
      component: RecommendationScoreView,
      meta: { requiresAuth: true, roles: ["ROLE_USER"] },
    },
  ],
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn.value) {
    return {
      name: "login",
      query: {
        redirect: to.fullPath,
        reason: "login-required",
      },
    };
  }

  // 2. 이미 로그인했는데 로그인/회원가입 페이지에 가려는 경우 -> 권한별 홈으로 리다이렉트
  if ((to.name === "login" || to.name === "signup") && isLoggedIn.value) {
    if (userRole.value === "ROLE_ADMIN") {
      return { name: "adminBatch" };
    } else {
      return { name: "home" };
    }
  }

  if (
    to.name === "home" &&
    isLoggedIn.value &&
    userRole.value === "ROLE_ADMIN"
  ) {
    return { name: "adminBatch" }; // 경고창 없이 관리자 페이지로 부드럽게 이동
  }

  // 3. 페이지에 역할 제한이 설정되어 있는 경우 권한 검사 (핵심!)
  if (to.meta.roles && isLoggedIn.value) {
    // 사용자의 권한이 접근 허용 권한 목록에 없는 경우
    if (!to.meta.roles.includes(userRole.value)) {
      alert("접근 권한이 없습니다.");
      // 각자의 기본 페이지로 튕겨내기
      if (userRole.value === "ROLE_ADMIN") {
        return { name: "adminBatch" };
      } else {
        return { name: "home" };
      }
    }
  }
});

export default router;
