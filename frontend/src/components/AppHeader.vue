<script setup>
import { RouterLink, useRoute } from "vue-router";
import { clearAuth, currentUser, isLoggedIn } from "../stores/auth";

const emit = defineEmits(["home"]);
const route = useRoute();

function handleLogout() {
  clearAuth();
  emit("home");
}
</script>

<template>
  <header class="top-bar">
    <a
      class="brand"
      href="#"
      aria-label="집다움 홈"
      @click.prevent="emit('home')"
    >
      <span class="brand-mark">Z</span>
      <span>집다움</span>
    </a>

    <nav class="main-nav" aria-label="주요 메뉴">
      <a
        :class="{ active: route.name === 'home' }"
        href="#"
        @click.prevent="emit('home')"
      >
        실거래가 검색
      </a>
      <RouterLink :to="{ name: 'favorites' }" active-class="active">
        관심목록
      </RouterLink>
      <RouterLink
        :to="{ name: 'mypage', query: { tab: 'compare' } }"
        :class="{ active: route.name === 'mypage' && route.query.tab === 'compare' }"
      >
        집 비교하기
      </RouterLink>
      <RouterLink
        :to="{ name: 'mypage' }"
        :class="{ active: route.name === 'mypage' && route.query.tab !== 'compare' }"
      >
        마이페이지
      </RouterLink>
    </nav>

    <div v-if="isLoggedIn" class="account-actions">
      <span class="user-name">{{ currentUser?.name || "회원" }}님</span>
      <button class="ghost-button" type="button" @click="handleLogout">
        로그아웃
      </button>
    </div>

    <div v-else class="account-actions">
      <RouterLink class="ghost-button" :to="{ name: 'login' }">
        로그인
      </RouterLink>
      <RouterLink class="primary-button" :to="{ name: 'signup' }">
        회원가입
      </RouterLink>
    </div>
  </header>
</template>
