<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { login } from '../api/auth'
import { saveAuth, userRole } from '../stores/auth'

const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

const noticeMessage = computed(() => {
  if (route.query.reason === 'login-required') {
    return '로그인이 필요한 서비스입니다. 로그인 후 관심 목록을 확인할 수 있습니다.'
  }

  if (route.query.reason === 'signup-success') {
    return '회원가입이 완료되었습니다. 가입한 이메일로 로그인해주세요.'
  }

  if (route.query.reason === 'account-deleted') {
    return '회원 탈퇴 신청이 완료되었습니다. 회원 정보는 2주 뒤 완전히 삭제됩니다.'
  }

  return ''
})

async function handleLogin() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const authResponse = await login({
      email: email.value,
      password: password.value
    })

    saveAuth(authResponse)

    await router.replace(getRedirectPath())
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '로그인 요청을 처리하지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

function getRedirectPath() {
  const redirect = route.query.redirect

  // 1. 로그인 전 원래 가려던 페이지(redirect 쿼리)가 있다면 그곳으로 우선 이동
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }

  // 2. 가려던 페이지가 없다면(단순 로그인 진입 시), 권한(Role)에 따라 분기
  if (userRole.value === 'ROLE_ADMIN') {
    return { name: 'adminBatch' } // 관리자는 배치 작업 페이지로
  }

  // 3. 일반 유저(ROLE_USER)의 기본 페이지는 홈
  return '/' 
}
</script>

<template>
  <main class="login-page">
    <section class="login-card" aria-labelledby="login-title">
      <a class="brand" href="/">
        <span class="brand-mark">Z</span>
        <span>집다움</span>
      </a>

      <div class="login-heading">
        <h1 id="login-title">로그인</h1>
        <p>관심 지역과 맞춤 주거 정보를 확인하세요.</p>
      </div>

      <p v-if="noticeMessage" class="form-message info-message" role="status">
        {{ noticeMessage }}
      </p>

      <form class="login-form" @submit.prevent="handleLogin">
        <label>
          <span>이메일</span>
          <input
            v-model.trim="email"
            type="email"
            name="email"
            autocomplete="email"
            maxlength="100"
            placeholder="user@example.com"
            required
          />
        </label>

        <label>
          <span>비밀번호</span>
          <input
            v-model="password"
            type="password"
            name="password"
            autocomplete="current-password"
            minlength="8"
            maxlength="100"
            placeholder="비밀번호를 입력해주세요"
            required
          />
        </label>

        <p v-if="errorMessage" class="form-message error-message" role="alert">
          {{ errorMessage }}
        </p>

        <button class="login-button" type="submit" :disabled="isLoading">
          {{ isLoading ? '로그인 중' : '로그인' }}
        </button>
      </form>

      <p class="signup-link">
        아직 회원이 아니신가요?
        <RouterLink :to="{ name: 'signup' }">회원가입</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped src="../assets/login.css"></style>
