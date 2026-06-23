<script setup>
import { useRoute, useRouter } from "vue-router";
import { clearAuth, currentUser, isLoggedIn } from "../stores/auth";

const emit = defineEmits(["home"]);
const route = useRoute();
const router = useRouter();

function getCurrentHref() {
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}

async function navigateTo(target) {
  const targetHref = router.resolve(target).href;
  const failure = await router.push(target).catch((error) => error);

  if (failure && getCurrentHref() !== targetHref) {
    window.location.assign(targetHref);
  }
}

async function goHome() {
  if (route.name === "home") {
    emit("home");
    return;
  }

  await navigateTo({ name: "home" });
}

function handleLogout() {
  clearAuth();
  goHome();
}

function goLogin() {
  navigateTo({ name: "login" });
}

function goSignup() {
  navigateTo({ name: "signup" });
}

function goFavorites() {
  navigateTo({ name: "favorites" });
}

function goCompare() {
  navigateTo({ name: "comparison" });
}

function goMyPage() {
  navigateTo({ name: "mypage" });
}
</script>

<template>
  <header class="top-bar">
    <button
      class="brand"
      aria-label="집다움 홈"
      type="button"
      @click="goHome"
    >
      <span class="brand-mark">Z</span>
      <span>집다움</span>
    </button>

    <nav class="main-nav" aria-label="주요 메뉴">
      <button
        :class="{ active: route.name === 'home' }"
        type="button"
        @click="goHome"
      >
        실거래가 검색
      </button>
      <button
        :class="{ active: route.name === 'favorites' }"
        type="button"
        @click="goFavorites"
      >
        관심목록
      </button>
      <button
        :class="{ active: route.name === 'comparison' }"
        type="button"
        @click="goCompare"
      >
        집 비교하기
      </button>
      <button
        :class="{ active: route.name === 'mypage' }"
        type="button"
        @click="goMyPage"
      >
        마이페이지
      </button>
    </nav>

    <div v-if="isLoggedIn" class="account-actions">
      <span class="user-name">{{ currentUser?.name || "회원" }}님</span>
      <button class="ghost-button" type="button" @click="handleLogout">
        로그아웃
      </button>
    </div>

    <div v-else class="account-actions">
      <button class="ghost-button" type="button" @click="goLogin">
        로그인
      </button>
      <button class="primary-button" type="button" @click="goSignup">
        회원가입
      </button>
    </div>
  </header>
</template>
